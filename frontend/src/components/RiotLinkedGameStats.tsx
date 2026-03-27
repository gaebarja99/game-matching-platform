import { Link } from 'react-router-dom';
import { buildRecordsProfileUrl, parseRiotDisplayName } from '../pages/records/recordsShared';

/**
 * Riot 연동 행에서 LoL / 발로란트 랭크를 게임별로 구분해 표시한다.
 * 블록 클릭 시 전적 검색(해당 게임)으로 이동한다.
 */
export function RiotLinkedGameStats(props: {
  /** `게임명#태그` — 전적 검색 다이렉트에 사용 */
  riotDisplayName?: string | null;
  lolRankSummary?: string | null;
  valorantRankSummary?: string | null;
  /** 본인 프로필 미리보기에서만 게임별 공개·비공개 배지 */
  showVisibilityBadges?: boolean;
  lolPublic?: boolean;
  valorPublic?: boolean;
}) {
  const {
    riotDisplayName,
    lolRankSummary,
    valorantRankSummary,
    showVisibilityBadges,
    lolPublic,
    valorPublic,
  } = props;

  const parsed = parseRiotDisplayName(riotDisplayName);
  const lolTo = parsed
    ? buildRecordsProfileUrl('lol', parsed.gameName, parsed.tagLine, '', 5)
    : '/records?game=lol';
  const valTo = parsed
    ? buildRecordsProfileUrl('valorant', parsed.gameName, parsed.tagLine, '', 5)
    : '/records?game=valorant';

  return (
    <div className="riot-game-stats" role="group" aria-label="Riot 게임별 전적">
      <Link
        to={lolTo}
        className="riot-game-stats__block riot-game-stats__block--lol riot-game-stats__block--link"
        aria-label="리그 오브 레전드 전적 검색으로 이동"
      >
        <div className="riot-game-stats__head">
          <span className="riot-game-stats__title">리그 오브 레전드</span>
          {showVisibilityBadges ? (
            <span className={`riot-game-stats__badge ${lolPublic === false ? 'is-off' : 'is-on'}`}>
              {lolPublic === false ? '비공개' : '공개'}
            </span>
          ) : null}
        </div>
        <p className={`riot-game-stats__detail ${!lolRankSummary?.trim() ? 'is-empty' : ''}`}>
          {lolRankSummary?.trim() || '표시할 랭크 정보가 없습니다.'}
        </p>
      </Link>
      <Link
        to={valTo}
        className="riot-game-stats__block riot-game-stats__block--valorant riot-game-stats__block--link"
        aria-label="발로란트 전적 검색으로 이동"
      >
        <div className="riot-game-stats__head">
          <span className="riot-game-stats__title">발로란트</span>
          {showVisibilityBadges ? (
            <span className={`riot-game-stats__badge ${valorPublic === false ? 'is-off' : 'is-on'}`}>
              {valorPublic === false ? '비공개' : '공개'}
            </span>
          ) : null}
        </div>
        <p className={`riot-game-stats__detail ${!valorantRankSummary?.trim() ? 'is-empty' : ''}`}>
          {valorantRankSummary?.trim() || '표시할 경쟁 티어 정보가 없습니다.'}
        </p>
      </Link>
    </div>
  );
}
