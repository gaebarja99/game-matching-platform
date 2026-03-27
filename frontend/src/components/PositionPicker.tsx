import PositionIcon from './PositionIcon';
import { TankIcon, DamageIcon, SupportIcon } from './overwatch';
import { EntryIcon, SupportIcon as Cs2SupportIcon, IglIcon, AwperIcon, LurkerIcon } from './cs2';
import { apiUrl } from '../api/client';

type PositionOption = {
  key: string;
  icon: string;
  label: string;
};

function getLolLocalMaskUrl(posKey: string): string | null {
  const k = posKey.toUpperCase();
  // 프론트(public)에서 같은 오리진으로 서빙해야 mask-image가 안정적으로 동작함
  if (k === 'TOP') return '/images/leagueoflegends/top.png';
  if (k === 'JUNGLE') return '/images/leagueoflegends/jg.png';
  if (k === 'MID' || k === 'MIDDLE') return '/images/leagueoflegends/mid.png';
  if (k === 'ADC' || k === 'BOTTOM') return '/images/leagueoflegends/adc.png';
  if (k === 'SUPPORT' || k === 'SUP' || k === 'UTILITY') return '/images/leagueoflegends/spt.png';
  return null;
}

/** 발로란트 역할 아이콘 — 로컬 public (랜덤매칭·방만들기 포지션 피커 전용) */
function getValorantLocalMaskUrl(posKey: string): string | null {
  const k = posKey.toUpperCase();
  if (k === 'DUELIST') return '/images/valorant-roles/duelist.png';
  if (k === 'SCOUT' || k === 'INITIATOR') return '/images/valorant-roles/scout.png';
  if (k === 'STRATEGIST' || k === 'CONTROLLER') return '/images/valorant-roles/strategist.png';
  if (k === 'WATCHER' || k === 'SENTINEL') return '/images/valorant-roles/watcher.png';
  return null;
}

/** OW2 역할 — SVG 컴포넌트 (공식 에셋 복제 아님, 프로젝트용 단순화 실루엣) */
function renderOverwatchRoleSvg(roleKey: string) {
  const k = roleKey.toUpperCase();
  const common = {
    className: 'position-icon-img ow-role-svg',
    width: 18,
    height: 18,
    'aria-hidden': true as const,
  };
  if (k === 'TANK') return <TankIcon {...common} />;
  if (k === 'DAMAGE' || k === 'DPS') return <DamageIcon {...common} />;
  if (k === 'SUPPORT' || k === 'HEALER') return <SupportIcon {...common} />;
  return null;
}

function renderCs2RoleSvg(roleKey: string) {
  const k = roleKey.toUpperCase();
  const common = {
    className: 'position-icon-img cs2-role-svg',
    width: 18,
    height: 18,
    'aria-hidden': true as const,
  };
  if (k === 'ENTRY') return <EntryIcon {...common} />;
  if (k === 'SUPPORT') return <Cs2SupportIcon {...common} />;
  if (k === 'IGL') return <IglIcon {...common} />;
  if (k === 'AWPER') return <AwperIcon {...common} />;
  if (k === 'LURKER') return <LurkerIcon {...common} />;
  return null;
}

function positionAccent(game: string | null | undefined, posKey: string): string {
  const g = (game ?? '').toUpperCase();
  const p = posKey.toUpperCase();
  if (g === 'LEAGUE_OF_LEGENDS') {
    if (p === 'TOP') return '#3B82F6';
    if (p === 'JUNGLE') return '#22C55E';
    if (p === 'MID' || p === 'MIDDLE') return '#A855F7';
    if (p === 'ADC' || p === 'BOTTOM') return '#EF4444';
    if (p === 'SUPPORT' || p === 'SUP') return '#10B981';
  }
  if (g === 'VALORANT') {
    if (p === 'DUELIST') return '#EF4444';
    if (p === 'SCOUT' || p === 'INITIATOR') return '#3B82F6';
    if (p === 'STRATEGIST' || p === 'CONTROLLER') return '#22C55E';
    if (p === 'WATCHER' || p === 'SENTINEL') return '#F59E0B';
  }
  if (g === 'OVERWATCH') {
    if (p === 'TANK') return '#3B82F6';
    if (p === 'DAMAGE') return '#EF4444';
    if (p === 'SUPPORT') return '#F59E0B';
  }
  if (g === 'COUNTER_STRIKE_2') {
    if (p === 'ENTRY') return '#EF4444';
    if (p === 'AWPER') return '#38BDF8';
    if (p === 'IGL') return '#A855F7';
    if (p === 'SUPPORT') return '#22C55E';
    if (p === 'LURKER') return '#F59E0B';
  }
  if (g === 'PUBG') {
    if (p === 'DUO') return '#38BDF8';
    if (p === 'SQUAD') return '#F59E0B';
    if (p === 'ALL') return '#9CA3AF';
  }
  return '#9CA3AF';
}

const LOL_POSITIONS: PositionOption[] = [
  { key: 'TOP', icon: 'T', label: 'Top' },
  { key: 'JUNGLE', icon: 'J', label: 'Jungle' },
  { key: 'MID', icon: 'M', label: 'Mid' },
  { key: 'ADC', icon: 'A', label: 'ADC' },
  { key: 'SUPPORT', icon: 'S', label: 'Support' },
];

const VALORANT_POSITIONS: PositionOption[] = [
  { key: 'DUELIST', icon: 'D', label: 'Duelist' },
  { key: 'SCOUT', icon: 'I', label: 'Initiator' },
  { key: 'STRATEGIST', icon: 'C', label: 'Controller' },
  { key: 'WATCHER', icon: 'W', label: 'Sentinel' },
];

const OVERWATCH_POSITIONS: PositionOption[] = [
  { key: 'TANK', icon: 'T', label: 'Tank' },
  { key: 'DAMAGE', icon: 'D', label: 'Damage' },
  { key: 'SUPPORT', icon: 'S', label: 'Support' },
];

const CS2_POSITIONS: PositionOption[] = [
  { key: 'ENTRY', icon: 'E', label: 'Entry' },
  { key: 'SUPPORT', icon: 'S', label: 'Support' },
  { key: 'IGL', icon: 'I', label: 'IGL' },
  { key: 'AWPER', icon: 'A', label: 'AWPer' },
  { key: 'LURKER', icon: 'L', label: 'Lurker' },
];

const PUBG_POSITIONS: PositionOption[] = [
  { key: 'ALL', icon: 'A', label: 'All' },
  { key: 'DUO', icon: 'D', label: 'Duo' },
  { key: 'SQUAD', icon: 'S', label: 'Squad' },
];

function getPositionsForGame(game?: string | null): PositionOption[] {
  if (!game || game === 'LEAGUE_OF_LEGENDS' || game === 'ALL') return LOL_POSITIONS;
  if (game === 'VALORANT') return VALORANT_POSITIONS;
  if (game === 'OVERWATCH') return OVERWATCH_POSITIONS;
  if (game === 'COUNTER_STRIKE_2') return CS2_POSITIONS;
  if (game === 'PUBG') return PUBG_POSITIONS;
  return [];
}

function getAllImageForGame(game?: string | null): string | null {
  if (game === 'LEAGUE_OF_LEGENDS') return apiUrl('images/leagueoflegends/all.png');
  // Valorant 필터의 전체 버튼도 같은 오리진(public)으로
  if (game === 'VALORANT') return '/images/valorant-roles/all.png';
  if (game === 'OVERWATCH') return '/images/overwatch-roles/role-all.png';
  if (game === 'COUNTER_STRIKE_2') return apiUrl('images/cs2-roles/role-all.png');
  if (game === 'PUBG') return apiUrl('images/pubg-team/team-all.png');
  return null;
}

interface PositionPickerProps {
  value: string | null;
  onChange: (position: string | null) => void;
  game?: string | null;
  filterMode?: boolean;
  includeAllOption?: boolean;
  disabled?: boolean;
  className?: string;
}

export default function PositionPicker({
  value,
  onChange,
  game,
  filterMode,
  includeAllOption = true,
  disabled = false,
  className = '',
}: PositionPickerProps) {
  const gameKey = (game ?? '').toUpperCase();
  const positions = getPositionsForGame(game ?? undefined);
  const allImage = getAllImageForGame(game);
  const showExplicitAllOption = game === 'PUBG';
  const useLolLocalIcons = gameKey === 'LEAGUE_OF_LEGENDS';
  const useValorantLocalIcons = gameKey === 'VALORANT';
  const useOverwatchLocalIcons = gameKey === 'OVERWATCH';
  const useCs2RoleIcons = gameKey === 'COUNTER_STRIKE_2';

  if (positions.length === 0) return null;

  const valueNorm = value?.trim().toUpperCase() ?? '';

  const isActive = (key: string) => {
    if (showExplicitAllOption && key === 'ALL') {
      return value == null || value === '' || value === 'ALL';
    }
    return valueNorm === key;
  };

  const handleClick = (key: string) => {
    if (disabled) return;
    if (showExplicitAllOption) {
      if (filterMode) {
        onChange(key === 'ALL' ? null : key);
        return;
      }
      onChange(valueNorm === key || key === 'ALL' ? (key === 'ALL' ? 'ALL' : null) : key);
      return;
    }

    onChange(valueNorm === key && !filterMode ? null : key);
  };

  return (
    <div
      className={`position-picker ${className}`}
      role="group"
      aria-label={filterMode ? 'Position filter' : 'Position select'}
      data-game={gameKey}
    >
      {filterMode && includeAllOption && !showExplicitAllOption && (
        <button
          type="button"
          className={`position-picker-btn ${value === null || value === '' ? 'active' : ''}`}
          onClick={() => onChange(null)}
          title="All"
          disabled={disabled}
          style={{ ['--pos-accent' as any]: positionAccent(game, 'ALL') }}
          data-game={gameKey}
        >
          {allImage ? (
            <img src={allImage} alt="All" className="position-icon-img" />
          ) : (
            'All'
          )}
        </button>
      )}

      {positions.map((position) => (
        <button
          key={position.key}
          type="button"
          className={`position-picker-btn ${isActive(position.key) ? 'active' : ''}`}
          onClick={() => handleClick(position.key)}
          title={position.label}
          disabled={disabled}
          style={{ ['--pos-accent' as any]: positionAccent(game, position.key) }}
          data-game={gameKey}
        >
          {showExplicitAllOption && position.key === 'ALL' && allImage ? (
            <img src={allImage} alt="All" className="position-icon-img" />
          ) : useLolLocalIcons ? (
            (() => {
              const maskUrl = getLolLocalMaskUrl(position.key);
              return maskUrl ? (
                <span
                  className="position-icon-img position-icon-mask"
                  style={{ ['--pos-mask-image' as any]: `url("${maskUrl}")` }}
                  aria-hidden
                />
              ) : (
                <PositionIcon position={position.key} game={game} showLabel={false} />
              );
            })()
          ) : useValorantLocalIcons ? (
            (() => {
              const maskUrl = getValorantLocalMaskUrl(position.key);
              return maskUrl ? (
                <span
                  className="position-icon-img position-icon-mask"
                  style={{ ['--pos-mask-image' as any]: `url("${maskUrl}")` }}
                  aria-hidden
                />
              ) : (
                <PositionIcon position={position.key} game={game} showLabel={false} />
              );
            })()
          ) : useOverwatchLocalIcons ? (
            renderOverwatchRoleSvg(position.key) ?? (
              <PositionIcon position={position.key} game={game} showLabel={false} />
            )
          ) : useCs2RoleIcons ? (
            renderCs2RoleSvg(position.key) ?? (
              <PositionIcon position={position.key} game={game} showLabel={false} />
            )
          ) : game ? (
            <PositionIcon position={position.key} game={game} showLabel={false} />
          ) : (
            position.icon
          )}
        </button>
      ))}
    </div>
  );
}
