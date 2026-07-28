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
    if (!window.modelUrl || modelUrl === '') {
        console.warn('[3D] modelUrl 为空，显示示例场景');
        showDemoScene();
        return;
    }

    console.log('[3D] 开始加载模型:', modelUrl);

    try {
        const loader = new THREE.GSplatLoader();

        loader.load(
            modelUrl,
            function (splat) {
                console.log('[3D] 模型加载成功');
                splatMesh = splat;
                scene.add(splatMesh);

                const box = new THREE.Box3().setFromObject(splatMesh);
                const center = box.getCenter(new THREE.Vector3());
                const size = box.getSize(new THREE.Vector3());

                controls.target.copy(center);

                const maxDim = Math.max(size.x, size.y, size.z);
                camera.position.copy(center);
                camera.position.z += maxDim * 2;

                animate();
            },
            function (xhr) {
                const percent = Math.round((xhr.loaded / xhr.total) * 100);
                console.log(`[3D] 模型加载进度: ${percent}%`);
            },
            function (err) {
                console.error('[3D] 模型加载失败:', err);
                showDemoScene();
            }
        );
    } catch (error) {
        console.error('[3D] 加载模型异常:', error);
        showDemoScene();
    }
}

function showDemoScene() {
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