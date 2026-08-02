/**
 * 建筑解剖 3D 分解展示
 * 点击右侧部位卡片 → 左侧 canvas 加载对应部位的 3D 模型
 * 模型未上传时，回退为部位图片或示例立方体
 */
let anatomyScene, anatomyCamera, anatomyRenderer, anatomyControls;
let anatomyCurrentMesh = null;     // 当前场景中的主网格（模型/图片/立方体）
let anatomyAnimated = false;       // animate 是否已启动
let anatomyLoadingTimer = null;    // 加载超时定时器

function initAnatomy3D() {
    const canvas = document.getElementById('anatomy-3d-canvas');
    if (!canvas) {
        console.warn('[Anatomy3D] 未找到 canvas 元素');
        return;
    }

    anatomyScene = new THREE.Scene();
    anatomyScene.background = new THREE.Color(0x0a0a0a);

    const width = canvas.parentElement.clientWidth;
    const height = canvas.parentElement.clientHeight;

    anatomyCamera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000);
    anatomyCamera.position.set(0, 1, 5);

    anatomyRenderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true });
    anatomyRenderer.setSize(width, height);
    anatomyRenderer.setPixelRatio(window.devicePixelRatio);

    anatomyControls = new THREE.OrbitControls(anatomyCamera, anatomyRenderer.domElement);
    anatomyControls.enableDamping = true;
    anatomyControls.dampingFactor = 0.05;
    anatomyControls.maxPolarAngle = Math.PI / 1.8;
    anatomyControls.minDistance = 1;
    anatomyControls.maxDistance = 15;

    // 灯光
    const ambient = new THREE.AmbientLight(0xffffff, 0.6);
    anatomyScene.add(ambient);
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.8);
    dirLight.position.set(5, 10, 5);
    anatomyScene.add(dirLight);
    const pointLight = new THREE.PointLight(0xd4af37, 0.5);
    pointLight.position.set(-5, 5, -5);
    anatomyScene.add(pointLight);

    window.addEventListener('resize', onAnatomyResize);

    // 初始展示示例场景
    showAnatomyDemoScene('点击右侧部位卡片，查看该部位的 3D 模型');
    hideAnatomyLoading();
}

/**
 * 切换部位：优先 3D 模型 → 其次部位图片 → 最后 Slot 占位符（非示例立方体）
 * @param modelUrl  3D 模型 URL（可能为空/null/undefined）
 * @param imageUrl  部位图片 URL（fallback）
 * @param partName  部位名称（用于提示）
 * @param partIndex 部位在列表中的序号（从1开始，用于生成 Slot ID）
 */
window.loadAnatomyPart = function (modelUrl, imageUrl, partName, partIndex) {
    if (!anatomyScene) {
        console.warn('[Anatomy3D] 场景未初始化');
        return;
    }
    // 清除当前网格 + 恢复 canvas 可见性
    clearAnatomyMesh();
    var canvas = document.getElementById('anatomy-3d-canvas');
    if (canvas) canvas.style.visibility = '';
    var ph = document.getElementById('anatomy-slot-placeholder');
    if (ph) ph.style.display = 'none';
    showAnatomyLoading();

    var name = partName || '该部位';
    var idx = partIndex || 1;

    // 1. 优先加载 3D 模型
    if (modelUrl && modelUrl !== 'null' && String(modelUrl).trim() !== '') {
        loadAnatomyModel(modelUrl, name, idx);
        return;
    }
    // 2. fallback：部位图片
    if (imageUrl && imageUrl !== 'null' && String(imageUrl).trim() !== '') {
        loadAnatomyImage(imageUrl, name);
        return;
    }
    // 3. 都没有：显示 Slot 占位符（MDL-05 / IMG-05）
    showAnatomySlotPlaceholder(name + ' 的 3D 模型与部位图片均未上传', idx, name);
    hideAnatomyLoading();
};

async function loadAnatomyModel(url, partName, partIndex) {
    console.log('[Anatomy3D] 开始加载部位模型:', url);

    // 超时保护：30 秒未完成则回退
    if (anatomyLoadingTimer) clearTimeout(anatomyLoadingTimer);
    anatomyLoadingTimer = setTimeout(function () {
        console.warn('[Anatomy3D] 模型加载超时，回退到图片/占位符');
        showAnatomySlotPlaceholder(partName + ' 的模型加载超时，显示占位符', partIndex, partName);
        hideAnatomyLoading();
    }, 30000);

    // 先用 HEAD 探测文件是否存在
    try {
        var headResp = await fetch(url, { method: 'HEAD' });
        if (!headResp.ok) {
            console.warn('[Anatomy3D] 模型文件不存在或不可访问:', headResp.status, url);
            showAnatomySlotPlaceholder(partName + ' 的 3D 模型暂未上传，显示占位符', partIndex, partName);
            hideAnatomyLoading();
            if (anatomyLoadingTimer) { clearTimeout(anatomyLoadingTimer); anatomyLoadingTimer = null; }
            return;
        }
    } catch (e) {
        console.warn('[Anatomy3D] 探测模型文件失败:', e);
        showAnatomySlotPlaceholder(partName + ' 的 3D 模型暂未上传，显示占位符', partIndex, partName);
        hideAnatomyLoading();
        if (anatomyLoadingTimer) { clearTimeout(anatomyLoadingTimer); anatomyLoadingTimer = null; }
        return;
    }

    // 检查 GSplatLoader 是否存在
    var LoaderCtor = window.GSplatLoader || (THREE && THREE.GSplatLoader);
    if (!LoaderCtor) {
        console.warn('[Anatomy3D] 未找到 GSplatLoader，无法解析 .splat 文件');
        showAnatomySlotPlaceholder(partName + ' 的模型加载器未就绪，显示占位符', partIndex, partName);
        hideAnatomyLoading();
        if (anatomyLoadingTimer) { clearTimeout(anatomyLoadingTimer); anatomyLoadingTimer = null; }
        return;
    }

    try {
        var loader = new LoaderCtor();
        loader.load(
            url,
            function (splat) {
                console.log('[Anatomy3D] 部位模型加载成功');
                if (anatomyLoadingTimer) { clearTimeout(anatomyLoadingTimer); anatomyLoadingTimer = null; }
                clearAnatomyMesh();
                anatomyCurrentMesh = splat;
                anatomyScene.add(anatomyCurrentMesh);

                // 自适应相机
                var box = new THREE.Box3().setFromObject(anatomyCurrentMesh);
                var center = box.getCenter(new THREE.Vector3());
                var size = box.getSize(new THREE.Vector3());
                anatomyControls.target.copy(center);
                var maxDim = Math.max(size.x, size.y, size.z);
                anatomyCamera.position.copy(center);
                anatomyCamera.position.z += maxDim * 2;

                hideAnatomyLoading();
                showAnatomyTip('正在展示：' + partName + '（可拖拽旋转）');
                startAnimate();
            },
            function (xhr) {
                var total = xhr.total || 0;
                var percent = total > 0 ? Math.round((xhr.loaded / total) * 100) : 0;
                console.log('[Anatomy3D] 模型加载进度: ' + percent + '%');
            },
            function (err) {
                console.error('[Anatomy3D] 模型加载失败:', err);
                if (anatomyLoadingTimer) { clearTimeout(anatomyLoadingTimer); anatomyLoadingTimer = null; }
                showAnatomySlotPlaceholder(partName + ' 的模型加载失败，显示占位符', partIndex, partName);
                hideAnatomyLoading();
            }
        );
    } catch (error) {
        console.error('[Anatomy3D] 加载模型异常:', error);
        if (anatomyLoadingTimer) { clearTimeout(anatomyLoadingTimer); anatomyLoadingTimer = null; }
        showAnatomySlotPlaceholder(partName + ' 的模型加载异常，显示占位符', partIndex, partName);
        hideAnatomyLoading();
    }
}

/**
 * 加载部位图片作为平面纹理（fallback）
 */
function loadAnatomyImage(url, partName) {
    console.log('[Anatomy3D] 加载部位图片:', url);
    var loader = new THREE.TextureLoader();
    loader.load(
        url,
        function (texture) {
            clearAnatomyMesh();
            var img = texture.image;
            var w = img.width || 4;
            var h = img.height || 3;
            var aspect = w / h;
            var planeW = 4;
            var planeH = planeW / aspect;
            var geometry = new THREE.PlaneGeometry(planeW, planeH);
            var material = new THREE.MeshBasicMaterial({ map: texture, side: THREE.DoubleSide });
            anatomyCurrentMesh = new THREE.Mesh(geometry, material);
            anatomyScene.add(anatomyCurrentMesh);
            anatomyControls.target.set(0, 0, 0);
            anatomyCamera.position.set(0, 0, 5);
            hideAnatomyLoading();
            showAnatomyTip('正在展示：' + partName + ' 的图片（3D 模型未上传）');
            startAnimate();
        },
        undefined,
        function (err) {
            console.error('[Anatomy3D] 图片加载失败:', err);
            showAnatomyDemoScene(partName + ' 的图片加载失败，显示示例场景');
            hideAnatomyLoading();
        }
    );
}

/**
 * 无任何素材时：显示 DOM Slot 占位符（而非示例立方体）
 * 占位符上标明 MDL-05 和 IMG-05 的 Slot 编号，引导后台上传
 * @param msg          提示文案（部位名 + 状态）
 * @param partIndex    部位在当前建筑部位列表中的序号（从1开始，用于拼接 Slot ID）
 * @param partName     部位名称（用于 slot-desc 描述）
 */
function showAnatomySlotPlaceholder(msg, partIndex, partName) {
    clearAnatomyMesh();
    // 隐藏 canvas，显示 DOM 占位符
    var canvas = document.getElementById('anatomy-3d-canvas');
    if (canvas) canvas.style.visibility = 'hidden';
    var ph = document.getElementById('anatomy-slot-placeholder');
    if (ph) {
        var idx = partIndex || 1;
        var pname = partName || '该部位';
        // 优先提示 MDL-05（3D模型），同时提示 IMG-05（图片fallback）
        var mdlSlotId = 'MDL-05-' + String(idx).padStart(2, '0');
        var imgSlotId = 'IMG-05-' + String(idx).padStart(2, '0');
        var badgeEl = document.getElementById('anatomy-slot-badge');
        var descEl = document.getElementById('anatomy-slot-desc');
        var statusEl = document.getElementById('anatomy-slot-status');
        var iconEl = document.getElementById('anatomy-slot-icon');
        if (badgeEl) badgeEl.textContent = mdlSlotId + ' / ' + imgSlotId;
        if (descEl) descEl.textContent = pname + ' · 3D模型(' + mdlSlotId + ') / 部位图片(' + imgSlotId + ') 待上传';
        if (statusEl) statusEl.textContent = '当前显示占位提示，请前往管理后台匹配对应 Slot 的素材';
        if (iconEl) iconEl.textContent = '🧊';
        ph.style.display = 'flex';
    }
    showAnatomyTip(msg);
    // 停止渲染循环（占位符不需要渲染3D）
    anatomyAnimated = false;
}

/**
 * 示例立方体场景（仅用于未选择任何部位时的初始引导状态）
 */
function showAnatomyDemoScene(msg) {
    clearAnatomyMesh();
    // 确保 canvas 可见、占位符隐藏
    var canvas = document.getElementById('anatomy-3d-canvas');
    if (canvas) canvas.style.visibility = '';
    var ph = document.getElementById('anatomy-slot-placeholder');
    if (ph) ph.style.display = 'none';

    var geometry = new THREE.BoxGeometry(1.8, 1.8, 1.8);
    var material = new THREE.MeshPhongMaterial({
        color: 0xd4af37,
        transparent: true,
        opacity: 0.8,
        shininess: 100
    });
    anatomyCurrentMesh = new THREE.Mesh(geometry, material);
    anatomyScene.add(anatomyCurrentMesh);

    // 线框
    var edges = new THREE.EdgesGeometry(geometry);
    var lineMaterial = new THREE.LineBasicMaterial({ color: 0xd4af37 });
    var wireframe = new THREE.LineSegments(edges, lineMaterial);
    anatomyCurrentMesh.add(wireframe);

    anatomyControls.target.set(0, 0, 0);
    anatomyCamera.position.set(0, 1, 5);
    showAnatomyTip(msg);
    startAnimate();
}

function clearAnatomyMesh() {
    if (anatomyCurrentMesh) {
        anatomyScene.remove(anatomyCurrentMesh);
        // 释放资源
        anatomyCurrentMesh.traverse(function (obj) {
            if (obj.geometry) obj.geometry.dispose();
            if (obj.material) {
                if (Array.isArray(obj.material)) {
                    obj.material.forEach(function (m) {
                        if (m.map) m.map.dispose();
                        m.dispose();
                    });
                } else {
                    if (obj.material.map) obj.material.map.dispose();
                    obj.material.dispose();
                }
            }
        });
        anatomyCurrentMesh = null;
    }
}

function showAnatomyLoading() {
    var loading = document.getElementById('anatomy-loading');
    if (loading) loading.style.display = '';
    hideAnatomyTip();
}

function hideAnatomyLoading() {
    var loading = document.getElementById('anatomy-loading');
    if (loading) loading.style.display = 'none';
}

function showAnatomyTip(msg) {
    if (!msg) return;
    var wrapper = document.querySelector('.anatomy-canvas-wrapper');
    if (!wrapper) return;
    var tip = wrapper.querySelector('.anatomy-tip');
    if (!tip) {
        tip = document.createElement('div');
        tip.className = 'anatomy-tip';
        wrapper.appendChild(tip);
    }
    tip.textContent = msg;
    tip.style.display = '';
}

function hideAnatomyTip() {
    var tip = document.querySelector('.anatomy-canvas-wrapper .anatomy-tip');
    if (tip) tip.style.display = 'none';
}

function onAnatomyResize() {
    var canvas = document.getElementById('anatomy-3d-canvas');
    if (!canvas || !anatomyCamera || !anatomyRenderer) return;
    var width = canvas.parentElement.clientWidth;
    var height = canvas.parentElement.clientHeight;
    anatomyCamera.aspect = width / height;
    anatomyCamera.updateProjectionMatrix();
    anatomyRenderer.setSize(width, height);
}

function startAnimate() {
    if (anatomyAnimated) return;
    anatomyAnimated = true;
    animateAnatomy();
}

function animateAnatomy() {
    requestAnimationFrame(animateAnatomy);
    if (anatomyControls) anatomyControls.update();
    if (anatomyRenderer && anatomyScene && anatomyCamera) {
        anatomyRenderer.render(anatomyScene, anatomyCamera);
    }
}

document.addEventListener('DOMContentLoaded', function () {
    console.log('[Anatomy3D] DOM 加载完成，准备初始化渲染');
    if (typeof THREE !== 'undefined') {
        initAnatomy3D();
    } else {
        console.log('[Anatomy3D] Three.js 尚未加载，等待加载完成');
        var checkThree = setInterval(function () {
            if (typeof THREE !== 'undefined') {
                clearInterval(checkThree);
                initAnatomy3D();
            }
        }, 100);
    }
});
