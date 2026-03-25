import { useEffect, useRef } from 'react';
import Hls from 'hls.js';

/** 라이브 카드 미리보기: HLS 재생 화면을 캔버스에 그려 썸네일로 표시. 30초마다 한 컷 갱신 */
const THUMB_DRAW_INTERVAL_MS = 30_000;

export interface LiveThumbProps {
  playbackUrl: string;
  /** 메인 페이지용 클래스명(기본 card-thumb-preview) */
  className?: string;
}

export default function LiveThumb({ playbackUrl, className = 'card-thumb-preview' }: LiveThumbProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const innerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const drawIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    const inner = innerRef.current;
    if (!video || !canvas || !inner || !playbackUrl) return;

    function drawFrame() {
      if (video.readyState >= 2 && video.videoWidth > 0 && inner) {
        const w = inner.offsetWidth;
        const h = inner.offsetHeight;
        if (w && h) {
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext('2d');
          if (ctx) ctx.drawImage(video, 0, 0, w, h);
        }
      }
    }

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;
      hls.loadSource(playbackUrl);
      hls.attachMedia(video);
      hls.on(Hls.Events.MANIFEST_PARSED, () => setTimeout(drawFrame, 500));
      hls.on(Hls.Events.ERROR, () => {});
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = playbackUrl;
      video.addEventListener('loadeddata', () => setTimeout(drawFrame, 500));
    }
    video.addEventListener('loadeddata', () => setTimeout(drawFrame, 500));
    video.addEventListener('playing', drawFrame);
    drawIntervalRef.current = setInterval(drawFrame, THUMB_DRAW_INTERVAL_MS);

    return () => {
      if (drawIntervalRef.current) {
        clearInterval(drawIntervalRef.current);
        drawIntervalRef.current = null;
      }
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      video.removeAttribute('src');
      video.load();
    };
  }, [playbackUrl]);

  return (
    <div ref={innerRef} className={className}>
      <video ref={videoRef} muted playsInline autoPlay />
      <canvas ref={canvasRef} className="thumb-canvas" />
    </div>
  );
}
