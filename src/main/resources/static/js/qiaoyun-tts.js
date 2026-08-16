/**
 * 薪火侨乡 - 统一 TTS 语音讲解工具（v5 最终版）
 * 
 * 策略：先尝试最简模式（不指定 voice），与测试语音使用相同路径
 * 这是经过验证的可靠方式
 */
(function (window) {
    'use strict';

    var synth = window.speechSynthesis;
    if (!synth) {
        console.warn('[QiaoyunTTS] 浏览器不支持 speechSynthesis');
        window.QiaoyunTTS = null;
        return;
    }

    var state = {
        voices: [],
        bestVoice: null,
        isPlaying: false,
        isPaused: false,
        onEndCallback: null
    };

    function loadVoices() {
        state.voices = synth.getVoices() || [];
        state.bestVoice = pickBestVoice(state.voices);
        console.log('[QiaoyunTTS] 声线加载完成，共', state.voices.length, '个');
    }

    function pickBestVoice(voices) {
        if (!voices || voices.length === 0) return null;
        var matchers = [
            function (v) { return /google/i.test(v.name) && /zh/i.test(v.lang) && /female|女|wan|xiao/i.test(v.name); },
            function (v) { return /google/i.test(v.name) && /zh/i.test(v.lang); },
            function (v) { return /microsoft/i.test(v.name) && /zh/i.test(v.lang) && /huihui|xiaoxiao|tingting|yaoyao/i.test(v.name); },
            function (v) { return /microsoft/i.test(v.name) && /zh/i.test(v.lang); },
            function (v) { return /zh/i.test(v.lang) && /female|女|wan|xiao/i.test(v.name); },
            function (v) { return /zh/i.test(v.lang); }
        ];
        for (var i = 0; i < matchers.length; i++) {
            for (var j = 0; j < voices.length; j++) {
                if (matchers[i](voices[j])) return voices[j];
            }
        }
        return null;
    }

    loadVoices();
    if (synth.onvoiceschanged !== undefined) {
        synth.addEventListener('voiceschanged', loadVoices);
    } else {
        var pollCount = 0;
        var pollTimer = setInterval(function () {
            pollCount++;
            if (state.voices.length > 0 || pollCount > 15) {
                clearInterval(pollTimer);
            } else {
                loadVoices();
            }
        }, 150);
    }

    function speak(text, options) {
        if (!synth) return false;
        if (!text || !text.trim()) return false;

        options = options || {};
        state.onEndCallback = options.onEnd || null;
        state.isPlaying = true;
        state.isPaused = false;

        // 关键：先 cancel，然后用 setTimeout 让浏览器完成清理
        synth.cancel();

        var rate = options.rate != null ? options.rate : 0.95;
        var pitch = options.pitch != null ? options.pitch : 1.0;
        var volume = options.volume != null ? options.volume : 1.0;

        // 用 setTimeout 模拟测试语音的成功模式
        setTimeout(function () {
            if (!state.isPlaying) return;

            var u = new SpeechSynthesisUtterance(text);
            u.lang = 'zh-CN';
            u.rate = rate;
            u.pitch = pitch;
            u.volume = volume;

            // 不指定 voice，让浏览器自动选择
            // 这是测试语音验证过的可靠模式

            var started = false;

            u.onstart = function () {
                started = true;
                console.log('[QiaoyunTTS] ✅ onstart 触发，语音开始播放');
            };

            u.onend = function () {
                if (!state.isPlaying) return;

                if (started) {
                    console.log('[QiaoyunTTS] ✅ onend 触发，语音正常播放完成');
                    finishOk();
                } else {
                    // onend 在 onstart 之前触发了
                    console.warn('[QiaoyunTTS] ⚠️ onend 先于 onstart，无声音');
                    // 再试一次
                    if (state.isPlaying) {
                        console.log('[QiaoyunTTS] 🔄 重试一次...');
                        synth.cancel();
                        setTimeout(function () {
                            if (!state.isPlaying) return;
                            var u2 = new SpeechSynthesisUtterance(text);
                            u2.lang = 'zh-CN';
                            u2.rate = rate;
                            u2.pitch = pitch;
                            u2.volume = volume;

                            var started2 = false;
                            u2.onstart = function () {
                                started2 = true;
                                console.log('[QiaoyunTTS] ✅ 重试 onstart 触发');
                            };
                            u2.onend = function () {
                                if (started2) {
                                    console.log('[QiaoyunTTS] ✅ 重试 onend 完成');
                                    finishOk();
                                } else {
                                    console.error('[QiaoyunTTS] ❌ 重试仍然失败');
                                    finishError('语音合成失败：浏览器无法播放语音。建议使用 Chrome 或 Edge 浏览器，并确保系统音量开启。');
                                }
                            };
                            u2.onerror = function (e) {
                                if (e.error !== 'canceled' && e.error !== 'interrupted') {
                                    finishError('语音合成出错：' + e.error);
                                }
                            };
                            synth.speak(u2);
                        }, 200);
                    }
                }
            };

            u.onerror = function (e) {
                if (e.error === 'canceled' || e.error === 'interrupted') return;
                console.error('[QiaoyunTTS] ❌ onerror:', e.error);
                finishError('语音合成出错：' + e.error);
            };

            console.log('[QiaoyunTTS] 🎙️ 准备 speak，文本长度:', text.length);
            synth.speak(u);

            // 3秒后检查是否真的开始播放了
            setTimeout(function () {
                if (state.isPlaying && !started) {
                    console.warn('[QiaoyunTTS] ⏰ 3秒超时仍未 onstart，尝试用指定声线');
                    synth.cancel();
                    if (state.bestVoice && state.isPlaying) {
                        var u3 = new SpeechSynthesisUtterance(text);
                        u3.voice = state.bestVoice;
                        u3.lang = state.bestVoice.lang;
                        u3.rate = rate;
                        u3.pitch = pitch;
                        u3.volume = volume;

                        var started3 = false;
                        u3.onstart = function () {
                            started3 = true;
                            console.log('[QiaoyunTTS] ✅ 指定声线 onstart 触发');
                        };
                        u3.onend = function () {
                            if (started3) {
                                finishOk();
                            } else {
                                finishError('语音合成失败：所有模式均无法播放。建议使用 Chrome 或 Edge 浏览器。');
                            }
                        };
                        u3.onerror = function (e) {
                            if (e.error !== 'canceled' && e.error !== 'interrupted') {
                                finishError('语音合成出错：' + e.error);
                            }
                        };
                        synth.speak(u3);
                    } else {
                        finishError('语音合成超时：浏览器无法播放语音');
                    }
                }
            }, 3000);

        }, 150);

        return true;
    }

    function finishOk() {
        state.isPlaying = false;
        state.isPaused = false;
        var cb = state.onEndCallback;
        state.onEndCallback = null;
        if (typeof cb === 'function') cb(null);
    }

    function finishError(msg) {
        state.isPlaying = false;
        state.isPaused = false;
        var cb = state.onEndCallback;
        state.onEndCallback = null;
        if (typeof cb === 'function') cb(msg);
    }

    function pause() {
        if (!synth || !state.isPlaying || state.isPaused) return;
        state.isPaused = true;
        synth.pause();
    }

    function resume() {
        if (!synth || !state.isPlaying || !state.isPaused) return;
        state.isPaused = false;
        synth.resume();
    }

    function stop() {
        state.isPlaying = false;
        state.isPaused = false;
        state.onEndCallback = null;
        if (synth.speaking || synth.paused) {
            synth.cancel();
        }
    }

    function isPlaying() {
        return state.isPlaying && !state.isPaused;
    }

    function getVoiceInfo() {
        if (!state.bestVoice) return '默认声线（浏览器自动选择）';
        return state.bestVoice.name + ' (' + state.bestVoice.lang + ')';
    }

    function listChineseVoices() {
        return (state.voices || []).filter(function (v) {
            return /zh/i.test(v.lang);
        }).map(function (v) {
            return { name: v.name, lang: v.lang, default: v.default };
        });
    }

    function testConnection() {
        return new Promise(function (resolve) {
            if (!synth) {
                resolve({
                    success: false, message: '浏览器不支持 speechSynthesis',
                    voicesCount: 0, selectedVoice: 'N/A', chineseVoices: [],
                    hint: '请使用 Chrome 或 Edge 浏览器'
                });
                return;
            }

            var started = false;
            var testUtterance = new SpeechSynthesisUtterance('测试语音');
            var result = {
                voicesCount: state.voices.length,
                selectedVoice: getVoiceInfo(),
                chineseVoices: listChineseVoices()
            };

            testUtterance.lang = 'zh-CN';
            testUtterance.rate = 1.0;
            testUtterance.pitch = 1.0;
            testUtterance.volume = 1.0;

            testUtterance.onstart = function () {
                started = true;
                synth.cancel();
                resolve({ success: true, message: '语音合成正常工作', details: result });
            };

            testUtterance.onend = function () {
                if (!started) {
                    synth.cancel();
                    resolve({
                        success: false, message: '语音合成立即结束（无声音）',
                        details: result,
                        hint: '声线不兼容，建议使用 Chrome 或 Edge 浏览器'
                    });
                }
            };

            testUtterance.onerror = function (e) {
                resolve({ success: false, message: '测试出错: ' + (e.error || e), details: result });
            };

            var timeout = setTimeout(function () {
                if (!started) {
                    synth.cancel();
                    resolve({
                        success: false, message: '语音合成超时（3秒未开始）',
                        details: result,
                        hint: '浏览器可能限制了自动播放'
                    });
                }
            }, 3000);

            synth.cancel();
            setTimeout(function () {
                synth.speak(testUtterance);
            }, 150);
        });
    }

    window.QiaoyunTTS = {
        speak: speak,
        pause: pause,
        resume: resume,
        stop: stop,
        isPlaying: isPlaying,
        getVoiceInfo: getVoiceInfo,
        listChineseVoices: listChineseVoices,
        testConnection: testConnection,
        speakGuide: function (text, onEnd) {
            return speak(text, { rate: 0.92, pitch: 0.98, volume: 1.0, onEnd: onEnd });
        }
    };

    console.log('[QiaoyunTTS] v5 已加载，当前声线：', getVoiceInfo());

})(window);
