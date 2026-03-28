package minecraftarmorweapon.client;

import java.util.HashSet;
import java.util.Set;

/**
 * クライアント側でガード中のエンティティを追跡する
 * サーバーからのGuardSyncPacketで更新される
 */
public class GuardClientState {
    private static final Set<Integer> guardingEntities = new HashSet<>();

    public static void setGuarding(int entityId, boolean guarding) {
        if (guarding) {
            guardingEntities.add(entityId);
        } else {
            guardingEntities.remove(entityId);
        }
    }

    public static boolean isGuarding(int entityId) {
        return guardingEntities.contains(entityId);
    }
}
