package the_four_primitives_and_weapons.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PythonのAIプロセスを管理するクラス
 *
 * ProcessBuilderを使ってPythonスクリプトを起動し、
 * JSON形式でデータをやり取りします。
 */
public class PythonAIProcess {

    private static final String PYTHON_SCRIPT_PATH = "src/main/python/ai/ai_bridge_wrapper.py";
    private static final long RESPONSE_TIMEOUT_MS = 5000; // 5秒タイムアウト

    private Process pythonProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private BufferedReader processError;

    private final Gson gson = new Gson();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String pythonCommand = "python"; // デフォルトはpython

    /**
     * Pythonプロセスを起動
     */
    public boolean start() {
        if (isRunning.get()) {
            System.err.println("Python AI process is already running");
            return false;
        }

        try {
            // Pythonスクリプトのパスを確認
            Path scriptPath = findPythonScript();
            if (scriptPath == null) {
                System.err.println("Python script not found: " + PYTHON_SCRIPT_PATH);
                return false;
            }

            // Python実行可能ファイルを検索
            String pythonExec = findPythonExecutable();
            if (pythonExec == null) {
                System.err.println("Python executable not found. Please install Python 3.x");
                return false;
            }

            this.pythonCommand = pythonExec;

            // Pythonプロセスを起動
            ProcessBuilder pb = new ProcessBuilder(pythonExec, scriptPath.toString());
            pb.redirectErrorStream(false);

            pythonProcess = pb.start();

            // 入出力ストリームを設定
            processInput = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
            processOutput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
            processError = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));

            // エラー出力を監視
            startErrorMonitor();

            // 起動完了メッセージを待つ
            String readyMessage = processOutput.readLine();
            if (readyMessage != null) {
                JsonObject response = JsonParser.parseString(readyMessage).getAsJsonObject();
                if ("ready".equals(response.get("status").getAsString())) {
                    isRunning.set(true);                    return true;
                }
            }

            System.err.println("Failed to receive ready message from Python");
            shutdown();
            return false;

        } catch (IOException e) {
            System.err.println("Failed to start Python AI process: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Pythonスクリプトを探す
     */
    private Path findPythonScript() {
        // 複数のパスを試す
        String[] possiblePaths = {
            PYTHON_SCRIPT_PATH,
            "the_four_primitives_and_weapons/" + PYTHON_SCRIPT_PATH,
            "../" + PYTHON_SCRIPT_PATH,
            System.getProperty("user.dir") + "/" + PYTHON_SCRIPT_PATH
        };

        for (String pathStr : possiblePaths) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return path.toAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Python実行可能ファイルを探す
     */
    private String findPythonExecutable() {
        String[] pythonCommands = {"python3", "python", "py"};

        for (String cmd : pythonCommands) {
            try {
                Process testProcess = new ProcessBuilder(cmd, "--version").start();
                testProcess.waitFor(2, TimeUnit.SECONDS);

                if (testProcess.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception e) {
                // このコマンドは使えないので次を試す
            }
        }

        return null;
    }

    /**
     * エラー出力を監視
     */
    private void startErrorMonitor() {
        new Thread(() -> {
            try {
                String line;
                while ((line = processError.readLine()) != null) {
                    System.err.println("Python AI Error: " + line);
                }
            } catch (IOException e) {
                // プロセス終了時は無視
            }
        }).start();
    }

    /**
     * コマンドを送信してレスポンスを受け取る
     */
    public JsonObject sendCommand(JsonObject command) {
        if (!isRunning.get()) {
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("error_message", "Python process is not running");
            return error;
        }

        try {
            // コマンドを送信
            String commandJson = gson.toJson(command);
            processInput.write(commandJson);
            processInput.newLine();
            processInput.flush();

            // レスポンスを受信（タイムアウト付き）
            Future<String> future = executor.submit(() -> processOutput.readLine());

            String responseLine = future.get(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (responseLine != null) {
                return JsonParser.parseString(responseLine).getAsJsonObject();
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("status", "error");
                error.addProperty("error_message", "No response from Python");
                return error;
            }

        } catch (TimeoutException e) {
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("error_message", "Python response timeout");
            return error;

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("error_message", "Error communicating with Python: " + e.getMessage());
            e.printStackTrace();
            return error;
        }
    }

    /**
     * プロセスをシャットダウン
     */
    public void shutdown() {
        if (!isRunning.get()) {
            return;
        }

        isRunning.set(false);

        try {
            if (processInput != null) {
                processInput.close();
            }
            if (processOutput != null) {
                processOutput.close();
            }
            if (processError != null) {
                processError.close();
            }

            if (pythonProcess != null && pythonProcess.isAlive()) {
                pythonProcess.destroy();
                pythonProcess.waitFor(5, TimeUnit.SECONDS);

                if (pythonProcess.isAlive()) {
                    pythonProcess.destroyForcibly();
                }
            }

            executor.shutdown();
        } catch (Exception e) {
            System.err.println("Error shutting down Python process: " + e.getMessage());
        }
    }

    /**
     * プロセスが実行中か確認
     */
    public boolean isRunning() {
        return isRunning.get() && pythonProcess != null && pythonProcess.isAlive();
    }
}
