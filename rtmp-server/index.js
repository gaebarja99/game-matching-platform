/**
 * Node-Media-Server: OBS RTMP 수신 및 HLS 변환
 *
 * 실행: npm install && npm start
 * OBS 서버: rtmp://localhost/live
 * HLS 재생: http://localhost:8000/live/스트림키/index.m3u8
 */
const NodeMediaServer = require('node-media-server');
const http = require('http');
const path = require('path');
const fs = require('fs');
const { execFileSync } = require('child_process');

const RTMP_PORT = 1935;
const HTTP_PORT = 8000;
const SPRING_BOOT_URL = 'http://127.0.0.1:8080';
const END_NOTIFY_DELAY_MS = 1200;

const pendingEndTimers = new Map();

function getStreamKey(streamPath) {
  if (!streamPath || typeof streamPath !== 'string') return null;
  const parts = streamPath.replace(/^\/+|\/+$/g, '').split('/');
  return parts.length >= 2 ? parts[parts.length - 1] : parts[0] || null;
}

function notifySpringBoot(targetPath, streamKey) {
  const data = new URLSearchParams({ name: streamKey }).toString();
  const url = new URL(targetPath, SPRING_BOOT_URL);
  const opts = {
    hostname: url.hostname,
    port: url.port || 80,
    path: url.pathname,
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': Buffer.byteLength(data),
    },
  };

  const req = http.request(opts, (res) => {
    if (res.statusCode && (res.statusCode < 200 || res.statusCode >= 300)) {
      console.error('[RTMP] Spring notify failed:', targetPath, 'HTTP', res.statusCode);
    }
  });

  req.on('error', (err) => {
    console.error('[RTMP] Spring connection failed:', targetPath, err.message);
  });

  req.write(data);
  req.end();
}

function cancelScheduledEnd(streamKey) {
  const timer = pendingEndTimers.get(streamKey);
  if (timer) {
    clearTimeout(timer);
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
    }, END_NOTIFY_DELAY_MS),
  );
}

const ffmpegCandidates = process.platform === 'win32'
  ? [
      process.env.FFMPEG_PATH,
      path.join(__dirname, '..', 'ffmpeg-2026-03-05-git-74cfcd1c69-full_build', 'bin', 'ffmpeg.exe'),
      'D:\\T4 GameMatcher\\GameMatcher1_base\\ffmpeg-2026-03-05-git-74cfcd1c69-full_build\\bin\\ffmpeg.exe',
    ].filter(Boolean)
  : [process.env.FFMPEG_PATH, 'ffmpeg'].filter(Boolean);

function canExecuteFfmpeg(targetPath) {
  try {
    if (process.platform === 'win32' && targetPath !== 'ffmpeg') {
      return fs.existsSync(targetPath);
    }
    execFileSync(targetPath, ['-version'], { stdio: 'pipe', timeout: 3000 });
    return true;
  } catch (error) {
    return false;
  }
}

const ffmpegPath = ffmpegCandidates.find(canExecuteFfmpeg) || ffmpegCandidates[0] || 'ffmpeg';
const ffmpegAvailable = canExecuteFfmpeg(ffmpegPath);

const config = {
  rtmp: {
    port: RTMP_PORT,
    chunk_size: 60000,
    gop_cache: true,
    ping: 30,
    ping_timeout: 60,
  },
  http: {
    port: HTTP_PORT,
    mediaroot: './media',
    allow_origin: '*',
  },
};

if (ffmpegAvailable) {
  config.trans = {
    ffmpeg: ffmpegPath,
    tasks: [
      {
        app: 'live',
        hls: true,
        hlsFlags: '[hls_time=2:hls_list_size=3:hls_flags=delete_segments]',
        hlsKeep: true,
      },
    ],
  };
  console.log('[OK] FFmpeg available:', ffmpegPath);
} else {
  console.error('');
  console.error('*** FFmpeg was not found, so HLS conversion is disabled. ***');
  console.error('   - Install FFmpeg: https://ffmpeg.org/download.html');
  console.error('   - Add the FFmpeg bin directory to PATH, or set FFMPEG_PATH');
  console.error('   - Example: set FFMPEG_PATH=C:\\ffmpeg\\bin\\ffmpeg.exe');
  console.error('');
}

const nms = new NodeMediaServer(config);

nms.on('postPublish', (id, streamPath, args) => {
  const streamKey = getStreamKey(streamPath);
  if (ffmpegAvailable && streamKey) {
    console.log('[RTMP] Stream started:', streamPath, '-> HLS:', `http://localhost:${HTTP_PORT}/live/${streamKey}/index.m3u8`);
  } else {
    console.log('[RTMP] Stream started:', streamPath);
  }

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

console.log(`RTMP server: rtmp://localhost:${RTMP_PORT}/live`);
if (ffmpegAvailable) {
  console.log(`HLS server: http://localhost:${HTTP_PORT}/live/스트림키/index.m3u8`);
} else {
  console.log('HLS server: disabled until FFmpeg is installed');
}
console.log('Spring Boot(8080) should be running first for stream-key validation.');
