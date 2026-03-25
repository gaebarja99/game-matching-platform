import PositionIcon from './PositionIcon';
import { apiUrl } from '../api/client';

type PositionOption = {
  key: string;
  icon: string;
  label: string;
};

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
  { key: 'DAMAGE', icon: 'D', label: 'Damage' },
  { key: 'SUPPORT', icon: 'S', label: 'Support' },
  { key: 'TANK', icon: 'T', label: 'Tank' },
];

const CS2_POSITIONS: PositionOption[] = [
  { key: 'ENTRY', icon: 'E', label: 'Entry' },
  { key: 'SUPPORT', icon: 'S', label: 'Support' },
  { key: 'IGL', icon: 'I', label: 'IGL' },
  { key: 'AWPER', icon: 'A', label: 'AWPer' },
  { key: 'LURKER', icon: 'L', label: 'Lurker' },
];

const APEX_POSITIONS: PositionOption[] = [
  { key: 'ASSAULT', icon: 'A', label: 'Assault' },
  { key: 'SKIRMISHER', icon: 'S', label: 'Skirmisher' },
  { key: 'RECON', icon: 'R', label: 'Recon' },
  { key: 'CONTROLLER', icon: 'C', label: 'Controller' },
  { key: 'SUPPORT', icon: 'P', label: 'Support' },
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
  if (game === 'APEX_LEGENDS') return APEX_POSITIONS;
  if (game === 'PUBG') return PUBG_POSITIONS;
  return [];
}

function getAllImageForGame(game?: string | null): string | null {
  if (game === 'LEAGUE_OF_LEGENDS') return apiUrl('images/leagueoflegends/all.png');
  if (game === 'VALORANT') return apiUrl('images/valorant-roles/all.png');
  if (game === 'OVERWATCH') return apiUrl('images/overwatch-roles/role-all.png');
  if (game === 'COUNTER_STRIKE_2') return apiUrl('images/cs2-roles/role-all.png');
  if (game === 'APEX_LEGENDS') return apiUrl('images/apex-class/role-all.png');
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
  const positions = getPositionsForGame(game ?? undefined);
  const allImage = getAllImageForGame(game);
  const showExplicitAllOption = game === 'PUBG';

  if (positions.length === 0) return null;

  const isActive = (key: string) => {
    if (showExplicitAllOption && key === 'ALL') {
      return value == null || value === '' || value === 'ALL';
    }
    return value === key;
  };

  const handleClick = (key: string) => {
    if (disabled) return;
    if (showExplicitAllOption) {
      if (filterMode) {
        onChange(key === 'ALL' ? null : key);
        return;
      }
      onChange(value === key || key === 'ALL' ? (key === 'ALL' ? 'ALL' : null) : key);
      return;
    }

    onChange(value === key && !filterMode ? null : key);
  };

  return (
    <div
      className={`position-picker ${className}`}
      role="group"
      aria-label={filterMode ? 'Position filter' : 'Position select'}
    >
      {filterMode && includeAllOption && !showExplicitAllOption && (
        <button
          type="button"
          className={`position-picker-btn ${value === null || value === '' ? 'active' : ''}`}
          onClick={() => onChange(null)}
          title="All"
          disabled={disabled}
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
        >
          {showExplicitAllOption && position.key === 'ALL' && allImage ? (
            <img src={allImage} alt="All" className="position-icon-img" />
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
