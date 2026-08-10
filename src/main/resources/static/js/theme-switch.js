/**
 * 主题切换脚本 - 薪火侨乡
 * 支持深色/浅色主题切换，自动保存用户偏好
 */

// 切换主题
function toggleTheme() {
    const body = document.body;
    const currentTheme = body.getAttribute('data-theme');
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    
    if (newTheme === 'light') {
        body.setAttribute('data-theme', 'light');
        localStorage.setItem('qiaoyun-theme', 'light');
    } else {
        body.removeAttribute('data-theme');
        localStorage.setItem('qiaoyun-theme', 'dark');
    }
    updateThemeToggleButton();
}

// 更新主题切换按钮的图标和提示
function updateThemeToggleButton() {
    const toggleBtn = document.getElementById('theme-toggle');
    if (!toggleBtn) return;
    
    const isLight = document.body.getAttribute('data-theme') === 'light';
    if (isLight) {
        toggleBtn.textContent = '☀️';
        toggleBtn.title = '切换为深色主题';
    } else {
        toggleBtn.textContent = '🌙';
        toggleBtn.title = '切换为浅色主题';
    }
}

// 初始化主题（页面加载时调用）
function initTheme() {
    const savedTheme = localStorage.getItem('qiaoyun-theme');
    
    if (savedTheme === 'light') {
        document.body.setAttribute('data-theme', 'light');
    }
    updateThemeToggleButton();
}

// 自动初始化
document.addEventListener('DOMContentLoaded', function() {
    initTheme();
});
