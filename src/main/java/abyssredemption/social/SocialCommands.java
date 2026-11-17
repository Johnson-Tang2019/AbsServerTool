package abyssredemption.social;

import abyssredemption.social.SocialModels.Board;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;

public final class SocialCommands {
    private SocialCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> pigeon() {
        return node("pigeon",Board.PIGEON_ALL).then(node("absent",Board.PIGEON_ABSENT)).then(node("flash",Board.PIGEON_FLASH));
    }
    public static LiteralArgumentBuilder<CommandSourceStack> street() { return node("street",Board.STREET_WANDERER); }
    private static LiteralArgumentBuilder<CommandSourceStack> node(String name,Board board) {
        return Commands.literal(name).requires(abyssredemption.social.network.SocialNetworking::mayView)
                .executes(c->show(c.getSource(),board,1))
                .then(Commands.argument("page",IntegerArgumentType.integer(1)).executes(c->show(c.getSource(),board,IntegerArgumentType.getInteger(c,"page"))))
                .then(Commands.literal("player").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("nameOrUuid",StringArgumentType.word()).executes(c->details(c.getSource(),board,StringArgumentType.getString(c,"nameOrUuid")))));
    }
    private static int show(CommandSourceStack source,Board board,int number) {
        if(!SocialRuntime.available(source.getServer())) return fail(source,"趣味榜已关闭或观察数据不可用，请检查服务端日志。");
        var config=abyssredemption.config.ConfigManager.get().socialLeaderboards();
        if(board==Board.STREET_WANDERER?!config.wanderer().enabled():!config.pigeon().enabled()) return fail(source,"该趣味榜已关闭。");
        var runtime=SocialRuntime.get(source.getServer());
        runtime.query(result->{
            try {
                var page=runtime.page(result,board,number);
                source.sendSuccess(()->Component.literal(board==Board.STREET_WANDERER?"========== 街溜子榜 ==========":"========== 鸽子榜 =========="),false);
                for(var e:page.entries()) source.sendSuccess(()->Component.literal("#"+e.rank()+" "+e.playerName()+" · "+e.score()+" 分 · "+
                        (e.subtype()==1?"潜水鸽 · "+e.daysSinceLastSeen()+" 天未登录":e.subtype()==2?"闪现鸽 · "+e.shortVisits()+" 次短暂访问":
                                String.format(java.util.Locale.ROOT,"徒步 %.2f km · 可观测操作 %.1f/h",e.onFootDistanceCm()/100000.0,e.observableActions()/Math.max(0.01,e.windowPlayTimeTicks()/72000.0)))),false);
                if(page.entries().isEmpty()) source.sendSuccess(()->Component.literal("暂无符合条件的玩家 / 数据观察中。默认需要至少 14 天的可靠快照。"),false);
                source.sendSuccess(()->Component.literal("第 "+page.page()+" / "+page.totalPages()+" 页 · "+(page.complete()?"观察窗口完整":"部分玩家数据仍在观察中")),false);
                source.sendSuccess(()->Component.literal("娱乐榜仅反映原版可观测活动，不代表贡献或责任，不用于处罚。"),false);
            } catch(IllegalArgumentException e) { fail(source,"页码超出范围。"); }
        },reason->fail(source,reason)); return 1;
    }
    private static int details(CommandSourceStack source,Board board,String identity) {
        if(!SocialRuntime.available(source.getServer())) return fail(source,"趣味榜不可用。");
        SocialRuntime.get(source.getServer()).query(result->{
            java.util.UUID id=null; try { id=java.util.UUID.fromString(identity); } catch(IllegalArgumentException ignored) { }
            if(id==null) for(var entry:result.names().entrySet()) if(entry.getValue().equalsIgnoreCase(identity)) { id=entry.getKey(); break; }
            var verdict=(board==Board.STREET_WANDERER?result.wanderer():result.pigeon()).get(id);
            var selected=verdict;
            source.sendSuccess(()->Component.literal(selected==null?"未观察到该玩家。":identity+" · "+selected.state()+" · "+selected.reason()+" · score="+selected.score()+" · activeDays="+selected.activeDays()+" · shortVisits="+selected.shortVisits()),false);
        },reason->fail(source,reason)); return 1;
    }
    private static int fail(CommandSourceStack source,String message) { source.sendFailure(Component.literal(message)); return 0; }
}
