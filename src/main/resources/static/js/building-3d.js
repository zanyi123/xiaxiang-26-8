let scene, camera, renderer, controls, splatMesh;
let isAutoRotating = false;

function init3D() {
    const canvas = document.getElementById('3d-canvas');
    if (!canvas) {
        console.error('[3D] 未找到 canvas 元素');
        return;
    }

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x0a0a0a);

    const width = canvas.parentElement.clientWidth;
    const height = canvas.parentElement.clientHeight;

    camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000);
    camera.position.set(0, 2, 5);

    renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    renderer.setSize(width, height);
    renderer.setPixelRatio(window.devicePixelRatio);

    controls = new THREE.OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;
    controls.maxPolarAngle = Math.PI / 2.2;
    controls.minDistance = 1;
    controls.maxDistance = 20;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(5, 10, 5);
    scene.add(directionalLight);

    const pointLight = new THREE.PointLight(0xd4af37, 0.5);
    pointLight.position.set(-5, 5, -5);
    scene.add(pointLight);

    window.addEventListener('resize', onWindowResize);

    loadSplatModel();
}

async function loadSplatModel() {
    var url = (typeof window.modelUrl !== 'undefined') ? window.modelUrl : '';
    if (!url || url === '' || url === 'null') {
        console.warn('[3D] modelUrl 为空，显示Slot占位符');
        showSlotPlaceholder();
        return;
    }

    console.log('[3D] 开始加载模型:', url);

    // 先用 fetch 探测文件是否存在，避免 loader 直接报错
    try {
        var headResp = await fetch(url, { method: 'HEAD' });
        if (!headResp.ok) {
            console.warn('[3D] 模型文件不存在或不可访问:', headResp.status, url);
            showSlotPlaceholder();
            return;
        }
    } catch (e) {
        console.warn('[3D] 探测模型文件失败:', e);
        showSlotPlaceholder();
        return;
    }

    // 尝试使用可用的 gsplat 加载器
    var LoaderCtor = window.GSplatLoader || (THREE && THREE.GSplatLoader);
    if (!LoaderCtor) {
        console.warn('[3D] 未找到 GSplatLoader，无法解析 .splat 文件，显示Slot占位符');
        showSlotPlaceholder();
        return;
    }

    try {
        var loader = new LoaderCtor();
        loader.load(
            url,
            function (splat) {
                console.log('[3D] 模型加载成功');
                splatMesh = splat;
                scene.add(splatMesh);

                var box = new THREE.Box3().setFromObject(splatMesh);
                var center = box.getCenter(new THREE.Vector3());
                var size = box.getSize(new THREE.Vector3());

                controls.target.copy(center);

                var maxDim = Math.max(size.x, size.y, size.z);
                camera.position.copy(center);
                camera.position.z += maxDim * 2;

                hideLoading();
                animate();
            },
            function (xhr) {
                var total = xhr.total || 0;
                var percent = total > 0 ? Math.round((xhr.loaded / total) * 100) : 0;
                console.log('[3D] 模型加载进度: ' + percent + '%');
            },
            function (err) {
                console.error('[3D] 模型加载失败:', err);
                showSlotPlaceholder();
            }
        );
    } catch (error) {
        console.error('[3D] 加载模型异常:', error);
        showSlotPlaceholder();
    }
}

/**
 * 优先显示页面模板预置的 slot 占位符（带 MDL-xx-xx 编号徽章），
 * 这样用户明确知道该绑哪个Slot到COS桶，不显示误导性的"示例场景立方体"。
 * 若无占位符再降级显示示例场景。
 */
function showSlotPlaceholder() {
    hideLoading();
    var ph = document.getElementById('model-placeholder');
    if (ph) {
        ph.style.display = 'flex';
        // 隐藏画布避免黑色背景覆盖占位符
        var canvas = document.getElementById('3d-canvas');
        if (canvas) canvas.style.visibility = 'hidden';
    } else {
        showDemoScene('暂无3D模型数据');
    }
}

function hideLoading() {
    var loading = document.getElementById('loading');
    if (loading) loading.style.display = 'none';
}

function showDemoScene(msg) {
    hideLoading();
    const geometry = new THREE.BoxGeometry(2, 2, 2);
    const material = new THREE.MeshPhongMaterial({
        color: 0xd4af37,
        transparent: true,
        opacity: 0.8,
        shininess: 100
    });
    splatMesh = new THREE.Mesh(geometry, material);
    scene.add(splatMesh);

    const edges = new THREE.EdgesGeometry(geometry);
    const lineMaterial = new THREE.LineBasicMaterial({ color: 0xd4af37 });
    const wireframe = new THREE.LineSegments(edges, lineMaterial);
    splatMesh.add(wireframe);

    controls.target.set(0, 0, 0);
    animate();

    // 显示提示信息
    if (msg) {
        var canvasWrap = document.querySelector('.canvas-wrapper');
        if (canvasWrap) {
            var tip = document.createElement('div');
            tip.style.cssText = 'position:absolute;bottom:1rem;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.7);color:#d4af37;padding:0.6rem 1.2rem;border-radius:8px;font-size:0.85rem;z-index:10;pointer-events:none;';
            tip.textContent = msg;
            canvasWrap.appendChild(tip);
        }
    }
}

function onWindowResize() {
    const canvas = document.getElementById('3d-canvas');
    if (!canvas || !camera || !renderer) return;

    const width = canvas.parentElement.clientWidth;
    const height = canvas.parentElement.clientHeight;

    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
}

function animate() {
    requestAnimationFrame(animate);
    
    if (isAutoRotating && splatMesh) {
        splatMesh.rotation.y += 0.005;
    }
    
    controls.update();
    renderer.render(scene, camera);
}

window.resetCamera = function() {
    if (!camera || !controls) return;
    camera.position.set(0, 2, 5);
    controls.target.set(0, 0, 0);
    controls.update();
};

window.toggleAutoRotate = function() {
    isAutoRotating = !isAutoRotating;
    controls.autoRotate = isAutoRotating;
};

document.addEventListener('DOMContentLoaded', function () {
    console.log('[3D] DOM 加载完成，准备初始化渲染');
    
    if (typeof THREE !== 'undefined') {
        init3D();
    } else {
        console.log('[3D] Three.js 尚未加载，等待加载完成');
        const checkThree = setInterval(() => {
            if (typeof THREE !== 'undefined') {
                clearInterval(checkThree);
                init3D();
            }
        }, 100);
    }
});