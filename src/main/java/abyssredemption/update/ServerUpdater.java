package abyssredemption.update;

import abyssredemption.AbsServerTool;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarFile;

/** Checks in the background; replacing the loaded mod is deferred until the JVM exits. */
public final class ServerUpdater {
    private static ServerUpdater active;
    private final ScheduledThreadPoolExecutor worker=new ScheduledThreadPoolExecutor(1,r->{
        var thread=new Thread(r,"AbsServerTool-update"); thread.setDaemon(true); return thread;
    });
    private final Path target, directory;
    private final String installed, game, loader;
    private final boolean beta, autoInstall;
    private volatile String status="尚未检查更新。";
    private volatile ReleaseCatalog.Asset candidate;
    private boolean busy, stopped;
    private Pending pending;
    private record Pending(Path work,Path jar,String oldHash,String newHash) {}

    private ServerUpdater(Path target,Path directory,String installed,String game,String loader,Properties settings) {
        this.target=target; this.directory=directory; this.installed=installed; this.game=game; this.loader=loader;
        beta=settings.getProperty("channel","beta").equalsIgnoreCase("beta");
        autoInstall=Boolean.parseBoolean(settings.getProperty("autoInstall","false"));
        if(Boolean.parseBoolean(settings.getProperty("autoCheck","true")))
            worker.scheduleWithFixedDelay(()->check(false),5,21600,TimeUnit.SECONDS);
    }

    public static void start(Path instance,Path origin,String installed,String game,String loader) {
        try {
            Path mods=instance.resolve("mods").toRealPath();
            if(!Files.isRegularFile(origin,LinkOption.NOFOLLOW_LINKS)) return;
            Path target=origin.toRealPath();
            if(!target.getParent().equals(mods) || !target.getFileName().toString().endsWith(".jar")) return;
            Path directory=instance.toRealPath().resolve("config/absservertool-update");
            Files.createDirectories(directory);
            if(directory.toRealPath().startsWith(mods)) throw new IOException("Update storage must be outside mods");
            var properties=new Properties(); Path config=directory.resolve("settings.properties");
            if(Files.notExists(config)) Files.writeString(config,"autoCheck=true\nchannel=beta\nautoInstall=false\n",StandardOpenOption.CREATE_NEW);
            try(var in=Files.newInputStream(config)) { properties.load(in); }
            active=new ServerUpdater(target,directory,installed,game,loader,properties);
        } catch(IOException | RuntimeException e) { AbsServerTool.LOGGER.info("AbsServerTool updater unavailable: {}",e.getMessage()); }
    }

    public static void stop() {
        var updater=active; active=null;
        if(updater!=null) updater.finish();
    }

    private void check(boolean install) {
        synchronized(this) { if(stopped || pending!=null) return; }
        try {
            if(install && candidate==null) throw new IOException("没有已检查的更新，请先运行 check。");
            if(!install) {
                try(var in=open(ReleaseCatalog.RELEASES,false)) {
                    byte[] bytes=in.readNBytes(2*1024*1024+1);
                    if(bytes.length>2*1024*1024) throw new IOException("Release metadata too large");
                    candidate=ReleaseCatalog.select(new String(bytes,StandardCharsets.UTF_8),installed,game,loader,beta);
                }
                report(candidate==null?"未找到适用于当前 Minecraft 版本和加载器的更新。":
                        "发现 "+candidate.tag()+"，运行 /absserver update install 可在停服后安装。");
            }
            if(candidate!=null && (install || autoInstall)) stage(candidate);
        } catch(Exception e) {
            if(!install) candidate=null;
            report("更新操作失败："+e.getMessage());
        }
    }

    private synchronized String submit(boolean install) {
        if(stopped || busy) return "更新任务正在执行。";
        if(pending!=null) return status;
        busy=true;
        worker.execute(()->{ try { check(install); } finally { synchronized(this) { busy=false; } } });
        return "更新任务已开始；用 /absserver update 查看结果。";
    }

    private void stage(ReleaseCatalog.Asset asset) throws IOException {
        synchronized(this) { if(stopped || pending!=null) return; }
        Path work=Files.createTempDirectory(directory,"install-"), jar=work.resolve("update.part");
        boolean ready=false;
        try {
            String oldHash=UpdateInstaller.hash(target);
            try(var in=open(asset.url(),true); var out=Files.newOutputStream(jar,StandardOpenOption.CREATE_NEW)) {
                long total=0; byte[] buffer=new byte[8192];
                for(int count;(count=in.read(buffer))!=-1;) {
                    if(Thread.currentThread().isInterrupted() || (total+=count)>asset.size()) throw new IOException("Invalid update download size");
                    out.write(buffer,0,count);
                }
                if(total!=asset.size()) throw new IOException("Incomplete update download");
            }
            if(!UpdateInstaller.hash(jar).equals(asset.sha256())) throw new IOException("Update SHA-256 mismatch");
            try(var archive=new JarFile(jar.toFile())) {
                String entry=loader.equals("fabric")?"abyssredemption/fabric/AbsServerToolFabric.class":
                        "abyssredemption/neoforge/AbsServerToolNeoForge.class";
                if(archive.getJarEntry(entry)==null) throw new IOException("Update JAR has the wrong loader");
            }
            synchronized(this) {
                if(stopped) return;
                pending=new Pending(work,jar,oldHash,asset.sha256());
                ready=true;
                report("已校验 "+asset.tag()+"。正常停服后安装，旧包保存在更新目录。请等待安装日志确认后再开服。");
            }
        } finally {
            if(!ready) { Files.deleteIfExists(jar); Files.deleteIfExists(work); }
        }
    }

    private synchronized void finish() {
        stopped=true; worker.shutdownNow();
        if(pending==null) return;
        try {
            String resource="abyssredemption/update/UpdateInstaller.class";
            Path helper=pending.work().resolve(resource);
            Files.createDirectories(helper.getParent());
            try(var in=ServerUpdater.class.getClassLoader().getResourceAsStream(resource)) {
                if(in==null) throw new IOException("Installer class missing");
                Files.copy(in,helper);
            }
            Path bin=Path.of(System.getProperty("java.home"),"bin");
            Path java=Files.isRegularFile(bin.resolve("java.exe"))?bin.resolve("java.exe"):bin.resolve("java");
            var backup=pending.work().resolve(target.getFileName()+".backup");
            Path log=pending.work().resolve("install.log");
            new ProcessBuilder(java.toString(),"-cp",pending.work().toString(),UpdateInstaller.class.getName(),
                    Long.toString(ProcessHandle.current().pid()),target.toString(),pending.jar().toString(),
                    backup.toString(),pending.oldHash(),pending.newHash())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            AbsServerTool.LOGGER.info("Update installer waiting for JVM exit; result: {}",log);
        } catch(IOException | RuntimeException e) { AbsServerTool.LOGGER.warn("Could not launch updater; current mod unchanged",e); }
    }

    private void report(String message) { status=message; AbsServerTool.LOGGER.info("AbsServerTool update: {}",message); }

    private static InputStream open(URI uri,boolean asset) throws IOException {
        for(int i=0;i<4;i++) {
            boolean trusted=asset?ReleaseCatalog.trusted(uri,"github.com") ||
                    ReleaseCatalog.trusted(uri,"release-assets.githubusercontent.com") ||
                    ReleaseCatalog.trusted(uri,"objects.githubusercontent.com"):ReleaseCatalog.trusted(uri,"api.github.com");
            if(!trusted) throw new IOException("Unexpected update host");
            var connection=(HttpURLConnection)uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000); connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent","AbsServerTool-Updater");
            if(!asset) connection.setRequestProperty("Accept","application/vnd.github+json");
            try {
                int code=connection.getResponseCode();
                if(asset && (code==301 || code==302 || code==303 || code==307 || code==308)) {
                    String location=connection.getHeaderField("Location");
                    connection.disconnect();
                    if(location==null) throw new IOException("Missing download redirect");
                    uri=uri.resolve(location); continue;
                }
                if(code!=200) throw new IOException("Update server returned HTTP "+code);
                return new FilterInputStream(connection.getInputStream()) {
                    private final long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(120);
                    @Override public int read(byte[] b,int offset,int length) throws IOException {
                        if(System.nanoTime()>deadline) throw new IOException("Update request timed out");
                        return super.read(b,offset,length);
                    }
                    @Override public void close() throws IOException { try { super.close(); } finally { connection.disconnect(); } }
                };
            } catch(IOException | RuntimeException e) { connection.disconnect(); throw e; }
        }
        throw new IOException("Too many update redirects");
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        var node=Commands.literal("update").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c->respond(c.getSource(),null));
        return node.then(Commands.literal("check").executes(c->respond(c.getSource(),false)))
                .then(Commands.literal("install").executes(c->respond(c.getSource(),true)));
    }
    private static int respond(CommandSourceStack source,Boolean install) {
        var updater=active;
        if(updater==null) { source.sendFailure(Component.literal("更新器仅支持从当前实例 mods 目录加载的独立 JAR。")); return 0; }
        String message=install==null?updater.status:updater.submit(install);
        source.sendSuccess(()->Component.literal(message),false);
        return 1;
    }
}
