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
    const videoEl = video;
    const canvasEl = canvas;
    const innerEl = inner;

    function drawFrame() {
      if (videoEl.readyState >= 2 && videoEl.videoWidth > 0) {
        const w = innerEl.offsetWidth;
        const h = innerEl.offsetHeight;
        if (w && h) {
          canvasEl.width = w;
          canvasEl.height = h;
          const ctx = canvasEl.getContext('2d');
          if (ctx) ctx.drawImage(videoEl, 0, 0, w, h);
        }
      }
    }

    const handleLoadedData = () => setTimeout(drawFrame, 500);

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;
      hls.loadSource(playbackUrl);
      hls.attachMedia(videoEl);
      hls.on(Hls.Events.MANIFEST_PARSED, handleLoadedData);
      hls.on(Hls.Events.ERROR, () => {});
    } else if (videoEl.canPlayType('application/vnd.apple.mpegurl')) {
      videoEl.src = playbackUrl;
    }
    videoEl.addEventListener('loadeddata', handleLoadedData);
    videoEl.addEventListener('playing', drawFrame);
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
      videoEl.removeEventListener('loadeddata', handleLoadedData);
      videoEl.removeEventListener('playing', drawFrame);
      videoEl.removeAttribute('src');
      videoEl.load();
    };
  }, [playbackUrl]);

  return (
    <div ref={innerRef} className={className}>
      <video ref={videoRef} muted playsInline autoPlay />
      <canvas ref={canvasRef} className="thumb-canvas" />
    </div>
  );
}
