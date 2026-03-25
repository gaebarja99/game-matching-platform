/**
 * alert() 대체: 브라우저 기본 창 제목(예: 127.0.0.1:8080 내용:) 없이 메시지만 팝업으로 표시
 * 라이트/다크 테마에 맞춰 팝업 색상 자동 적용
 */
(function () {
    var overlay = null;
    var content = null;
    var btn = null;

    function injectThemeStyles() {
        if (document.getElementById('gm-alert-styles')) return;
        var style = document.createElement('style');
        style.id = 'gm-alert-styles';
        style.textContent = [
            '#gm-alert-overlay { position:fixed;inset:0;background:rgba(0,0,0,0.5);display:none;align-items:center;justify-content:center;z-index:99999; }',
            '#gm-alert-overlay[style*="flex"] { display:flex !important; }',
            '#gm-alert-content { padding:24px;border-radius:12px;max-width:360px;width:90%;font-family:\'Noto Sans KR\',\'Malgun Gothic\',sans-serif;font-size:0.95rem;line-height:1.5; border:1px solid; }',
            '#gm-alert-content { background:#1e1e21; color:#efeff1; border-color:#2c2c2e; box-shadow:0 8px 32px rgba(0,0,0,0.4); }',
            'body.theme-light #gm-alert-content { background:#fff; color:#1a1a1d; border-color:#e5e5e7; box-shadow:0 8px 32px rgba(0,0,0,0.12); }',
            '#gm-alert-message { margin:0 0 20px 0;white-space:pre-wrap;word-break:break-word; }',
            '#gm-alert-ok { display:block;margin-left:auto;padding:10px 24px;background:#00e676;color:#0d0d0f;border:none;border-radius:8px;font-size:0.9rem;font-weight:600;cursor:pointer;font-family:inherit; }',
            '#gm-alert-ok:hover { background:#33eb91; }',
            'body.theme-light #gm-alert-ok { background:#00a858; color:#fff; }',
            'body.theme-light #gm-alert-ok:hover { background:#00e676; }'
        ].join('\n');
        document.head.appendChild(style);
    }

    function ensureModal() {
        if (overlay) return;
        injectThemeStyles();
        overlay = document.createElement('div');
        overlay.setAttribute('id', 'gm-alert-overlay');
        overlay.style.cssText = 'display:none;align-items:center;justify-content:center;';
        content = document.createElement('div');
        content.setAttribute('id', 'gm-alert-content');
        var msg = document.createElement('p');
        msg.setAttribute('id', 'gm-alert-message');
        msg.style.cssText = 'margin:0 0 20px 0;white-space:pre-wrap;word-break:break-word;';
        btn = document.createElement('button');
        btn.setAttribute('id', 'gm-alert-ok');
        btn.textContent = '확인';
        btn.addEventListener('click', function () {
            overlay.style.display = 'none';
        });
        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) overlay.style.display = 'none';
        });
        content.appendChild(msg);
        content.appendChild(btn);
        overlay.appendChild(content);
        document.body.appendChild(overlay);
    }

    window.alert = function (message) {
        ensureModal();
        var msgEl = document.getElementById('gm-alert-message');
        if (msgEl) msgEl.textContent = message == null ? '' : String(message);
        overlay.style.display = 'flex';
    };
})();
