@echo off
setlocal enabledelayedexpansion

echo TaCZ Test - Windows Native
echo ==========================
echo.

REM カレントディレクトリを確認
echo Current directory: %CD%

REM TaCZ確認
if exist "compile-mods-1.19.2\tacz-1.1.4.jar" (
    echo [OK] TaCZ found: compile-mods-1.19.2\tacz-1.1.4.jar
) else (
    echo [ERROR] TaCZ not found in compile-mods-1.19.2\
    dir compile-mods-1.19.2\
    pause
    exit /b 1
)

REM Java確認
echo.
echo Checking Java versions...
echo Java in PATH:
java -version
echo.

REM Gradle Wrapper作成（存在しない場合）
if not exist "gradlew.bat" (
    echo Creating gradlew.bat...
    echo @echo off > gradlew.bat
    echo java -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain %%* >> gradlew.bat
)

REM ビルドとクライアント実行
echo.
echo Building and running client...
echo This will take several minutes...
echo.

REM Java 17が必要だがJava 21があるので、強制的に使用
set JAVA_HOME=
java -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon runClient

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Build/Run failed. Trying build only...
    java -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon build
    
    if %ERRORLEVEL% EQU 0 (
        echo Build successful! Attempting to run client...
        java -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon runClient
    )
)

echo.
pause