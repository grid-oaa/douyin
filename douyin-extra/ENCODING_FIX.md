# Windows中文乱码问题解决方案

## 问题描述

在Windows CMD终端中运行 `start.bat` 时，中文显示为乱码，例如：
```
鎶栭煶鐩存挱瑙嗛鎻愬彇鍣?
```

## 原因

Windows CMD默认使用GBK编码（代码页936），而脚本文件使用UTF-8编码，导致中文显示乱码。

---

## 解决方案

### 方案一：使用英文版启动脚本（推荐）

已更新 `start.bat` 为英文版本，避免编码问题：

```bash
start.bat
```

输出示例：
```
========================================
Douyin Live Recorder
========================================

[1/4] Checking Java environment...
[OK] Java environment check passed

[2/4] Checking FFmpeg...
[OK] FFmpeg check passed

[3/4] Checking application...
[OK] Application ready

[4/4] Starting application...
```

### 方案二：使用中文版启动脚本

如果需要中文提示，使用 `start-cn.bat`：

```bash
start-cn.bat
```

该脚本已添加 `chcp 65001` 命令来设置UTF-8编码。

### 方案三：手动设置终端编码

在运行脚本前，先设置终端编码：

```bash
chcp 65001
start.bat
```

### 方案四：使用PowerShell（推荐）

PowerShell对UTF-8支持更好：

```powershell
# 打开PowerShell
powershell

# 运行启动脚本
.\start.bat
```

### 方案五：直接使用Maven命令

跳过启动脚本，直接使用Maven：

```bash
mvnw.cmd spring-boot:run
```

---

## 永久解决方案

### 修改Windows终端默认编码

1. **修改注册表**（需要管理员权限）：
   - 按 `Win + R`，输入 `regedit`
   - 导航到：`HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Command Processor`
   - 新建字符串值：`Autorun`
   - 值设为：`chcp 65001 >nul`

2. **重启终端**，所有CMD窗口将默认使用UTF-8编码

### 使用Windows Terminal（推荐）

Windows Terminal对UTF-8支持更好：

1. 从Microsoft Store安装 [Windows Terminal](https://aka.ms/terminal)
2. 在Windows Terminal中运行脚本，中文显示正常

---

## 各启动方式对比

| 方式 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| `start.bat` | 无编码问题，简单直接 | 英文提示 | ⭐⭐⭐⭐⭐ |
| `start-cn.bat` | 中文提示 | 可能仍有编码问题 | ⭐⭐⭐ |
| PowerShell | UTF-8支持好 | 需要额外步骤 | ⭐⭐⭐⭐ |
| Windows Terminal | 最佳体验 | 需要安装 | ⭐⭐⭐⭐⭐ |
| Maven直接运行 | 无脚本依赖 | 无环境检查 | ⭐⭐⭐⭐ |

---

## 快速测试

### 测试1：使用英文版脚本
```bash
start.bat
```

### 测试2：使用中文版脚本
```bash
start-cn.bat
```

### 测试3：使用PowerShell
```powershell
powershell
.\start.bat
```

### 测试4：直接使用Maven
```bash
mvnw.cmd spring-boot:run
```

---

## 常见问题

### Q1: start-cn.bat 仍然乱码怎么办？
**A:** 使用 `start.bat`（英文版）或在PowerShell中运行。

### Q2: 如何查看当前终端编码？
**A:** 在CMD中输入 `chcp`，显示当前代码页：
- 936 = GBK（中文）
- 65001 = UTF-8

### Q3: 修改注册表安全吗？
**A:** 安全，但建议先备份注册表。或者直接使用Windows Terminal。

### Q4: 应用启动后日志乱码怎么办？
**A:** 应用日志使用UTF-8编码，在以下环境中显示正常：
- Windows Terminal
- PowerShell
- 设置了UTF-8编码的CMD

---

## 推荐配置

### 最佳实践

1. **安装Windows Terminal**（推荐）
   ```
   从Microsoft Store搜索 "Windows Terminal" 并安装
   ```

2. **使用英文版启动脚本**
   ```bash
   start.bat
   ```

3. **或使用Maven直接运行**
   ```bash
   mvnw.cmd spring-boot:run
   ```

### 开发环境配置

如果使用IDE（IntelliJ IDEA、Eclipse等）：
- IDE内置终端通常支持UTF-8
- 直接在IDE中运行，无需担心编码问题

---

## 总结

- ✅ **推荐使用**: `start.bat`（英文版，无编码问题）
- ✅ **备选方案**: Windows Terminal + `start-cn.bat`
- ✅ **简单方案**: `mvnw.cmd spring-boot:run`

**注意**: 应用启动后的功能不受终端编码影响，只是启动脚本的提示信息显示问题。

---

## 相关文档

- [QUICK_START.md](QUICK_START.md) - 快速开始指南
- [USER_MANUAL.md](USER_MANUAL.md) - 完整使用手册
- [DEPLOYMENT.md](DEPLOYMENT.md) - 部署指南
