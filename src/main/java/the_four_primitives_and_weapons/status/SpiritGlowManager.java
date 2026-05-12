package the_four_primitives_and_weapons.status;

import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * ScoreboardTeam を使って霊視用の色付き発光を管理する。
 *
 * Minecraft の発光エフェクト（Glowing）はチームカラーに従って色が変わるため、
 * 敵対/中立/友好それぞれのチームにエンティティを動的に割り当てる。
 */
public class SpiritGlowManager {

    /**
     * 対象エンティティに霊視発光を適用する。
     * - glowingTag を true にセット
     * - 対応するチームに追加してカラーを適用
     */
    public static void setGlow(LivingEntity target, SpiritGlowType glowType) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        target.setGlowingTag(true);

        ServerScoreboard scoreboard = serverLevel.getServer().getScoreboard();
        ensureTeamExists(scoreboard, glowType);

        String entityName = target.getStringUUID();
        scoreboard.addPlayerToTeam(entityName,
                scoreboard.getPlayerTeam(glowType.getTeamName()));
    }

    /**
     * 霊視発光を解除する。
     */
    public static void clearGlow(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        target.setGlowingTag(false);

        ServerScoreboard scoreboard = serverLevel.getServer().getScoreboard();
        String entityName = target.getStringUUID();

        for (SpiritGlowType type : SpiritGlowType.values()) {
            PlayerTeam team = scoreboard.getPlayerTeam(type.getTeamName());
            if (team != null && team.getPlayers().contains(entityName)) {
                scoreboard.removePlayerFromTeam(entityName, team);
                break;
            }
        }
    }

    /**
     * 霊視用チームが存在しない場合は作成する。
     */
    private static void ensureTeamExists(Scoreboard scoreboard, SpiritGlowType glowType) {
        PlayerTeam team = scoreboard.getPlayerTeam(glowType.getTeamName());
        if (team == null) {
            team = scoreboard.addPlayerTeam(glowType.getTeamName());
            team.setColor(glowType.getColor());
            team.setSeeFriendlyInvisibles(false);
            team.setNameTagVisibility(Team.Visibility.NEVER);
        }
    }
}
