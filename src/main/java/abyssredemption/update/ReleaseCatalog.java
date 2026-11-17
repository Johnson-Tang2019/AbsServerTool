package abyssredemption.update;

import com.google.gson.*;
import java.net.URI;
import java.util.regex.Pattern;

/** Selects only newer, exact game/loader artifacts from the official repository. */
final class ReleaseCatalog {
    static final String REPO = "Johnson-Tang2019/AbsServerTool";
    static final URI RELEASES = URI.create("https://api.github.com/repos/" + REPO + "/releases?per_page=50");
    static final long MAX_JAR = 32L * 1024 * 1024;
    private static final Pattern VERSION = Pattern.compile("(\\d{1,8})\\.(\\d{1,8})\\.(\\d{1,8})(?:-beta\\.(\\d{1,8}))?");
    record Version(int major,int minor,int patch,int beta) implements Comparable<Version> {
        static Version parse(String text) {
            var m=VERSION.matcher(text);
            if(!m.matches()) return null;
            return new Version(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),
                    m.group(4)==null?Integer.MAX_VALUE:Integer.parseInt(m.group(4)));
        }
        public int compareTo(Version other) {
            int c=Integer.compare(major,other.major);
            if(c==0)c=Integer.compare(minor,other.minor);
            if(c==0)c=Integer.compare(patch,other.patch);
            return c==0?Integer.compare(beta,other.beta):c;
        }
    }
    record Asset(String tag,String name,URI url,String sha256,long size) {}
    static Asset select(String json,String installed,String game,String loader,boolean beta) {
        var current=Version.parse(installed.split("\\+",2)[0]);
        if(current==null) throw new IllegalArgumentException("Unsupported installed version: "+installed);
        Version best=current; Asset selected=null;
        for(var entry:JsonParser.parseString(json).getAsJsonArray()) {
            try {
                var release=entry.getAsJsonObject();
                if(release.get("draft").getAsBoolean()) continue;
                String tag=release.get("tag_name").getAsString();
                if(!tag.startsWith("v")) continue;
                String version=tag.substring(1); var candidate=Version.parse(version);
                if(candidate==null || candidate.compareTo(best)<=0) continue;
                boolean isBeta=candidate.beta()!=Integer.MAX_VALUE;
                if(!beta && (isBeta || release.get("prerelease").getAsBoolean()) || !isBeta && release.get("prerelease").getAsBoolean()) continue;
                String name="absservertool-"+loader+"-"+version+"+"+game+".jar";
                for(var element:release.getAsJsonArray("assets")) {
                    var asset=element.getAsJsonObject();
                    if(!name.equals(asset.get("name").getAsString())) continue;
                    String digest=asset.get("digest").getAsString();
                    long size=asset.get("size").getAsLong();
                    if(!digest.matches("sha256:[a-f0-9]{64}") || size<1 || size>MAX_JAR) continue;
                    var url=URI.create(asset.get("browser_download_url").getAsString());
                    if(!trusted(url,"github.com") || !("/"+REPO+"/releases/download/"+tag+"/"+name).equals(url.getPath())) continue;
                    selected=new Asset(tag,name,url,digest.substring(7),size); best=candidate; break;
                }
            } catch(RuntimeException ignored) {
                // Incomplete or unverified release metadata cannot be installed.
            }
        }
        return selected;
    }
    static boolean trusted(URI uri,String host) {
        return "https".equals(uri.getScheme()) && host.equals(uri.getHost()) && uri.getUserInfo()==null
                && (uri.getPort()==-1 || uri.getPort()==443) && uri.getFragment()==null;
    }
}
