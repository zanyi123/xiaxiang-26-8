/**
 * Slot 占位符渲染器
 *
 * 职责：扫描页面上所有带 data-slot-id 属性的元素，
 *       如果对应的素材URL为空/无效，就显示Slot编号占位符。
 *
 * 使用方式：
 *   1. 模板中：<img data-slot-id="IMG-02-01" th:src="${imageUrl}">
 *      - 如果 imageUrl 有值且COS文件存在 → 显示真实图片
 *      - 如果 imageUrl 为空 → 显示"IMG-02-01"占位符
 *
 *   2. 调用：SlotPlaceholder.init() 页面加载后自动运行
 */
(function (window, document) {
    'use strict';

    var TYPE_ICONS = {
        'IMG': '🖼️',
        'AUD': '🎵',
        'VID': '🎬',
        'MDL': '🧊'
    };

    /**
     * 为单个元素渲染占位符
     */
    function renderPlaceholder(el) {
        var slotId = el.getAttribute('data-slot-id');
        if (!slotId) return;

        var typePrefix = slotId.split('-')[0] || '???';
        var icon = TYPE_ICONS[typePrefix] || '📦';
        var desc = el.getAttribute('data-slot-desc') || '';
        var isSmall = el.hasAttribute('data-slot-small');

        // 创建占位符DOM
        var placeholder = document.createElement('div');
        placeholder.className = 'slot-placeholder' + (isSmall ? ' slot-placeholder-small' : '');
        placeholder.innerHTML =
            '<div class="slot-type-icon">' + icon + '</div>' +
            '<div class="slot-badge">' + slotId + '</div>' +
            (desc ? '<div class="slot-desc">' + escapeHtml(desc) + '</div>' : '') +
            '<div class="slot-status">待填充素材</div>';

        // 替换原元素
        var parent = el.parentNode;
        if (parent) {
            parent.replaceChild(placeholder, el);
        }
    }

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * 扫描页面所有带 data-slot-id 的元素
     * 根据素材是否存在决定显示真实内容还是占位符
     */
    function scanAndRender() {
        var elements = document.querySelectorAll('[data-slot-id]');
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            var slotId = el.getAttribute('data-slot-id');

            // 判断素材URL是否有效
            var src = el.getAttribute('src') || el.getAttribute('data-src') || '';
            var hasValidUrl = src && !src.startsWith('trae-api-cn') && !src.startsWith('/mock/') && src.length > 0;

            if (!hasValidUrl) {
                // URL无效 → 显示占位符
                renderPlaceholder(el);
            }
            // URL有效 → 保持原样显示真实图片
        }
    }

    /**
     * 页面加载后自动扫描
     */
    function init() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', scanAndRender);
        } else {
            scanAndRender();
        }
        // 500ms后再扫一次（确保Thymeleaf渲染完成后的动态内容也被处理）
        setTimeout(scanAndRender, 500);
    }

    // 暴露API
    window.SlotPlaceholder = {
        init: init,
        scan: scanAndRender,
        renderPlaceholder: renderPlaceholder
    };

    // 自动初始化
    init();

})(window, document);
