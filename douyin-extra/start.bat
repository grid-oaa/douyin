@echo off
chcp 65001 >nul
REM Douyin Live Recorder - Windows Startup Script

echo ========================================
echo Douyin Live Recorder
echo ========================================
echo.

REM Check Java installation
echo [1/4] Checking Java environment...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install Java 17 or higher
    echo Download: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo [OK] Java environment check passed
echo.

REM Check FFmpeg installation
echo [2/4] Checking FFmpeg...
where ffmpeg >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] FFmpeg not found. Recording will not work.
    echo Please install FFmpeg and add it to system PATH
    echo Download: https://ffmpeg.org/download.html
    echo.
    echo Continue anyway? (Y/N)
    set /p continue=
    if /i not "%continue%"=="Y" exit /b 1
) else (
    echo [OK] FFmpeg check passed
)
echo.

REM Check JAR file
echo [3/4] Checking application...
if not exist "target\douyin-live-recorder-1.0.0.jar" (
    echo [INFO] JAR file not found. Building...
    call mvnw.cmd clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [ERROR] Build failed
        pause
        exit /b 1
    )
    echo [OK] Build completed
) else (
    echo [OK] Application ready
)
echo.

REM Start application
echo [4/4] Starting application...
echo.
echo ========================================
echo Service URL: http://localhost:8080
echo Health Check: http://localhost:8080/actuator/health
echo Press Ctrl+C to stop
echo ========================================
echo.

java -jar target\douyin-live-recorder-1.0.0.jar

pause
