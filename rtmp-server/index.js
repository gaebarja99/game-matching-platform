/**
 * Node-Media-Server: OBS RTMP 수신 → HLS 변환
 * 참고: https://lemonpie313.tistory.com/239
 *
 * 실행: npm install && npm start
 * OBS 서버: rtmp://localhost/live  스트림 키: (방송 등록 시 발급된 키)
 * HLS 재생: http://localhost:8000/live/스트림키/index.m3u8
 *
 * ⚠ 브라우저에서 재생이 안 되면 FFmpeg가 PATH에 있는지 확인하세요. (ffmpeg -version)
 */
const NodeMediaServer = require('node-media-server');
const http = require('http');
const path = require('path');
const { execSync } = require('child_process');

const RTMP_PORT = 1935;
const HTTP_PORT = 8000;
const SPRING_BOOT_URL = 'http://127.0.0.1:8080';
const DEFAULT_FFMPEG_PATH = process.platform === 'win32'
  ? 'D:/T4 GameMatcher/ffmpeg-2026-03-05-git-74cfcd1c69-full_build/bin/ffmpeg.exe'
  : 'ffmpeg';

function getStreamKey(streamPath) {
  if (!streamPath || typeof streamPath !== 'string') return null;
  const parts = streamPath.replace(/^\/+|\/+$/g, '').split('/');
  return parts.length >= 2 ? parts[parts.length - 1] : parts[0] || null;
}

function notifySpringBoot(path, streamKey) {
  const data = new URLSearchParams({ name: streamKey }).toString();
  const url = new URL(path, SPRING_BOOT_URL);
  const opts = {
    hostname: url.hostname,
    port: url.port || 80,
    path: url.pathname,
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Content-Length': Buffer.byteLength(data) }
  };
  const req = http.request(opts, (res) => {
    if (res.statusCode && (res.statusCode < 200 || res.statusCode >= 300)) {
      console.error('[RTMP] Spring 알림 실패:', path, 'HTTP', res.statusCode, '(8080에서 백엔드가 떠 있는지 확인)');
    }
  });
  req.on('error', (err) => {
    console.error('[RTMP] Spring 연결 실패:', path, err.message);
  });
  req.write(data);
  req.end();
}

/** OBS 재시작 시 donePublish가 postPublish보다 늦게 오면 DB가 다시 ENDED로 덮일 수 있음 → 종료 알림만 짧게 지연, 재송출 시 취소 */
const END_NOTIFY_DELAY_MS = 1200;
const pendingEndTimers = new Map();

function cancelScheduledEnd(streamKey) {
  const t = pendingEndTimers.get(streamKey);
  if (t) {
    clearTimeout(t);
    pendingEndTimers.delete(streamKey);
  }
}

function scheduleNotifyEnd(streamKey) {
  cancelScheduledEnd(streamKey);
  pendingEndTimers.set(
    streamKey,
    setTimeout(() => {
      pendingEndTimers.delete(streamKey);
      notifySpringBoot('/api/streams/notify/end', streamKey);
    }, END_NOTIFY_DELAY_MS)
  );
}

const config = {
  rtmp: {
    port: RTMP_PORT,
    chunk_size: 60000,
    gop_cache: true,
    ping: 30,
    ping_timeout: 60
  },
  http: {
    port: HTTP_PORT,
    mediaroot: './media',
    allow_origin: '*'
  },
  trans: {
    ffmpeg: process.env.FFMPEG_PATH || DEFAULT_FFMPEG_PATH,
    tasks: [
      {
        app: 'live',
        hls: true,
        hlsFlags: '[hls_time=2:hls_list_size=3:hls_flags=delete_segments]',
        hlsKeep: true
      }
    ]
  }
};

const ffmpegPath = process.env.FFMPEG_PATH || DEFAULT_FFMPEG_PATH;
try {
  execSync('"' + ffmpegPath + '" -version', { stdio: 'pipe', timeout: 3000 });
  console.log('[OK] FFmpeg 사용 가능:', ffmpegPath);
} catch (e) {
  console.error('');
  console.error('*** FFmpeg를 찾을 수 없습니다. HLS 변환이 되지 않아 브라우저에서 재생이 안 됩니다. ***');
  console.error('   - FFmpeg 설치: https://ffmpeg.org/download.html');
  console.error('   - 설치 후 bin 폴더를 PATH에 추가하거나, FFMPEG_PATH 환경 변수로 경로 지정');
  console.error('   예: set FFMPEG_PATH=C:\\ffmpeg\\bin\\ffmpeg.exe  (Windows)');
  console.error('');
}

const nms = new NodeMediaServer(config);

nms.on('postPublish', (id, streamPath, args) => {
  const streamKey = getStreamKey(streamPath);
  console.log('[RTMP] 방송 시작:', streamPath, '-> HLS: http://localhost:' + HTTP_PORT + '/live/' + streamKey + '/index.m3u8');
  if (streamKey) {
    cancelScheduledEnd(streamKey);
    notifySpringBoot('/api/streams/notify/live', streamKey);
  }
});

nms.on('donePublish', (id, streamPath, args) => {
  const streamKey = getStreamKey(streamPath);
  if (streamKey) scheduleNotifyEnd(streamKey);
});

nms.run();

console.log('RTMP 서버: rtmp://localhost:' + RTMP_PORT + '/live');
console.log('HLS 서버: http://localhost:' + HTTP_PORT + '/live/스트림키/index.m3u8');
console.log('Spring Boot(8080)가 먼저 실행 중이어야 스트림 키 검증이 됩니다.');
