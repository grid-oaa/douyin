@echo off
chcp 65001 >nul
REM 抖音直播视频提取器 - Windows启动脚本

echo ========================================
echo 抖音直播视频提取器
echo Douyin Live Recorder
echo ========================================
echo.

REM 检查Java是否安装
echo [1/4] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Java，请先安装Java 17或更高版本
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo [成功] Java环境检测通过
echo.

REM 检查FFmpeg是否安装
echo [2/4] 检查FFmpeg...
where ffmpeg >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 未检测到FFmpeg，录制功能将无法使用
    echo 请安装FFmpeg并添加到系统PATH
    echo 下载地址: https://ffmpeg.org/download.html
    echo.
    echo 是否继续启动？(Y/N)
    set /p continue=
    if /i not "%continue%"=="Y" exit /b 1
) else (
    echo [成功] FFmpeg检测通过
)
echo.

REM 检查JAR文件是否存在
echo [3/4] 检查应用程序...
if not exist "target\douyin-live-recorder-1.0.0.jar" (
    echo [提示] 未找到JAR文件，正在构建...
    call mvnw.cmd clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [错误] 构建失败
        pause
        exit /b 1
    )
    echo [成功] 构建完成
) else (
    echo [成功] 应用程序已就绪
)
echo.

REM 启动应用
echo [4/4] 启动应用程序...
echo.
echo ========================================
echo 服务地址: http://localhost:8080
echo 健康检查: http://localhost:8080/actuator/health
echo 按 Ctrl+C 停止服务
echo ========================================
echo.

java -jar target\douyin-live-recorder-1.0.0.jar

pause
