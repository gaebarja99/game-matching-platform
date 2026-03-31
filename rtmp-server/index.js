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
const fs = require('fs');
const http = require('http');
const path = require('path');
const { execSync, execFileSync } = require('child_process');

const RTMP_PORT = 1935;
const HTTP_PORT = 8000;
const SPRING_BOOT_URL = process.env.SPRING_BOOT_URL || 'http://127.0.0.1:8080';
const DEFAULT_FFMPEG_PATH = process.platform === 'win32' ? 'ffmpeg.exe' : 'ffmpeg';
const BACKEND_PROPERTIES_PATH = path.resolve(__dirname, '../src/main/resources/application.properties');
const MEDIA_ROOT = path.resolve(__dirname, 'media').replace(/\\/g, '/');

function loadBackendStreamingProperties() {
  try {
    const raw = fs.readFileSync(BACKEND_PROPERTIES_PATH, 'utf8');
    return raw.split(/\r?\n/).reduce((acc, line) => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) return acc;
      const idx = trimmed.indexOf('=');
      if (idx < 0) return acc;
      const key = trimmed.slice(0, idx).trim();
      const value = trimmed.slice(idx + 1).trim();
      acc[key] = value;
      return acc;
    }, {});
  } catch (_error) {
    return {};
  }
}

function normalizeFsPath(value) {
  if (!value) return null;
  const trimmed = String(value).trim();
  if (!trimmed) return null;
  const normalized = trimmed.replace(/^['"]|['"]$/g, '');
  if (path.extname(normalized)) {
    return normalized;
  }
  if (fs.existsSync(normalized) && fs.statSync(normalized).isDirectory()) {
    return path.join(normalized, process.platform === 'win32' ? 'ffmpeg.exe' : 'ffmpeg');
  }
  return normalized;
}

function isOnPath(candidate) {
  if (process.platform === 'win32') {
    try {
      execSync(`where.exe "${candidate}"`, { stdio: 'pipe', timeout: 3000 });
      return true;
    } catch (_e) {
      return false;
    }
  }
  try {
    execFileSync('/bin/sh', ['-c', `command -v -- ${JSON.stringify(candidate)}`], {
      stdio: 'pipe',
      timeout: 3000,
    });
    return true;
  } catch (_e) {
    return false;
  }
}

function tryResolveFullExecutable(candidate) {
  if (path.isAbsolute(candidate) && fs.existsSync(candidate)) {
    try {
      return fs.realpathSync(candidate);
    } catch (_e) {
      return candidate;
    }
  }
  if (process.platform === 'win32') {
    try {
      const out = execSync(`where.exe ${JSON.stringify(candidate)}`, {
        encoding: 'utf8',
        stdio: 'pipe',
        timeout: 3000,
      });
      const first = out.trim().split(/\r?\n/)[0];
      if (first && fs.existsSync(first)) return first.trim();
    } catch (_e) {
      /* ignore */
    }
  } else {
    try {
      const out = execFileSync('/bin/sh', ['-c', `command -v -- ${JSON.stringify(candidate)}`], {
        encoding: 'utf8',
        stdio: 'pipe',
        timeout: 3000,
      });
      const p = out.trim();
      if (p && fs.existsSync(p)) return p;
    } catch (_e) {
      /* ignore */
    }
  }
  return candidate;
}

function resolveFfmpegPath() {
  const props = loadBackendStreamingProperties();
  const raw = [process.env.FFMPEG_PATH, props['app.streaming.ffmpeg-path'], DEFAULT_FFMPEG_PATH]
    .map(normalizeFsPath)
    .filter(Boolean);
  // 이 OS에 없는 절대 경로(예: Linux용 /usr/bin/ffmpeg 를 Windows에서 읽은 경우)는 제외
  const candidates = raw.filter((c) => !path.isAbsolute(c) || fs.existsSync(c));

  for (const candidate of candidates) {
    if (path.isAbsolute(candidate) && fs.existsSync(candidate)) {
      return tryResolveFullExecutable(candidate);
    }
    if (isOnPath(candidate)) {
      return tryResolveFullExecutable(candidate);
    }
  }

  // 잘못된 properties 값을 넘기지 않음 (기존: candidates[0] 때문에 없는 /usr/bin/ffmpeg 가 선택됨)
  return DEFAULT_FFMPEG_PATH;
}

const ffmpegPath = resolveFfmpegPath();

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
    mediaroot: MEDIA_ROOT,
    allow_origin: '*'
  },
  trans: {
    ffmpeg: ffmpegPath,
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

try {
  execFileSync(ffmpegPath, ['-version'], { stdio: 'pipe', timeout: 5000 });
  console.log('[OK] FFmpeg 사용 가능:', ffmpegPath);
} catch (e) {
  console.error('');
  console.error('*** FFmpeg를 찾을 수 없습니다. HLS 변환이 되지 않아 브라우저에서 재생이 안 됩니다. ***');
  console.error('   - Linux(EC2): sudo dnf install -y ffmpeg  (AL2023) / sudo apt install -y ffmpeg  (Ubuntu)');
  console.error('   - Windows: PATH에 bin 추가 또는 set FFMPEG_PATH=C:\\ffmpeg\\bin\\ffmpeg.exe');
  console.error('   - 기타: FFMPEG_PATH 환경 변수로 실행 파일 전체 경로 지정');
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
