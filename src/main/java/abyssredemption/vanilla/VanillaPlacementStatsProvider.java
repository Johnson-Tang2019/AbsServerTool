package abyssredemption.vanilla;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class VanillaPlacementStatsProvider {
    private final java.util.List<Item> blockItems;
    public VanillaPlacementStatsProvider(boolean includeModded) {
        blockItems = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof BlockItem)
                .filter(item -> includeModded || BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                .toList();
    }
    public long readOnline(ServerPlayer player) {
        long total = 0;
        for (Item item : blockItems) total += player.getStats().getValue(Stats.ITEM_USED, item);
        return total;
    }
    public long readUsed(com.google.gson.JsonObject used) {
        long total = 0;
        for (Item item : blockItems) {
            var id = BuiltInRegistries.ITEM.getKey(item).toString();
            var value = used.get(id);
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) total += Math.max(0, value.getAsLong());
        }
        return total;
    }
}
