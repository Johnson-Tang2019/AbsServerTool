package abyssredemption.regression;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.optimization.PiglinOptimization;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Real server and gold pickup -> admiration -> barter cycle; no Carpet present. */
public final class PiglinRegressionScenario {
    private final List<Piglin> stacked = new ArrayList<>();
    private Piglin isolated;
    private ItemEntity gold;
    private int ticks;
    private boolean admired;
    private boolean observedBarterLoot;
    private final java.util.Set<java.util.UUID> preexistingItems = new java.util.HashSet<>();
    public void started(MinecraftServer server) {
            var level=server.overworld(); level.setChunkForced(0,0,true); level.getChunk(0,0);
    }
    public void tick(MinecraftServer server) {
            ticks++;
            if (ticks==20) {
            command(server,"absserver piglinOptimization false");
            var level=server.overworld();
            level.getEntitiesOfClass(Piglin.class,new net.minecraft.world.phys.AABB(0,78,0,16,85,12)).forEach(Piglin::discard);
            level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(0,78,0,16,85,12)).forEach(ItemEntity::discard);
            for (int x=0;x<15;x++) for(int z=0;z<10;z++) level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
            for (int i=0;i<3;i++) stacked.add(spawn(server,4.5,4.5,i!=0));
            isolated=spawn(server,12.5,4.5,true);
            require(!PiglinOptimization.active(stacked.getFirst()),"Must start disabled");
            command(server,"absserver piglinOptimization limit 3");
            var node=server.getCommands().getDispatcher().getRoot().getChild("absserver").getChild("piglinOptimization");
            var restricted=new CommandSourceStack(CommandSource.NULL,Vec3.ZERO,Vec2.ZERO,level,
                    PermissionSet.NO_PERMISSIONS,Component.literal("restricted"),server);
            require(!node.canUse(restricted),"Non-operator can access command");
            command(server,"absserver piglinOptimization true");
            }
            if (ticks==60) {
                for (var piglin : stacked) {
                    var state=(PiglinOptimization.State)piglin;
                    AbsServerTool.LOGGER.info("Piglin fixture tick={} nearby={} generation={} expected={}",piglin.tickCount,
                            state.absservertool$nearbyPiglins(),state.absservertool$generation(),PiglinOptimization.generation());
                }
                require(stacked.stream().allMatch(PiglinOptimization::active),"Stack threshold not detected");
                require(!PiglinOptimization.active(isolated),"Isolated piglin affected");
                var zombie=EntityTypes.ZOMBIE.create(server.overworld(),EntitySpawnReason.COMMAND);
                require(!PiglinOptimization.active(zombie),"Other entity affected");
                var piglin=stacked.getFirst(); var position=piglin.position();
                piglin.move(MoverType.SELF,new Vec3(0.1,0,0)); require(piglin.position().equals(position),"Self movement not blocked");
                piglin.move(MoverType.PISTON,new Vec3(0.1,0,0)); require(!piglin.position().equals(position),"Piston movement blocked");
                require(wantsPickup(piglin,new ItemStack(Items.GOLD_INGOT)),"Gold acceptance blocked");
                require(!wantsPickup(piglin,new ItemStack(Items.GOLD_NUGGET)),"Non-gold pickup not suppressed");
                gold=new ItemEntity(server.overworld(),piglin.getX(),piglin.getY(),piglin.getZ(),new ItemStack(Items.GOLD_INGOT));
                gold.setNoPickUpDelay(); server.overworld().addFreshEntity(gold);
                server.overworld().getEntitiesOfClass(ItemEntity.class,piglin.getBoundingBox().inflate(16))
                        .forEach(item -> preexistingItems.add(item.getUUID()));
            }
            if (ticks>60) {
                var piglin=stacked.getFirst();
                admired |= piglin.getOffhandItem().is(Items.GOLD_INGOT);
                // Observe the emission, not its later location after random throw/fall motion.
                if (admired && gold.isRemoved() && piglin.getOffhandItem().isEmpty() && !observedBarterLoot) {
                    var emitted=server.overworld().getEntitiesOfClass(ItemEntity.class,piglin.getBoundingBox().inflate(4),
                            item -> !item.getItem().is(Items.GOLD_INGOT) && !preexistingItems.contains(item.getUUID()));
                    if (!emitted.isEmpty()) {
                        observedBarterLoot=true;
                        AbsServerTool.LOGGER.info("Piglin barter loot observed at tick {}: {} new item entities",ticks,emitted.size());
                    }
                }
            }
            if (ticks==240) {
                require(admired && gold.isRemoved(),"Natural gold pickup/admiration failed");
                require(stacked.getFirst().getOffhandItem().isEmpty(),"Barter never completed");
                require(observedBarterLoot,"Barter produced no observed loot");
                command(server,"absserver piglinOptimization false");
                require(stacked.stream().noneMatch(PiglinOptimization::active),"Disable did not immediately restore vanilla");
                ConfigManager.load(); require(!ConfigManager.get().piglinOptimization().enabled()
                        && ConfigManager.get().piglinOptimization().entityLimit()==3,"Config did not persist");
                var piglin=stacked.getFirst(); var position=piglin.position();
                piglin.move(MoverType.SELF,new Vec3(0.1,0,0)); require(!piglin.position().equals(position),"Vanilla movement not restored");
                command(server,"absserver piglinOptimization limit 1");
                command(server,"absserver piglinOptimization true");
            }
            if (ticks==250) {
                require(PiglinOptimization.active(isolated),"Changed threshold not applied");
                command(server,"absserver piglinOptimization limit 10000");
                require(!PiglinOptimization.active(isolated),"Changed threshold used stale count");
                command(server,"absserver piglinOptimization false");
                command(server,"absserver piglinOptimization limit 100");
                stacked.forEach(Piglin::discard); isolated.discard();
                server.overworld().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(-16,78,-16,32,85,32)).forEach(ItemEntity::discard);
                AbsServerTool.LOGGER.info("PIGLIN_REGRESSION_PASSED: permissions, threshold, isolation, gold pickup, natural barter loot, immediate disable, persistence");
                server.halt(false);
            }
            if (ticks>270) throw new AssertionError("Piglin regression timed out");
    }
    private static Piglin spawn(MinecraftServer server,double x,double z,boolean noAi) {
        var piglin=EntityTypes.PIGLIN.create(server.overworld(),EntitySpawnReason.COMMAND);
        require(piglin!=null,"Cannot spawn piglin"); piglin.setImmuneToZombification(true); piglin.setNoAi(noAi);
        piglin.setCanPickUpLoot(true); piglin.setPos(x,80,z); server.overworld().addFreshEntity(piglin); return piglin;
    }
    private static boolean wantsPickup(Piglin piglin,ItemStack stack) {
        try { var method=PiglinAi.class.getDeclaredMethod("wantsToPickup",Piglin.class,ItemStack.class);
            method.setAccessible(true); return (boolean)method.invoke(null,piglin,stack);
        } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void command(MinecraftServer server,String command) {
        try { require(server.getCommands().getDispatcher().execute(command,server.createCommandSourceStack())==1,"Command failed: "+command); }
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e) { throw new AssertionError(e); }
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
