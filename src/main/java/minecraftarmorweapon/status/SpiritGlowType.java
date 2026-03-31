package minecraftarmorweapon.status;

import net.minecraft.ChatFormatting;

/**
 * 霊視の発光色分類
 */
public enum SpiritGlowType {

    HOSTILE  ("spirit_hostile",  ChatFormatting.RED),     // 敵対 — 赤
    NEUTRAL  ("spirit_neutral",  ChatFormatting.YELLOW),  // 中立 — 黄
    FRIENDLY ("spirit_friendly", ChatFormatting.GREEN);   // 友好 — 緑

    private final String teamName;
    private final ChatFormatting color;

    SpiritGlowType(String teamName, ChatFormatting color) {
        this.teamName = teamName;
        this.color    = color;
    }

    public String getTeamName() { return teamName; }
    public ChatFormatting getColor()    { return color; }
}
