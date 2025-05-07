// package minecraftarmorweapon.util;

// import com.google.gson.Gson;
// import com.google.gson.JsonObject;

// import java.io.InputStreamReader;
// import java.io.Reader;
// import java.nio.charset.StandardCharsets;

// public class GuideBookReader {
//     /**
//      * JSONファイルを読み込むユーティリティメソッド
//      *
//      * @param fileName JSONファイル名
//      * @return JsonObject ガイドブックのデータ
//      */
//     public static JsonObject loadGuideBook(String fileName) {
//         Gson gson = new Gson();
//         try (Reader reader = new InputStreamReader(
//                 GuideBookReader.class.getResourceAsStream("/data/minecraftarmorweapon/guidebooks/" + fileName),
//                 StandardCharsets.UTF_8)) {
//             return gson.fromJson(reader, JsonObject.class);
//         } catch (Exception e) {
//             e.printStackTrace();
//             return null;
//         }
//     }
// }
