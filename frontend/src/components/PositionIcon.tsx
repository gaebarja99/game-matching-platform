/**
 * 포지션/역할 아이콘 — 스펙 이미지(positions/, valorant-roles/ 등) 사용, 없으면 이모지
 * 게임 context 있으면 백엔드 static(/images/...) PNG 사용 (apiUrl 경유)
 */
import { useState } from 'react';
import { getPositionImagePath } from '../utils/positionImages';
import { apiUrl } from '../api/client';

const POSITION_ICONS: Record<string, string> = {
  FILL: '✱',
  TOP: '⬆️',
  JUNGLE: '🌲',
  MID: '⚔️',
  MIDDLE: '⚔️',
  ADC: '🎯',
  BOTTOM: '🎯',
  SUPPORT: '🛡️',
  SUP: '🛡️',
  CONTROLLER: '🛡️',
  DUELIST: '⚔️',
  SCOUT: '👣',
  STRATEGIST: '👁️',
  WATCHER: '🧠',
  INITIATOR: '💥',
  SENTINEL: '🛡️',
  ASSAULT: '⚔️',
  RECON: '👁️',
  SKIRMISHER: '⚔️',
  ASSAULT_LEGEND: '⚔️',
  SUPPORT_LEGEND: '🛡️',
  RECON_LEGEND: '👁️',
  DEFENSE: '🛡️',
  DEFENSE_LEGEND: '🛡️',
  TANK: '🛡️',
  DAMAGE: '⚔️',
  ENTRY: '💥',
  IGL: '📋',
  AWPER: '🎯',
  LURKER: '👁️',
};

const POSITION_LABELS: Record<string, string> = {
  FILL: '채우기',
  TOP: '탑',
  JUNGLE: '정글',
  MID: '미드',
  MIDDLE: '미드',
  ADC: '원딜',
  BOTTOM: '원딜',
  SUPPORT: '서포터',
  SUP: '서포터',
  CONTROLLER: '컨트롤러',
  DUELIST: '타격대',
  SCOUT: '척후병',
  STRATEGIST: '전략가',
  WATCHER: '감시자',
  INITIATOR: '이니시에이터',
  SENTINEL: '센티넬',
  ASSAULT: '어썰트',
  RECON: '리콘',
  SKIRMISHER: '스커미셔',
  DEFENSE: '디펜스',
  TANK: '탱커',
  DAMAGE: '딜러',
  ENTRY: '엔트리',
  IGL: 'IGL',
  AWPER: '에이퍼',
  LURKER: '루커',
};

function normalizeKey(pos: string | null | undefined): string | null {
  if (pos == null || pos === '') return null;
  const u = pos.trim().toUpperCase().replace(/\s+/g, '_');
  return u in POSITION_ICONS ? u : u in POSITION_LABELS ? u : null;
}

interface PositionIconProps {
  position: string | null | undefined;
  /** 게임 키(LEAGUE_OF_LEGENDS, VALORANT 등) 있으면 스펙 이미지 경로 사용 */
  game?: string | null;
  showLabel?: boolean;
  className?: string;
}

export default function PositionIcon({ position, game, showLabel = true, className = '' }: PositionIconProps) {
  const key = normalizeKey(position);
  const [imgFailed, setImgFailed] = useState(false);
  const imagePath = game && key && !imgFailed ? getPositionImagePath(game, position) : null;
  const imageUrl = imagePath ? apiUrl(imagePath) : null;

  if (!key) return <span className={`position-icon ${className}`}>{position || '—'}</span>;
  const icon = POSITION_ICONS[key] ?? '•';
  const label = POSITION_LABELS[key] ?? position;

  return (
    <span className={`position-icon ${className}`} title={label}>
      {imageUrl ? (
        <img
          src={imageUrl}
          alt=""
          className="position-icon-img"
          aria-hidden
          onError={() => setImgFailed(true)}
        />
      ) : (
        <span className="position-icon-emoji" aria-hidden>{icon}</span>
      )}
      {showLabel && <span className="position-icon-label">{label}</span>}
    </span>
  );
}
