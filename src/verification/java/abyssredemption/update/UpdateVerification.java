package abyssredemption.update;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class UpdateVerification {
    public static void main(String[] args) throws Exception {
        var catalog=new Gson().toJson(List.of(
                release("0.4.0-beta.2","fabric","26.3",false,true,true),
                release("0.4.0-beta.10","fabric","26.3",false,true,true),
                release("0.3.10","fabric","26.3",false,false,true),
                release("1.0.0","fabric","26.2",false,false,true),
                release("2.0.0","neoforge","26.3",false,false,true),
                release("3.0.0","fabric","26.3",true,false,true),
                release("4.0.0","fabric","26.3",false,false,false)));
        require(ReleaseCatalog.select(catalog,"0.3.9+26.3","26.3","fabric",true).tag().equals("v0.4.0-beta.10"),"Beta versions sort numerically");
        require(ReleaseCatalog.select(catalog,"0.3.9+26.3","26.3","fabric",false).tag().equals("v0.3.10"),"Stable channel excludes beta");
        require(ReleaseCatalog.select(catalog,"0.4.0","26.3","fabric",true)==null,"Never downgrade stable to beta");
        require(ReleaseCatalog.select(catalog,"2.0.0","26.3","neoforge",true)==null,"Never reinstall same version");
        Path dir=Files.createTempDirectory("absservertool-update-test-");
        Path target=dir.resolve("current.jar"), staged=dir.resolve("update.part"), backup=dir.resolve("previous.backup");
        Path helper=dir.resolve("abyssredemption/update/UpdateInstaller.class");
        try {
            Files.writeString(target,"old"); Files.writeString(staged,"new");
            String oldHash=UpdateInstaller.hash(target), newHash=UpdateInstaller.hash(staged);
            try { UpdateInstaller.install(target,staged,backup,oldHash,"0".repeat(64)); throw new AssertionError("Bad digest accepted"); }
            catch(java.io.IOException expected) { require(Files.readString(target).equals("old")&&!Files.exists(backup),"Failed validation leaves old JAR untouched"); }
            Files.createDirectories(helper.getParent());
            try(var input=UpdateInstaller.class.getResourceAsStream("UpdateInstaller.class")) { Files.copy(input,helper); }
            Path javaBin=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
            Process parent=new ProcessBuilder(javaBin.toString(),"-version").redirectErrorStream(true).start();
            require(parent.waitFor(10,TimeUnit.SECONDS),"Fixture process did not exit");
            var install=new ProcessBuilder(javaBin.toString(),"-cp",dir.toString(),UpdateInstaller.class.getName(),
                    Long.toString(parent.pid()),target.toString(),staged.toString(),backup.toString(),oldHash,newHash).inheritIO().start();
            if(!install.waitFor(20,TimeUnit.SECONDS)) { install.destroyForcibly(); throw new AssertionError("Installer timed out"); }
            require(install.exitValue()==0 && Files.readString(target).equals("new") && Files.readString(backup).equals("old") && Files.notExists(staged),"Standalone helper installs and backs up");
        } finally {
            Files.deleteIfExists(helper); Files.deleteIfExists(helper.getParent()); Files.deleteIfExists(dir.resolve("abyssredemption"));
            Files.deleteIfExists(staged); Files.deleteIfExists(backup); Files.deleteIfExists(target); Files.deleteIfExists(dir);
        }
        System.out.println("UPDATE_VERIFICATION_PASSED");
    }
    private static Map<String,Object> release(String version,String loader,String game,boolean draft,boolean beta,boolean verified) {
        String tag="v"+version, name="absservertool-"+loader+"-"+version+"+"+game+".jar";
        return Map.of("tag_name",tag,"draft",draft,"prerelease",beta,"assets",List.of(Map.of(
                "name",name,"size",128,"digest",verified?"sha256:"+"a".repeat(64):"","browser_download_url",
                "https://github.com/"+ReleaseCatalog.REPO+"/releases/download/"+tag+"/"+name)));
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
