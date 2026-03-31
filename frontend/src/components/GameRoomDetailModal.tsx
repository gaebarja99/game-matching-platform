import { useEffect, useCallback } from 'react';
import type { GameRoomItem } from '../api/gameRooms';
import { parseGameOptions } from '../utils/parseGameRoomOptions';
import { getRoomCapacityMeta } from '../utils/gameRoomCapacity';
import {
  GAME_OPTIONS,
  createFormShowTier,
  modeLabel,
  partySizeLabel,
  tierLabel,
  showPositionForRoom,
} from '../utils/randomMatchHelpers';
import { LOL_ROOM_QUEUE_OPTIONS } from '../utils/lolRoomCreateHelpers';
import PositionIcon from './PositionIcon';
import './GameRoomDetailModal.css';

export type MatchRoomPeek = {
  id: number;
  game: string;
  createdAt: string;
  memberCount: number | null;
  hostNickname: string;
};

export type RoomModalOpen =
  | { kind: 'game'; room: GameRoomItem }
  | { kind: 'match'; room: MatchRoomPeek }
  | null;

function lolQueueLabel(mode: string | undefined): string {
  if (!mode) return '-';
  const fromLol = LOL_ROOM_QUEUE_OPTIONS.find((o) => o.value === mode)?.label;
  if (fromLol) return fromLol;
  return mode;
}

function cs2ModeLabel(mode: string | undefined): string {
  if (!mode) return '-';
  switch (mode) {
    case 'PREMIER':
      return '프리미어';
    case 'COMPETITIVE':
      return '경쟁';
    case 'WINGMAN':
      return '윙맨';
    default:
      return mode;
  }
}

function formatModalDate(s: string) {
  try {
    return new Date(s).toLocaleString('ko-KR', {
      month: '2-digit',
      day: '2-digit',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return s;
  }
}

type Row = { label: string; value: React.ReactNode };

function buildGameRows(room: GameRoomItem): Row[] {
  const op = parseGameOptions(room.gameOptions);
  const gameLabel = GAME_OPTIONS.find((o) => o.key === room.game)?.label ?? room.game;
  const rows: Row[] = [{ label: '게임', value: gameLabel }, { label: '제목', value: room.title || '-' }];

  if (room.memo?.trim()) {
    rows.push({ label: '메모', value: room.memo.trim() });
  }

  switch (room.game) {
    case 'LEAGUE_OF_LEGENDS': {
      rows.push({ label: '모드', value: lolQueueLabel(op.mode) });
      if (createFormShowTier('LEAGUE_OF_LEGENDS', op.mode ?? '') && op.tier) {
        rows.push({ label: '티어', value: tierLabel(op.tier, 'LEAGUE_OF_LEGENDS') });
      }
      if (op.mode === 'QUICK') {
        if (op.primaryRole) {
          rows.push({
            label: '주 포지션',
            value: (
              <span className="game-room-detail-modal__pos">
                <PositionIcon position={op.primaryRole} game="LEAGUE_OF_LEGENDS" showLabel />
              </span>
            ),
          });
        }
        if (op.secondaryRole) {
          rows.push({
            label: '부 포지션',
            value: (
              <span className="game-room-detail-modal__pos">
                <PositionIcon position={op.secondaryRole} game="LEAGUE_OF_LEGENDS" showLabel />
              </span>
            ),
          });
        }
        if (op.findPosition) {
          rows.push({
            label: '찾는 포지션',
            value: (
              <span className="game-room-detail-modal__pos">
                <PositionIcon position={op.findPosition} game="LEAGUE_OF_LEGENDS" showLabel />
              </span>
            ),
          });
        }
      } else if (op.mode !== 'ARAM') {
        if (op.myPosition) {
          rows.push({
            label: '나의 포지션',
            value: (
              <span className="game-room-detail-modal__pos">
                <PositionIcon position={op.myPosition} game="LEAGUE_OF_LEGENDS" showLabel />
              </span>
            ),
          });
        }
        if (op.partnerPosition) {
          rows.push({
            label: '찾는 포지션',
            value: (
              <span className="game-room-detail-modal__pos">
                <PositionIcon position={op.partnerPosition} game="LEAGUE_OF_LEGENDS" showLabel />
              </span>
            ),
          });
        }
      }
      break;
    }
    case 'VALORANT': {
      rows.push({ label: '모드', value: modeLabel('VALORANT', op.mode ?? '') });
      if (op.tier) {
        rows.push({ label: '티어', value: tierLabel(op.tier, 'VALORANT') });
      }
      if (op.position && showPositionForRoom('VALORANT', op.mode)) {
        rows.push({
          label: '포지션',
          value: (
            <span className="game-room-detail-modal__pos">
              <PositionIcon position={op.position} game="VALORANT" showLabel />
            </span>
          ),
        });
      }
      break;
    }
    case 'OVERWATCH': {
      rows.push({ label: '모드', value: modeLabel('OVERWATCH', op.mode ?? '') });
      if (op.tier) {
        rows.push({ label: '티어', value: tierLabel(op.tier, 'OVERWATCH') });
      }
      if (op.position && showPositionForRoom('OVERWATCH', op.mode)) {
        rows.push({
          label: '포지션',
          value: (
            <span className="game-room-detail-modal__pos">
              <PositionIcon position={op.position} game="OVERWATCH" showLabel />
            </span>
          ),
        });
      }
      break;
    }
    case 'PUBG': {
      if (op.platform) {
        rows.push({
          label: '플랫폼',
          value: op.platform === 'STEAM' ? '스팀' : op.platform === 'KAKAO' ? '카카오' : op.platform,
        });
      }
      if (op.partySize) {
        rows.push({ label: '파티', value: partySizeLabel(op.partySize, 'PUBG') });
      }
      rows.push({ label: '모드', value: modeLabel('PUBG', op.mode ?? '') });
      if (op.preferredMap?.trim()) {
        rows.push({ label: '선호 맵', value: op.preferredMap.trim() });
      }
      break;
    }
    case 'COUNTER_STRIKE_2': {
      rows.push({ label: '모드', value: cs2ModeLabel(op.mode) });
      if (op.position) {
        rows.push({
          label: '포지션',
          value: (
            <span className="game-room-detail-modal__pos">
              <PositionIcon position={op.position} game="COUNTER_STRIKE_2" showLabel />
            </span>
          ),
        });
      }
      break;
    }
    default:
      break;
  }

  const { maxPlayers: maxP } = getRoomCapacityMeta(room);
  rows.push({
    label: '인원',
    value: maxP != null ? `${room.memberCount}/${maxP}` : partySizeLabel(op.partySize, room.game) || '-',
  });
  rows.push({ label: '방장', value: room.hostNickname ?? '-' });
  rows.push({ label: '등록일', value: formatModalDate(room.createdAt) });

  return rows;
}

function buildMatchRows(room: MatchRoomPeek): Row[] {
  const gameLabel = GAME_OPTIONS.find((o) => o.key === room.game)?.label ?? room.game;
  return [
    { label: '유형', value: '랜덤 매칭' },
    { label: '게임', value: gameLabel },
    {
      label: '인원',
      value: room.memberCount != null ? `${room.memberCount}명` : '-',
    },
    { label: '방장', value: room.hostNickname },
    { label: '등록일', value: formatModalDate(room.createdAt) },
  ];
}

type Props = {
  open: RoomModalOpen;
  onClose: () => void;
  /** API 게임방: 입장(이미 참여) 또는 참가 */
  onGameRoomAction: (room: GameRoomItem) => void;
  onMatchRoomEnter: (sessionId: number) => void;
  joinRoomId: number | null;
};

export default function GameRoomDetailModal({
  open,
  onClose,
  onGameRoomAction,
  onMatchRoomEnter,
  joinRoomId,
}: Props) {
  const onKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (!open) return;
    document.addEventListener('keydown', onKeyDown);
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = prev;
    };
  }, [open, onKeyDown]);

  if (!open) return null;

  const isGame = open.kind === 'game';
  const room = open.room;
  const rows = isGame ? buildGameRows(room as GameRoomItem) : buildMatchRows(room as MatchRoomPeek);

  const title = isGame
    ? (room as GameRoomItem).title
    : `[랜덤] ${GAME_OPTIONS.find((o) => o.key === (room as MatchRoomPeek).game)?.label ?? (room as MatchRoomPeek).game}`;

  const primaryLabel =
    isGame && (room as GameRoomItem).closed
      ? '마감'
      : isGame && (room as GameRoomItem).isMember && (room as GameRoomItem).groupChatRoomId
        ? '입장'
        : isGame && getRoomCapacityMeta(room as GameRoomItem).isFull
          ? '모집 완료'
          : isGame
            ? '참가'
            : '입장';

  const primaryDisabled =
    isGame &&
    ((room as GameRoomItem).closed ||
      joinRoomId === (room as GameRoomItem).id ||
      getRoomCapacityMeta(room as GameRoomItem).isFull);

  return (
    <div
      className="game-room-detail-modal-backdrop"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="game-room-detail-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="game-room-detail-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="game-room-detail-modal__head">
          <h2 id="game-room-detail-modal-title" className="game-room-detail-modal__title">
            방 정보
          </h2>
          <button type="button" className="game-room-detail-modal__close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>
        <p className="game-room-detail-modal__subtitle">{title}</p>
        <dl className="game-room-detail-modal__dl">
          {rows.map((row) => (
            <div key={row.label} className="game-room-detail-modal__row">
              <dt>{row.label}</dt>
              <dd>{row.value}</dd>
            </div>
          ))}
        </dl>
        <div className="game-room-detail-modal__actions">
          <button type="button" className="game-room-detail-modal__btn ghost" onClick={onClose}>
            닫기
          </button>
          {isGame ? (
            <button
              type="button"
              className="game-room-detail-modal__btn primary"
              disabled={!!primaryDisabled}
              onClick={() => onGameRoomAction(room as GameRoomItem)}
            >
              {joinRoomId === (room as GameRoomItem).id ? '참가 중…' : primaryLabel}
            </button>
          ) : (
            <button type="button" className="game-room-detail-modal__btn primary" onClick={() => onMatchRoomEnter((room as MatchRoomPeek).id)}>
              입장
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
