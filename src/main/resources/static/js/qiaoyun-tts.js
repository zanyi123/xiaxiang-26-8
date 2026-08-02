/**
 * 薪火侨乡 - 统一 TTS 语音讲解工具
 *
 * 解决浏览器原生 speechSynthesis 的痛点：
 * 1. 声线僵硬 → 智能选择最佳中文女声（优先 Google/Microsoft 高质量声线）
 * 2. 首次播放无声线 → 等待 voicesloaded 事件
 * 3. 长文本被截断 → 分段播放（按句号/问号/感叹号切分）
 * 4. 各页面重复代码 → 统一 API：speak / pause / resume / stop
 *
 * 使用方式：
 *   QiaoyunTTS.speak('你好，欢迎来到侨乡');  // 播放
 *   QiaoyunTTS.pause();                       // 暂停
 *   QiaoyunTTS.resume();                      // 继续
 *   QiaoyunTTS.stop();                        // 停止
 *   QiaoyunTTS.isPlaying();                   // 是否播放中
 */
(function (window) {
    'use strict';

    var synth = window.speechSynthesis;
    if (!synth) {
        console.warn('[QiaoyunTTS] 浏览器不支持 speechSynthesis，TTS 功能不可用');
        return;
    }

    var state = {
        voices: [],
        bestVoice: null,
        queue: [],              // 待播放文本分段队列
        currentUtterance: null,  // 当前正在播放的 utterance
        isPlaying: false,
        isPaused: false,
        onEndCallback: null
    };

    // ====== 声线加载与选择 ======

    /**
     * 加载所有可用声线，并选出最佳中文女声
     */
    function loadVoices() {
        state.voices = synth.getVoices() || [];
        state.bestVoice = pickBestVoice(state.voices);
        console.log('[QiaoyunTTS] 声线加载完成，共', state.voices.length, '个，已选：',
            state.bestVoice ? (state.bestVoice.name + ' / ' + state.bestVoice.lang) : '默认');
    }

    /**
     * 智能选择最佳中文声线
     * 优先级（从高到低）：
     *   1. Google 普通话女声（Chrome 在线声线，最自然）
     *   2. Microsoft 普通话女声（Edge/Win 本地声线）
     *   3. Microsoft 粤语女声（若讲解为粤语场景）
     *   4. 任意 zh-CN 女声
     *   5. 任意 zh-CN 声线
     *   6. 任意 zh* 声线
     *   7. default
     */
    function pickBestVoice(voices) {
        if (!voices || voices.length === 0) return null;

        // 按优先级定义声线匹配规则
        var matchers = [
            // 1. Google 普通话女声（最自然）
            function (v) {
                return /google/i.test(v.name) && /zh(-|_)?cn/i.test(v.lang) && /female|女|wan|xiao/i.test(v.name);
            },
            // 2. Google 任意普通话
            function (v) {
                return /google/i.test(v.name) && /zh(-|_)?cn/i.test(v.lang);
            },
            // 3. Microsoft 普通话女声（Huihui/Xiaoxiao/Tingting）
            function (v) {
                return /microsoft/i.test(v.name) && /zh(-|_)?cn/i.test(v.lang) &&
                    /huihui|xiaoxiao|tingting|yaoyao|female|女/i.test(v.name);
            },
            // 4. Microsoft 任意普通话
            function (v) {
                return /microsoft/i.test(v.name) && /zh(-|_)?cn/i.test(v.lang);
            },
            // 5. 任意 zh-CN 女声
            function (v) {
                return /zh(-|_)?cn/i.test(v.lang) && /female|女|wan|xiao/i.test(v.name);
            },
            // 6. 任意 zh-CN 声线
            function (v) {
                return /zh(-|_)?cn/i.test(v.lang);
            },
            // 7. 任意 zh* 声线（含 zh-HK 粤语、zh-TW 等）
            function (v) {
                return /zh/i.test(v.lang);
            }
        ];

        for (var i = 0; i < matchers.length; i++) {
            for (var j = 0; j < voices.length; j++) {
                if (matchers[i](voices[j])) {
                    return voices[j];
                }
            }
        }
        return voices[0]; // 兜底：第一个
    }

    // 声线异步加载（Chrome 需要监听 voiceschanged 事件）
    loadVoices();
    if (typeof synth.onvoiceschanged !== 'undefined') {
        synth.addEventListener('voiceschanged', loadVoices);
    } else {
        // 部分浏览器不支持该事件，轮询兜底
        var pollCount = 0;
        var pollTimer = setInterval(function () {
            pollCount++;
            if (state.voices.length > 0 || pollCount > 10) {
                clearInterval(pollTimer);
            } else {
                loadVoices();
            }
        }, 200);
    }

    // ====== 文本分段 ======

    /**
     * 将长文本按句号/问号/感叹号/分号切分为短句
     * 避免浏览器 TTS 对长文本截断
     */
    function splitText(text) {
        if (!text) return [];
        // 按中文标点切分，保留标点
        var parts = text.split(/(?<=[。！？；\n])/);
        var result = [];
        var buffer = '';
        for (var i = 0; i < parts.length; i++) {
            var p = (parts[i] || '').trim();
            if (!p) continue;
            // 每段控制在 80 字以内（太长浏览器可能截断）
            if ((buffer + p).length > 80) {
                if (buffer) result.push(buffer);
                buffer = p;
            } else {
                buffer += p;
            }
        }
        if (buffer) result.push(buffer);
        return result;
    }

    // ====== 核心 API ======

    /**
     * 播放文本（会自动停止当前播放）
     * @param text      要播放的文本
     * @param options   可选：{ rate: 0.95, pitch: 1.0, volume: 1.0, onEnd: fn, lang: 'zh-CN' }
     */
    function speak(text, options) {
        if (!synth) return;
        if (!text || !text.trim()) return;

        options = options || {};
        stopInternal(); // 先停止当前

        var segments = splitText(text);
        if (segments.length === 0) return;

        state.queue = segments.slice();
        state.onEndCallback = options.onEnd || null;
        state.isPlaying = true;
        state.isPaused = false;

        playNext(options);
    }

    /**
     * 播放下一段
     */
    function playNext(options) {
        if (state.queue.length === 0) {
            // 全部播放完毕
            state.isPlaying = false;
            state.currentUtterance = null;
            if (typeof state.onEndCallback === 'function') {
                var cb = state.onEndCallback;
                state.onEndCallback = null;
                cb();
            }
            return;
        }

        // 暂停状态下不播放下一段
        if (state.isPaused) return;

        var segment = state.queue.shift();
        var utterance = new SpeechSynthesisUtterance(segment);

        // 声线与参数
        if (state.bestVoice) {
            utterance.voice = state.bestVoice;
        }
        utterance.lang = options.lang || (state.bestVoice ? state.bestVoice.lang : 'zh-CN');
        utterance.rate = options.rate != null ? options.rate : 0.95;   // 略慢，讲解感
        utterance.pitch = options.pitch != null ? options.pitch : 1.0; // 标准 pitch，避免过尖
        utterance.volume = options.volume != null ? options.volume : 1.0;

        utterance.onend = function () {
            state.currentUtterance = null;
            // 继续下一段（除非已停止/暂停）
            if (state.isPlaying && !state.isPaused) {
                // 小间隔，避免连读
                setTimeout(function () {
                    playNext(options);
                }, 120);
            }
        };

        utterance.onerror = function (e) {
            console.warn('[QiaoyunTTS] 播放出错:', e.error || e);
            state.currentUtterance = null;
            // 出错也继续下一段
            if (state.isPlaying && !state.isPaused) {
                setTimeout(function () {
                    playNext(options);
                }, 120);
            }
        };

        state.currentUtterance = utterance;
        synth.speak(utterance);
    }

    /**
     * 暂停播放
     */
    function pause() {
        if (!synth || !state.isPlaying) return;
        if (state.isPaused) return;
        state.isPaused = true;
        if (synth.speaking) {
            synth.pause();
        }
        console.log('[QiaoyunTTS] 已暂停');
    }

    /**
     * 继续播放
     */
    function resume() {
        if (!synth || !state.isPlaying || !state.isPaused) return;
        state.isPaused = false;
        if (synth.paused) {
            synth.resume();
        }
        console.log('[QiaoyunTTS] 已继续');
    }

    /**
     * 停止播放（清空队列）
     */
    function stop() {
        stopInternal();
        console.log('[QiaoyunTTS] 已停止');
    }

    function stopInternal() {
        state.queue = [];
        state.isPlaying = false;
        state.isPaused = false;
        state.currentUtterance = null;
        state.onEndCallback = null;
        if (synth.speaking || synth.paused) {
            synth.cancel();
        }
    }

    /**
     * 是否正在播放
     */
    function isPlaying() {
        return state.isPlaying && !state.isPaused;
    }

    /**
     * 获取当前选用的声线信息（调试用）
     */
    function getVoiceInfo() {
        if (!state.bestVoice) return '默认声线';
        return state.bestVoice.name + ' (' + state.bestVoice.lang + ')';
    }

    /**
     * 获取所有中文声线（调试用）
     */
    function listChineseVoices() {
        return (state.voices || []).filter(function (v) {
            return /zh/i.test(v.lang);
        }).map(function (v) {
            return { name: v.name, lang: v.lang, default: v.default };
        });
    }

    // ====== 导出 API ======
    window.QiaoyunTTS = {
        speak: speak,
        pause: pause,
        resume: resume,
        stop: stop,
        isPlaying: isPlaying,
        getVoiceInfo: getVoiceInfo,
        listChineseVoices: listChineseVoices,
        // 讲解预设：略慢、温和，适合文化讲解场景
        speakGuide: function (text, onEnd) {
            speak(text, { rate: 0.92, pitch: 0.98, volume: 1.0, onEnd: onEnd });
        }
    };

    console.log('[QiaoyunTTS] 工具已加载，当前声线：', getVoiceInfo());

})(window);
