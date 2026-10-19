package abyssredemption.verification;

import abyssredemption.network.payload.WaypointCategories;
import abyssredemption.waypoint.WaypointCategoryService;
import java.nio.file.Files;
import java.util.Map;

public final class CategoryVerification {
    public static void main(String[] args) throws Exception {
        var request=new WaypointCategories.Request(2,1,true,6,3,"Homes");
        check(WaypointCategoryService.validateEdit(true,true,3,request)==0,"Admin allowed");
        check(WaypointCategoryService.validateEdit(true,false,3,request)==1,"Viewer denied");
        check(WaypointCategoryService.validateEdit(false,true,3,request)==7,"Disabled denied");
        check(WaypointCategoryService.validateEdit(true,true,4,request)==3,"Stale revision denied");
        check(WaypointCategoryService.validateEdit(true,true,3,new WaypointCategories.Request(2,1,true,16,3,"Name"))==4,"Invalid color denied");
        check(!WaypointCategories.validName("x".repeat(33)) && !WaypointCategories.validName("bad\nname") && !WaypointCategories.validName("bad\u00a7name"),"Invalid names denied");
        var directory=Files.createTempDirectory("waypoint-category-verification-");
        var file=directory.resolve("categories.json");
        try {
            check(WaypointCategoryService.load(file).equals(new WaypointCategoryService.Data(0,Map.of())),"New worlds have no names");
            var data=new WaypointCategoryService.Data(9,Map.of(0,"Structures",6,"Homes",15,"其他"));
            WaypointCategoryService.save(file,data);
            check(WaypointCategoryService.load(file).equals(data),"Names and revision survive restart");
            Files.writeString(file,"{broken"); boolean rejected=false;
            try { WaypointCategoryService.load(file); } catch(java.io.IOException expected) { rejected=true; }
            check(rejected && Files.readString(file).equals("{broken"),"Corrupt store must not be overwritten");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
        System.out.println("Category server verification passed: permissions, disabled state, conflict, validation, restart persistence and corrupt-store preservation.");
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
