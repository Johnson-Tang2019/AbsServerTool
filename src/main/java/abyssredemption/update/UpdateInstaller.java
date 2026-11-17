package abyssredemption.update;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/** Standalone JDK-only helper, copied outside the mod JAR before launch. */
public final class UpdateInstaller {
    private UpdateInstaller() {}
    public static void main(String[] args) throws Exception {
        var parent=ProcessHandle.of(Long.parseLong(args[0]));
        if(parent.isPresent()) parent.get().onExit().get(5,TimeUnit.MINUTES);
        Path target=Path.of(args[1]), staged=Path.of(args[2]), backup=Path.of(args[3]);
        install(target,staged,backup,args[4],args[5]);
        System.out.println("Update installed: "+target+"; backup: "+backup);
    }
    static void install(Path target,Path staged,Path backup,String oldHash,String newHash) throws IOException {
        if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS) || !hash(target).equals(oldHash))
            throw new IOException("Installed mod changed; update cancelled");
        if(!Files.isRegularFile(staged,LinkOption.NOFOLLOW_LINKS) || !hash(staged).equals(newHash))
            throw new IOException("Staged update checksum mismatch");
        Files.copy(target,backup);
        if(!hash(backup).equals(oldHash)) throw new IOException("Backup checksum mismatch");
        Path temporary=Files.createTempFile(target.getParent(),".absservertool-update-",".part");
        try {
            Files.copy(staged,temporary,StandardCopyOption.REPLACE_EXISTING);
            if(!hash(temporary).equals(newHash) || !hash(target).equals(oldHash))
                throw new IOException("Update or installed file changed during installation");
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
        Files.deleteIfExists(staged);
    }
    static String hash(Path file) throws IOException {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            try(var input=Files.newInputStream(file)) {
                byte[] buffer=new byte[8192];
                for(int count;(count=input.read(buffer))!=-1;) digest.update(buffer,0,count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch(NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
}
