#!/bin/bash
# 抖音直播视频提取器 - Linux/macOS启动脚本

echo "========================================"
echo "抖音直播视频提取器"
echo "Douyin Live Recorder"
echo "========================================"
echo ""

# 检查Java是否安装
echo "[1/4] 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "[错误] 未检测到Java，请先安装Java 17或更高版本"
    echo "下载地址: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi
echo "[成功] Java环境检测通过"
echo ""

# 检查FFmpeg是否安装
echo "[2/4] 检查FFmpeg..."
if ! command -v ffmpeg &> /dev/null; then
    echo "[警告] 未检测到FFmpeg，录制功能将无法使用"
    echo "请安装FFmpeg："
    echo "  Ubuntu/Debian: sudo apt install ffmpeg"
    echo "  CentOS/RHEL:   sudo yum install ffmpeg"
    echo "  macOS:         brew install ffmpeg"
    echo ""
    read -p "是否继续启动？(y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "[成功] FFmpeg检测通过"
fi
echo ""

# 检查JAR文件是否存在
echo "[3/4] 检查应用程序..."
if [ ! -f "target/douyin-live-recorder-1.0.0.jar" ]; then
    echo "[提示] 未找到JAR文件，正在构建..."
    ./mvnw clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "[错误] 构建失败"
        exit 1
    fi
    echo "[成功] 构建完成"
else
    echo "[成功] 应用程序已就绪"
fi
echo ""

# 启动应用
echo "[4/4] 启动应用程序..."
echo ""
echo "========================================"
echo "服务地址: http://localhost:8080"
echo "健康检查: http://localhost:8080/actuator/health"
echo "按 Ctrl+C 停止服务"
echo "========================================"
echo ""

java -jar target/douyin-live-recorder-1.0.0.jar
