@echo off
setlocal EnableDelayedExpansion

REM Minecraft クライアント起動スクリプト ( Windows cmd / PowerShell 用 )
REM
REM 使い方:
REM   run_client_windows.bat                通常 ( オンライン、 TLS workaround on )
REM   run_client_windows.bat offline        オフライン ( キャッシュ済み依存のみ )
REM   run_client_windows.bat notls          テザリング等 ( workaround off で素の TLS )
REM   run_client_windows.bat offline notls  併用可
REM   run_client_windows.bat keepdaemon     gradle daemon を kill しない
REM
REM 起動前に外部 mod ( libs/local/ 配下 ) を含めるか対話で尋ねる ( y/N )。

cd /d "%~dp0"

set GRADLE_ARGS=runClient
set USE_TLS_WORKAROUND=yes
set KILL_DAEMON=yes

for %%a in (%*) do (
    if /I "%%a"=="offline" (
        set GRADLE_ARGS=!GRADLE_ARGS! --offline -x downloadAssets
        echo === Offline mode ( using cached dependencies, skipping downloadAssets ) ===
    )
    if /I "%%a"=="notls"      ( set USE_TLS_WORKAROUND=no )
    if /I "%%a"=="no-tls"     ( set USE_TLS_WORKAROUND=no )
    if /I "%%a"=="keepdaemon" ( set KILL_DAEMON=no )
    if /I "%%a"=="keep-daemon"( set KILL_DAEMON=no )
)
if "%USE_TLS_WORKAROUND%"=="no" (
    echo === TLS workaround OFF ( デフォルト TLS で接続 ) ===
)

REM --- gradle daemon を停止 ( JVM 引数 / 環境変数の変更を確実に反映 ) ---
if "%KILL_DAEMON%"=="yes" (
    if exist gradlew.bat (
        echo === Stopping any running gradle daemon ^(--stop^) ===
        call gradlew.bat --stop >nul 2>&1
    )
)

REM --- TLS workaround ( Cisco Umbrella 対策 ) ---
REM JAVA_TOOL_OPTIONS は子 JVM ( gradle daemon / worker ) に自動伝播する。
set TLS_WORKAROUND_FILE=%CD%\tls_workaround.properties
if "%USE_TLS_WORKAROUND%"=="yes" (
    if exist "%TLS_WORKAROUND_FILE%" (
        set JAVA_TOOL_OPTIONS=-Djava.security.properties=%TLS_WORKAROUND_FILE% -Djdk.tls.client.protocols=TLSv1.2 -Dhttps.protocols=TLSv1.2 -Djdk.tls.client.cipherSuites=TLS_RSA_WITH_AES_256_GCM_SHA384,TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256
    )
)

REM --- 外部 mod 同梱の対話確認 ---
REM 環境変数で明示指定もできる: WITH_EXTERNAL_MODS=1 / SKIP_EXTERNAL_MODS_PROMPT=1
set USE_EXTERNAL_MODS=no
if defined WITH_EXTERNAL_MODS (
    set USE_EXTERNAL_MODS=yes
) else if defined WITH_SPELLBOOKS (
    set USE_EXTERNAL_MODS=yes
) else if defined SKIP_EXTERNAL_MODS_PROMPT (
    set USE_EXTERNAL_MODS=no
) else if defined SKIP_SPELLBOOKS_PROMPT (
    set USE_EXTERNAL_MODS=no
) else (
    set /p ANSWER=外部 mod をオンにしますか? [y/N]:
    if /I "!ANSWER!"=="y"   set USE_EXTERNAL_MODS=yes
    if /I "!ANSWER!"=="yes" set USE_EXTERNAL_MODS=yes
)

if "%USE_EXTERNAL_MODS%"=="yes" (
    set GRADLE_ARGS=!GRADLE_ARGS! -PwithExternalMods=true
    echo === 外部 mod ON ^( libs/local/ 配下の .jar を自動ロード ^) ===
    if exist libs\local (
        set FOUND=0
        for /R libs\local %%f in (*.jar) do (
            echo   ^-^> %%f
            set /a FOUND+=1
        )
        if "!FOUND!"=="0" (
            echo   ! libs\local\ に .jar がありません ^( ここに置けば自動取り込み ^)
        )
    ) else (
        echo   ! libs\local\ ディレクトリが存在しません — 作成して .jar を置いてください
    )
) else (
    echo === 外部 mod OFF ===
)

call gradlew.bat %GRADLE_ARGS%

endlocal
