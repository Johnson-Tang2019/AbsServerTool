package abyssredemption.stats;

import abyssredemption.AbsServerTool;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;

public final class ModStats {
    public static final Identifier BLOCKS_PLACED = register("blocks_placed", StatFormatter.DEFAULT);

    private ModStats() {}

    private static Identifier register(String path, StatFormatter formatter) {
        Identifier id = Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, path);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
        net.minecraft.stats.Stats.CUSTOM.get(id, formatter);
        return id;
    }

    public static void initialize() {}
}
