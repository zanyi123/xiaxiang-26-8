#!/bin/bash
# ============================================================
# 薪火侨乡项目一键部署脚本
# 用法: bash deploy.sh <服务器公网IP>
# 注意: 请在服务器上运行此脚本
# ============================================================

set -e

SERVER_IP=${1:-"127.0.0.1"}
PROJECT_NAME="xiaxiang"
PROJECT_DIR="/opt/$PROJECT_NAME"
APP_PORT=8080

echo "========================================"
echo "  薪火侨乡项目部署脚本"
echo "  服务器IP: $SERVER_IP"
echo "========================================"

# --------------------------------------------------
# 1. 安装依赖
# --------------------------------------------------
echo "[1/7] 安装系统依赖..."
apt-get update -qq
apt-get install -y -qq \
    openjdk-17-jdk \
    maven \
    nginx \
    git \
    curl \
    unzip \
    2>/dev/null || true

# 验证Java
if ! command -v java &> /dev/null; then
    echo "错误: Java安装失败"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
echo "  Java版本: $JAVA_VERSION"

# --------------------------------------------------
# 2. 创建项目目录
# --------------------------------------------------
echo "[2/7] 创建项目目录..."
mkdir -p $PROJECT_DIR

# --------------------------------------------------
# 3. 克隆代码
# --------------------------------------------------
echo "[3/7] 拉取项目代码..."
if [ -d "$PROJECT_DIR/.git" ]; then
    cd $PROJECT_DIR
    git pull origin main
else
    git clone https://github.com/zanyi123/xiaxiang-26-8.git $PROJECT_DIR
fi

# --------------------------------------------------
# 4. 创建 application.yml（从模板复制）
# --------------------------------------------------
echo "[4/7] 配置 application.yml..."
cd $PROJECT_DIR

if [ ! -f "src/main/resources/application.yml" ]; then
    if [ -f "src/main/resources/application.yml.example" ]; then
        cp src/main/resources/application.yml.example src/main/resources/application.yml
        echo "  已从 example 模板创建 application.yml"
        echo "  ⚠️  请手动编辑 src/main/resources/application.yml 填入你的腾讯云COS密钥！"
    else
        echo "错误: 未找到 application.yml 或模板文件"
        exit 1
    fi
else
    echo "  application.yml 已存在，跳过创建"
fi

# 提示用户修改配置
echo ""
echo "========================================"
echo "  ⚠️  重要：请修改COS配置"
echo "========================================"
echo "  文件路径: $PROJECT_DIR/src/main/resources/application.yml"
echo "  需要修改:"
echo "    cos.secret-id     -> 你的腾讯云SecretId"
echo "    cos.secret-key    -> 你的腾讯云SecretKey"
echo "    cos.bucket-name   -> 你的COS存储桶名称"
echo "    app.mock-mode     -> true(本地模式) / false(COS模式)"
echo "========================================"
echo ""

# --------------------------------------------------
# 5. Maven构建
# --------------------------------------------------
echo "[5/7] Maven构建项目..."
cd $PROJECT_DIR
mvn clean package -DskipTests -q

if [ ! -f "target/xiaxiang-1.0-SNAPSHOT.jar" ]; then
    echo "错误: 构建失败，未找到jar包"
    exit 1
fi
echo "  构建成功: target/xiaxiang-1.0-SNAPSHOT.jar"

# --------------------------------------------------
# 6. 创建Systemd服务
# --------------------------------------------------
echo "[6/7] 配置Systemd服务..."

cat > /