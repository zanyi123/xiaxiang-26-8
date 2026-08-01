# -*- coding: utf-8 -*-
"""
薪火侨乡 - 腾讯云 COS 素材自动上传脚本
=========================================
用途：把本地素材文件（图片/音频/视频/3D模型）批量上传到 COS，
      自动生成对应的 key，并输出要填到 application.yml 的内容。

使用方法（Windows PowerShell）：
    # 1. 首次运行装依赖
    pip install cos-python-sdk-v5 pyyaml

    # 2. 上传单个文件
    python tools/upload_to_cos.py upload .\本地图片.jpg images

    # 3. 批量上传整个文件夹（例如所有建筑模型）
    python tools/upload_to_cos.py batch .\素材\models models

    # 4. 只看 key 预览（不上传）
    python tools/upload_to_cos.py preview .\素材\images images

上传完会自动输出：
    ✅ 已上传：本地图片.jpg
       → key: images/6f2...abc.jpg
       → YAML写法：
           cover-image: "images/6f2...abc.jpg"
"""

import os
import sys
import hashlib
from datetime import datetime

# ============================================================
# 读取 COS 配置（自动从 application.yml 读取，避免重复配置）
# ============================================================
def load_cos_config():
    """从 application.yml 读取 COS 配置"""
    try:
        import yaml
    except ImportError:
        print("❌ 缺少依赖 pyyaml，请先运行：pip install cos-python-sdk-v5 pyyaml")
        sys.exit(1)

    # 脚本目录的上两级是项目根
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    yml_path = os.path.join(project_root, "src", "main", "resources", "application.yml")

    if not os.path.exists(yml_path):
        print(f"❌ 找不到配置文件：{yml_path}")
        sys.exit(1)

    with open(yml_path, "r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)

    cos = cfg.get("cos", {})
    app = cfg.get("app", {})
    mock_mode = app.get("mock-mode", True)

    if mock_mode:
        print("⚠️  注意：当前 application.yml 中 app.mock-mode=true")
        print("   上传后请把 mock-mode 改成 false 才能看到真实 COS 素材\n")

    return (
        cos.get("secret-id"),
        cos.get("secret-key"),
        cos.get("region"),
        cos.get("bucket-name"),
        yml_path,
    )


def gen_cos_client(secret_id, secret_key, region):
    try:
        from qcloud_cos import CosConfig, CosS3Client
    except ImportError:
        print("❌ 缺少依赖 cos-python-sdk-v5，请先运行：")
        print("   pip install cos-python-sdk-v5 pyyaml")
        sys.exit(1)

    conf = CosConfig(Region=region, SecretId=secret_id, SecretKey=secret_key)
    return CosS3Client(conf)


# ============================================================
# 核心工具：生成文件唯一 key（内容哈希+扩展名，避免覆盖）
# ============================================================
def make_key(local_file, prefix):
    """生成 COS key：prefix/{sha1前12位}.{ext}
    好处：相同文件不会重复上传，不同文件不冲突。
    """
    ext = os.path.splitext(local_file)[1].lower()
    sha1 = hashlib.sha1()
    with open(local_file, "rb") as f:
        while True:
            chunk = f.read(8192)
            if not chunk:
                break
            sha1.update(chunk)
    digest = sha1.hexdigest()[:12]
    base = os.path.basename(local_file)
    stem = os.path.splitext(base)[0][:20].replace(" ", "-").replace("(", "").replace(")", "").replace("，", "").replace(",", "")
    # prefix/stem_digest.ext
    return f"{prefix.strip('/')}/{stem}_{digest}{ext}"


def key_exists(client, bucket, key):
    try:
        client.head_object(Bucket=bucket, Key=key)
        return True
    except Exception:
        return False


# ============================================================
# 单文件上传
# ============================================================
def upload_one(client, bucket, local_file, prefix, dry_run=False):
    if not os.path.isfile(local_file):
        print(f"❌ 文件不存在：{local_file}")
        return None

    key = make_key(local_file, prefix)
    size_mb = os.path.getsize(local_file) / (1024 * 1024)

    if dry_run:
        print(f"👁️  [预览] {local_file} ({size_mb:.1f}MB)")
        print(f"   → key: {key}")
        return key

    # 查重
    if key_exists(client, bucket, key):
        print(f"✅ 已存在，跳过：{os.path.basename(local_file)}")
        print(f"   → key: {key}")
        return key

    print(f"⬆️  上传中：{os.path.basename(local_file)} ({size_mb:.1f}MB)...", end="", flush=True)

    # 大文件分片上传
    if size_mb > 50:
        client.upload_file(
            Bucket=bucket,
            Key=key,
            LocalFilePath=local_file,
            PartSize=10,
            MAXThread=5,
            EnableMD5=False,
        )
    else:
        with open(local_file, "rb") as f:
            client.put_object(Bucket=bucket, Body=f, Key=key)

    print(" 完成")
    print(f"   → key: {key}")
    return key


# ============================================================
# 批量上传文件夹
# ============================================================
def batch_upload(client, bucket, local_dir, prefix, dry_run=False):
    if not os.path.isdir(local_dir):
        print(f"❌ 目录不存在：{local_dir}")
        return []

    # 允许的扩展名
    allow_ext = {
        # 图片
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
        # 音频
        ".mp3", ".wav", ".aac", ".m4a",
        # 视频
        ".mp4", ".mov", ".m4v",
        # 模型
        ".splat", ".ply", ".obj", ".glb", ".gltf", ".fbx",
    }

    files = []
    for root, _, names in os.walk(local_dir):
        for n in names:
            ext = os.path.splitext(n)[1].lower()
            if ext in allow_ext:
                files.append(os.path.join(root, n))

    if not files:
        print(f"⚠️  {local_dir} 下没有可上传的文件（支持 jpg/png/mp3/mp4/splat 等）")
        return []

    print(f"📁 发现 {len(files)} 个文件，开始批量上传...\n")
    results = []
    for f in sorted(files):
        # 子目录也放进 prefix
        rel = os.path.relpath(f, local_dir)
        sub = os.path.dirname(rel)
        sub_prefix = f"{prefix}/{sub}" if sub else prefix
        k = upload_one(client, bucket, f, sub_prefix, dry_run=dry_run)
        if k:
            results.append((f, k))
        print()
    return results


# ============================================================
# 输出 YAML 填写建议（告诉用户把 key 填到 application.yml 哪里）
# ============================================================
YAML_FIELD_HINTS = {
    "images": (
        "【图片类 key 用法】\n"
        "  建筑/地点/故事 封面 → cover-image 或 coverImage 或 image-key\n"
        "  建筑解剖图        → image-key\n"
        "  老照片对比        → old-image-key / new-image-key\n"
        "  侨批/印章         → image-key\n"
        "  例：cover-image: \"images/xxx.jpg\""
    ),
    "audio": (
        "【音频类 key 用法】\n"
        "  故事/方言讲解     → audio-key\n"
        "  例：audio-key: \"audio/xxx.mp3\""
    ),
    "video": (
        "【视频类 key 用法】\n"
        "  建筑4K视频        → video-key\n"
        "  展播视频          → video-key\n"
        "  例：video-key: \"videos/xxx.mp4\""
    ),
    "models": (
        "【3D模型类 key 用法】\n"
        "  建筑/地点 3D模型  → model-key\n"
        "  例：model-key: \"models/xxx.splat\""
    ),
}


def print_yaml_hint(results):
    if not results:
        return
    prefixes = set(k.split("/")[0] for _, k in results)
    print("=" * 60)
    print("📝 以下 key 请复制到 application.yml 对应字段：\n")
    for _, key in results:
        p = key.split("/")[0]
        if p in ("videos", "video"):
            field = "video-key"
        elif p in ("models", "model"):
            field = "model-key"
        elif p in ("audio", "audios"):
            field = "audio-key"
        else:
            field = "image-key / cover-image / coverImage"
        print(f"  • {field}: \"{key}\"")

    print()
    for p in prefixes:
        hint_key = p if p in YAML_FIELD_HINTS else "images"
        if hint_key in YAML_FIELD_HINTS:
            print(YAML_FIELD_HINTS[hint_key])
            print()


# ============================================================
# CLI 入口
# ============================================================
HELP_TEXT = """
薪火侨乡 COS 素材上传脚本 v1.0

命令格式：
  python tools/upload_to_cos.py <命令> <本地路径> <COS前缀目录>

命令：
  upload  <文件>   <prefix>      上传单个文件
  batch   <文件夹> <prefix>      批量上传文件夹内所有素材
  preview <路径>   <prefix>      只预览 key，不实际上传

COS 前缀约定（对应 application.yml 里各种 *-key 字段）：
  images/   → 所有图片（建筑封面、风景照、档案照片、印章等）
  audio/    → 所有音频（故事讲解、方言录音）
  videos/   → 所有视频（4K建筑视频、宣传片）
  models/   → 所有3D模型（.splat / .ply / .glb）

示例：
  # 上传建筑封面
  python tools/upload_to_cos.py upload D:\\素材\\瑞石楼封面.jpg images

  # 批量上传建筑模型文件夹
  python tools/upload_to_cos.py batch D:\\素材\\3d模型 models

  # 批量上传全部方言音频
  python tools/upload_to_cos.py batch D:\\素材\\方言音频 audio

上传完成后会自动输出 YAML 写法，复制粘贴到 application.yml 即可。
"""


def main():
    if len(sys.argv) < 2:
        print(HELP_TEXT)
        return

    cmd = sys.argv[1].lower()
    if cmd in ("-h", "--help", "help"):
        print(HELP_TEXT)
        return

    if cmd not in ("upload", "batch", "preview"):
        print(f"❌ 未知命令：{cmd}")
        print(HELP_TEXT)
        return

    if len(sys.argv) < 4:
        print("❌ 参数不足，格式：")
        print("   python tools/upload_to_cos.py upload <文件路径> images")
        print("   python tools/upload_to_cos.py batch  <文件夹>   models")
        return

    local_path = sys.argv[2]
    prefix = sys.argv[3].strip("/")
    dry_run = (cmd == "preview")

    secret_id, secret_key, region, bucket, yml_path = load_cos_config()
    print(f"📋 COS 配置：")
    print(f"   区域：{region}")
    print(f"   存储桶：{bucket}")
    print(f"   配置来源：{yml_path}")
    print()

    client = None if dry_run else gen_cos_client(secret_id, secret_key, region)

    results = []
    if cmd in ("upload", "preview"):
        k = upload_one(client, bucket, local_path, prefix, dry_run=dry_run)
        if k:
            results.append((local_path, k))
    else:  # batch
        results = batch_upload(client, bucket, local_path, prefix, dry_run=dry_run)

    if results:
        print_yaml_hint(results)

    print(f"\n🎉 全部完成！共处理 {len(results)} 个文件。")
    if not dry_run:
        print(f"   记得把 application.yml 中 mock-mode 改成 false 来启用真实 COS 资源。")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n⚠️  用户中断")
