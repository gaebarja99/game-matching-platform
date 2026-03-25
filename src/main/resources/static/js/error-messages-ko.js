/**
 * API/시스템 오류 메시지를 한글로 변환하여 화면에 빨간글씨로 나오는 오류를 한글로 통일
 */
(function () {
    function toKoreanErrorMessage(msg) {
        if (msg == null || typeof msg !== 'string') return '';
        var s = msg.trim();
        if (!s) return '';
        if (/Numeric value .* out of range of int|out of range of int/i.test(s))
            return '입력한 값이 너무 큽니다. (정수 범위: -2,147,483,648 ~ 2,147,483,647)';
        if (/out of range/i.test(s)) return '입력값이 허용 범위를 벗어났습니다.';
        if (/JSON parse error|JsonParseException|parse error/i.test(s))
            return '입력 형식이 올바르지 않습니다.';
        if (/Unauthorized|401|로그인이 필요/i.test(s)) return '로그인이 필요합니다.';
        if (/Bad Request|400/i.test(s)) return '잘못된 요청입니다.';
        if (/Failed to fetch|NetworkError|network error/i.test(s))
            return '네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
        if (/Forbidden|403/i.test(s)) return '권한이 없습니다.';
        if (/Not Found|404/i.test(s)) return '요청한 항목을 찾을 수 없습니다.';
        if (/Internal Server Error|500/i.test(s)) return '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
        return msg;
    }
    window.toKoreanErrorMessage = toKoreanErrorMessage;
})();
