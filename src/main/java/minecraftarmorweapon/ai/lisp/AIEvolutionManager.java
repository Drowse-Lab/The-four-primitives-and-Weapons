package minecraftarmorweapon.ai.lisp;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

/**
 * 遺伝的プログラミングによるAI進化マネージャ。
 *
 * - Mobが死亡したとき、そのゲノム（S式）とフィットネスを記録
 * - 一定数たまったら世代交代（選択・交叉・突然変異）
 * - 新しいMobスポーン時に進化済みゲノムを配布
 * - ワールドに永続化（SavedData）
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class AIEvolutionManager {

    private static final int POPULATION_SIZE = 20;
    private static final int ELITE_COUNT = 4;
    private static final float MUTATION_RATE = 0.15f;

    // エンティティタイプ別のゲノムプール
    private static final Map<String, GenomePool> pools = new HashMap<>();

    // .lispファイルから読み込んだデフォルトゲノム（エンティティタイプ別）
    private static final Map<String, String> DEFAULT_GENOMES = new HashMap<>();

    // エンティティタイプ → .lispファイル名のマッピング
    private static final Map<String, String> ENTITY_TO_LISP = new HashMap<>();

    static {
        // どのエンティティがどの.lispファイルを使うか
        ENTITY_TO_LISP.put("minecraft:zombie", "zombie");
        ENTITY_TO_LISP.put("minecraft:husk", "zombie");
        ENTITY_TO_LISP.put("minecraft:drowned", "zombie");
        ENTITY_TO_LISP.put("minecraft:wither_skeleton", "zombie");
        ENTITY_TO_LISP.put("minecraft:skeleton", "skeleton");
        ENTITY_TO_LISP.put("minecraft:stray", "skeleton");
        ENTITY_TO_LISP.put("minecraft:creeper", "creeper");
        ENTITY_TO_LISP.put("minecraft:spider", "spider");
        ENTITY_TO_LISP.put("minecraft:cave_spider", "spider");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:common_soldier", "common_soldier");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:elite_soldier", "elite_soldier");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:singularity", "singularity");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:heroic_tier", "heroic_tier");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:mythical_tier", "mythical_tier");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:angel_tier", "angel_tier");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:divine_tier", "divine_tier");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:angel_serious", "angel_trio");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:angel_mocker1", "angel_trio");
        ENTITY_TO_LISP.put("minecraft_armor_weapon:angel_mocker2", "angel_trio");
    }

    // フォールバック（.lispファイルが読めなかった場合）
    private static final String FALLBACK_GENOME = "(if has-target (if (< distance 3) (attack) (approach)) (idle))";

    // === .lispファイルの読み込み ===

    private static final GenomeReloadListener RELOAD_LISTENER = new GenomeReloadListener();

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(RELOAD_LISTENER);
    }

    /**
     * data/<namespace>/ai_genomes/ 以下の.lispファイルを読み込むリスナー。
     */
    private static class GenomeReloadListener extends SimplePreparableReloadListener<Map<String, String>> {
        @Nonnull
        @Override
        protected Map<String, String> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
            Map<String, String> loaded = new HashMap<>();

            manager.listResources("ai_genomes", loc -> loc.getPath().endsWith(".lisp")).forEach((location, resource) -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    // ファイル名からキーを抽出 (例: "ai_genomes/zombie.lisp" → "zombie")
                    String path = location.getPath();
                    String key = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                    loaded.put(key, sb.toString());
                } catch (Exception e) {
                    // 読み込みエラーは無視
                }
            });

            return loaded;
        }

        @Override
        protected void apply(@Nonnull Map<String, String> prepared,
                             @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
            DEFAULT_GENOMES.clear();

            // .lispファイルの内容をエンティティタイプにマッピング
            for (Map.Entry<String, String> entityMapping : ENTITY_TO_LISP.entrySet()) {
                String entityTypeId = entityMapping.getKey();
                String lispFileName = entityMapping.getValue();
                String genome = prepared.get(lispFileName);
                if (genome != null && !genome.isBlank()) {
                    DEFAULT_GENOMES.put(entityTypeId, genome);
                }
            }

            // マッピングにないファイルも直接登録（カスタムMob用）
            for (Map.Entry<String, String> entry : prepared.entrySet()) {
                String key = entry.getKey();
                if (!ENTITY_TO_LISP.containsValue(key)) {
                    // ファイル名をそのままエンティティIDとして登録（例: "mymod_custom_mob" → 使えるように）
                    DEFAULT_GENOMES.putIfAbsent(key, entry.getValue());
                }
            }
        }
    }

    /**
     * 指定されたMobタイプのゲノムを取得する。
     * プールに進化済みゲノムがあればそこから、なければ.lispファイルのデフォルトを返す。
     */
    public static String getGenome(String entityTypeId) {
        GenomePool pool = pools.get(entityTypeId);
        if (pool != null && !pool.genomes.isEmpty()) {
            // プールからランダムに選択
            return pool.genomes.get(pool.random.nextInt(pool.genomes.size()));
        }
        return DEFAULT_GENOMES.getOrDefault(entityTypeId, FALLBACK_GENOME);
    }

    /**
     * Mobの死亡時にフィットネスとゲノムを記録する。
     */
    public static void reportDeath(String entityTypeId, String genome, float fitness) {
        GenomePool pool = pools.computeIfAbsent(entityTypeId, k -> new GenomePool());
        pool.deathRecords.add(new DeathRecord(genome, fitness));

        // 十分なサンプルがたまったら世代交代
        if (pool.deathRecords.size() >= POPULATION_SIZE) {
            evolve(pool);
        }
    }

    /**
     * 遺伝的プログラミングによる世代交代。
     */
    private static void evolve(GenomePool pool) {
        List<DeathRecord> records = pool.deathRecords;

        // フィットネス順にソート（降順）
        records.sort((a, b) -> Float.compare(b.fitness, a.fitness));

        // エリート選択
        List<String> newGenomes = new ArrayList<>();
        for (int i = 0; i < Math.min(ELITE_COUNT, records.size()); i++) {
            newGenomes.add(records.get(i).genome);
        }

        LispInterpreter parser = new LispInterpreter();

        // 残りを交叉・突然変異で生成
        while (newGenomes.size() < POPULATION_SIZE) {
            // トーナメント選択で親を2つ選ぶ
            String parent1 = tournamentSelect(records, pool.random);
            String parent2 = tournamentSelect(records, pool.random);

            // 交叉
            Object ast1 = parser.parse(parent1);
            Object ast2 = parser.parse(parent2);
            Object child = crossover(ast1, ast2, pool.random);

            // 突然変異
            if (pool.random.nextFloat() < MUTATION_RATE) {
                child = mutate(child, pool.random);
            }

            newGenomes.add(LispInterpreter.toSExpression(child));
        }

        pool.genomes = newGenomes;
        pool.deathRecords.clear();
        pool.generation++;
    }

    /**
     * トーナメント選択（3個体から最良を選ぶ）。
     */
    private static String tournamentSelect(List<DeathRecord> records, Random random) {
        DeathRecord best = null;
        for (int i = 0; i < 3; i++) {
            DeathRecord candidate = records.get(random.nextInt(records.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }
        return best.genome;
    }

    /**
     * 交叉 — 2つのASTからランダムなサブツリーを交換。
     */
    @SuppressWarnings("unchecked")
    private static Object crossover(Object ast1, Object ast2, Random random) {
        if (!(ast1 instanceof List) || !(ast2 instanceof List)) {
            return random.nextBoolean() ? ast1 : ast2;
        }

        List<Object> list1 = new ArrayList<>((List<Object>) ast1);
        List<Object> list2 = (List<Object>) ast2;

        if (list1.size() > 1 && list2.size() > 1) {
            // ランダムな位置のサブツリーを交換
            int pos1 = 1 + random.nextInt(list1.size() - 1);
            int pos2 = 1 + random.nextInt(list2.size() - 1);
            list1.set(pos1, deepCopy(list2.get(pos2)));
        }

        return list1;
    }

    /**
     * 突然変異 — ランダムなノードを変更。
     */
    @SuppressWarnings("unchecked")
    private static Object mutate(Object ast, Random random) {
        if (!(ast instanceof List)) return ast;

        List<Object> list = new ArrayList<>((List<Object>) ast);
        if (list.size() <= 1) return list;

        int mutationPoint = 1 + random.nextInt(list.size() - 1);
        Object target = list.get(mutationPoint);

        // 数値の場合: 値を微調整
        if (target instanceof Integer i) {
            list.set(mutationPoint, i + random.nextInt(5) - 2);
            return list;
        }
        if (target instanceof Double d) {
            list.set(mutationPoint, d + (random.nextDouble() - 0.5) * 0.4);
            return list;
        }

        // サブリストの場合: 再帰的に突然変異
        if (target instanceof List) {
            list.set(mutationPoint, mutate(target, random));
            return list;
        }

        // シンボルの場合: 同カテゴリのアクションに置換
        if (target instanceof LispInterpreter.Symbol sym) {
            String[] attackActions = {"attack", "charge-attack", "combo-attack", "ranged-attack"};
            String[] moveActions = {"approach", "retreat", "circle-strafe", "leap"};
            String[] defenseActions = {"dodge", "block-shield", "parry"};

            String name = sym.name;
            String[] category = null;
            for (String a : attackActions) if (a.equals(name)) { category = attackActions; break; }
            if (category == null) for (String a : moveActions) if (a.equals(name)) { category = moveActions; break; }
            if (category == null) for (String a : defenseActions) if (a.equals(name)) { category = defenseActions; break; }

            if (category != null) {
                list.set(mutationPoint, new LispInterpreter.Symbol(category[random.nextInt(category.length)]));
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopy(Object ast) {
        if (ast instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<Object>) ast) {
                copy.add(deepCopy(item));
            }
            return copy;
        }
        return ast; // アトムはイミュータブル
    }

    // === SavedData永続化 ===

    /**
     * ワールドから進化データを読み込む。
     */
    public static void loadFromWorld(ServerLevel level) {
        EvolutionSavedData data = level.getDataStorage().computeIfAbsent(
            EvolutionSavedData::load, EvolutionSavedData::new, "minecraft_armor_weapon_ai_evolution");
        pools.clear();
        pools.putAll(data.pools);
    }

    /**
     * ワールドに進化データを保存する。
     */
    public static void saveToWorld(ServerLevel level) {
        EvolutionSavedData data = level.getDataStorage().computeIfAbsent(
            EvolutionSavedData::load, EvolutionSavedData::new, "minecraft_armor_weapon_ai_evolution");
        data.pools.clear();
        data.pools.putAll(pools);
        data.setDirty();
    }

    /**
     * 指定タイプの現在の世代数を取得。
     */
    public static int getGeneration(String entityTypeId) {
        GenomePool pool = pools.get(entityTypeId);
        return pool != null ? pool.generation : 0;
    }

    // === 内部クラス ===

    private static class GenomePool {
        List<String> genomes = new ArrayList<>();
        List<DeathRecord> deathRecords = new ArrayList<>();
        int generation = 0;
        Random random = new Random();
    }

    private static class DeathRecord {
        final String genome;
        final float fitness;

        DeathRecord(String genome, float fitness) {
            this.genome = genome;
            this.fitness = fitness;
        }
    }

    // === SavedData ===

    public static class EvolutionSavedData extends SavedData {
        final Map<String, GenomePool> pools = new HashMap<>();

        public EvolutionSavedData() {}

        public static EvolutionSavedData load(CompoundTag tag) {
            EvolutionSavedData data = new EvolutionSavedData();

            CompoundTag poolsTag = tag.getCompound("pools");
            for (String key : poolsTag.getAllKeys()) {
                CompoundTag poolTag = poolsTag.getCompound(key);
                GenomePool pool = new GenomePool();
                pool.generation = poolTag.getInt("generation");

                // ゲノム一覧を読み込む
                if (poolTag.contains("genomes")) {
                    String genomesJson = poolTag.getString("genomes");
                    try {
                        JsonArray arr = JsonParser.parseString(genomesJson).getAsJsonArray();
                        for (JsonElement elem : arr) {
                            pool.genomes.add(elem.getAsString());
                        }
                    } catch (Exception e) {
                        // パースエラーは無視
                    }
                }

                data.pools.put(key, pool);
            }

            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            CompoundTag poolsTag = new CompoundTag();

            for (Map.Entry<String, GenomePool> entry : pools.entrySet()) {
                CompoundTag poolTag = new CompoundTag();
                GenomePool pool = entry.getValue();
                poolTag.putInt("generation", pool.generation);

                // ゲノム一覧をJSON配列として保存
                JsonArray arr = new JsonArray();
                for (String genome : pool.genomes) {
                    arr.add(genome);
                }
                poolTag.putString("genomes", arr.toString());

                poolsTag.put(entry.getKey(), poolTag);
            }

            tag.put("pools", poolsTag);
            return tag;
        }
    }
}
