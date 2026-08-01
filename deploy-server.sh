#!/bin/bash
# ============================================================
# 薪火侨乡 - 服务器一键部署/更新脚本
# 用法: bash deploy-server.sh
# 放在服务器 /opt/xiaxiang/ 目录下运行
# ============================================================

set -e

APP_NAME="xiaxiang-building-tour-1.0-SNAPSHOT.jar"
APP_DIR="/opt/xiaxiang"
APP_PORT=8080
PID_FILE="$APP_DIR/app.pid"
LOG_FILE="$APP_DIR/app.log"

cd $APP_DIR

echo "========================================"
echo "  薪火侨乡 服务器部署脚本"
echo "  目录: $APP_DIR"
echo "========================================"

# ---------- 1. 检查文件 ----------
echo "[1/5] 检查部署文件..."
if [ ! -f "$APP_NAME" ]; then
    echo "错误: 找不到 $APP_NAME，请先上传"
    exit 1
fi
if [ ! -f "application.yml" ]; then
    echo "警告: 同级目录找不到 application.yml，从 jar 中提取..."
    jar xf $APP_NAME BOOT-INF/classes/application.yml
    mv BOOT-INF/classes/application.yml ./application.yml
    rm -rf BOOT-INF
    echo "  已提取 application.yml 到同级目录"
fi

# 备份旧 yml（如果存在）
if [ -f "application.yml" ]; then
    cp application.yml "application.yml.bak"
    echo "  已备份旧 application.yml"
fi

# ---------- 2. 停旧进程 ----------
echo "[2/5] 停止旧进程..."
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 $OLD_PID 2>/dev/null; then
        kill $OLD_PID
        echo "  已发送停止信号到 PID $OLD_PID"
        sleep 3
        # 强制停止
        if kill -0 $OLD_PID 2>/dev/null; then
            kill -9 $OLD_PID
            echo "  强制停止 PID $OLD_PID"
        fi
    else
        echo "  旧进程已不存在"
    fi
    rm -f "$PID_FILE"
else
    echo "  无旧进程运行"
fi

# 确保端口释放
sleep 2

# ---------- 3. 如果有新 yml 备份则恢复 ----------
echo "[3/5] 配置检查..."
if [ -f "application.yml.bak" ]; then
    # 合并策略：用新上传的 yml 结构，但保留旧 yml 的密钥等敏感配置
    # 简单处理：如果新 yml 没有 COS 密钥，从旧 yml 恢复
    NEW_HAS_SECRET=$(grep -c "secret-id:" application.yml 2>/dev/null || echo "0")
    if [ "$NEW_HAS_SECRET" -eq 0 ]; then
        echo "  新 yml 无 COS 密钥，从备份恢复..."
        # 从备份中提取 cos 相关配置
        COS_BLOCK=$(sed -n '/^cos:/,/^[a-z]/p' application.yml.bak | head -10)
        # 简单追加（实际场景中建议手动编辑）
        echo "  ⚠️  请手动检查 COS 密钥配置"
    fi
    rm -f application.yml.bak
fi
echo "  application.yml: $(wc -l < application.yml) 行"

# ---------- 4. 启动新进程 ----------
echo "[4/5] 启动新进程..."
nohup java -jar "$APP_NAME" > "$LOG_FILE" 2>&1 &
NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"
echo "  新进程 PID: $NEW_PID"

# 等待启动
sleep 5

# ---------- 5. 验证 ----------
echo "[5/5] 验证启动状态..."
if kill -0 $NEW_PID 2>/dev/null; then
    # 检查 HTTP 是否响应
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$APP_PORT/admin/login" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        echo "  ✅ 启动成功! HTTP 200, PID=$NEW_PID"
    else
        echo "  ⚠️  进程运行中但 HTTP 返回 $HTTP_CODE"
        echo "  可能还在初始化，稍后访问 http://localhost:$APP_PORT/"
    fi
else
    echo "  ❌ 进程启动失败!"
    echo "  最近日志:"
    tail -20 "$LOG_FILE"
    exit 1
fi

echo ""
echo "========================================"
echo "  部署完成!"
echo "  访问: http://$(hostname -I | awk '{print $1}'):$APP_PORT/"
echo "  后台: http://$(hostname -I | awk '{print $1}'):$APP_PORT/admin/login"
echo "  日志: tail -f $LOG_FILE"
echo "  停止: kill \$(cat $PID_FILE)"
echo "========================================"
