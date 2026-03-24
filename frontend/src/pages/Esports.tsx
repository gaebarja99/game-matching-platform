import { useMemo, useState } from 'react';
import Layout from '../components/Layout';

type MatchStatus = 'FINAL' | 'UPCOMING' | 'LIVE';

type MatchRow = {
  id: string;
  month: string;
  dayLabel: string;
  time: string;
  status: MatchStatus;
  round: string;
  leftTeam: string;
  rightTeam: string;
  leftCode: string;
  rightCode: string;
  leftScore?: number;
  rightScore?: number;
  venue: string;
  cta: string;
};

type LeagueData = {
  id: string;
  label: string;
  shortLabel: string;
  title: string;
  description: string;
  featureLabel: string;
  featureSummary: string;
  months: string[];
  teamFilters: { id: string; label: string }[];
  matches: MatchRow[];
};

const leagueData: LeagueData[] = [
  {
    id: 'lck',
    label: 'LCK',
    shortLabel: 'L',
    title: '리그 오브 레전드 챔피언스 코리아',
    description: '정규 시즌 흐름을 우리 서비스 톤에 맞는 카드형 일정 허브로 정리했습니다.',
    featureLabel: '이달의 메인 매치',
    featureSummary: 'Gen.G와 한화생명의 상위권 맞대결 중심으로 3월 일정을 배치했습니다.',
    months: ['1월', '2월', '3월', '4월'],
    teamFilters: [
      { id: 'all', label: '전체' },
      { id: 'gen', label: 'Gen.G' },
      { id: 'hle', label: '한화생명' },
      { id: 't1', label: 'T1' },
      { id: 'kt', label: 'KT' },
    ],
    matches: [
      {
        id: 'lck-0301-1',
        month: '3월',
        dayLabel: '03월 01일 (일)',
        time: '17:00',
        status: 'FINAL',
        round: '결승전',
        leftTeam: 'Gen.G',
        rightTeam: 'BNK 피어엑스',
        leftCode: 'GEN',
        rightCode: 'BFX',
        leftScore: 3,
        rightScore: 0,
        venue: '홍콩 카이탁 아레나',
        cta: '다시보기',
      },
      {
        id: 'lck-0303-1',
        month: '3월',
        dayLabel: '03월 03일 (화)',
        time: '17:00',
        status: 'FINAL',
        round: '정규 시즌 1R',
        leftTeam: '한화생명e스포츠',
        rightTeam: 'DRX',
        leftCode: 'HLE',
        rightCode: 'DRX',
        leftScore: 2,
        rightScore: 0,
        venue: '종로 롤파크',
        cta: '하이라이트',
      },
      {
        id: 'lck-0303-2',
        month: '3월',
        dayLabel: '03월 03일 (화)',
        time: '19:30',
        status: 'FINAL',
        round: '정규 시즌 1R',
        leftTeam: 'T1',
        rightTeam: '디플러스 기아',
        leftCode: 'T1',
        rightCode: 'DK',
        leftScore: 2,
        rightScore: 1,
        venue: '종로 롤파크',
        cta: '다시보기',
      },
      {
        id: 'lck-0306-1',
        month: '3월',
        dayLabel: '03월 06일 (금)',
        time: '17:00',
        status: 'UPCOMING',
        round: '정규 시즌 1R',
        leftTeam: 'Gen.G',
        rightTeam: 'KT 롤스터',
        leftCode: 'GEN',
        rightCode: 'KT',
        venue: '종로 롤파크',
        cta: '알림받기',
      },
      {
        id: 'lck-0306-2',
        month: '3월',
        dayLabel: '03월 06일 (금)',
        time: '19:30',
        status: 'UPCOMING',
        round: '정규 시즌 1R',
        leftTeam: '농심 레드포스',
        rightTeam: '피어엑스',
        leftCode: 'NS',
        rightCode: 'BFX',
        venue: '종로 롤파크',
        cta: '알림받기',
      },
      {
        id: 'lck-0402-1',
        month: '4월',
        dayLabel: '04월 02일 (목)',
        time: '18:00',
        status: 'UPCOMING',
        round: '정규 시즌 2R',
        leftTeam: '한화생명e스포츠',
        rightTeam: 'T1',
        leftCode: 'HLE',
        rightCode: 'T1',
        venue: '종로 롤파크',
        cta: '알림받기',
      },
    ],
  },
  {
    id: 'first-stand',
    label: '퍼스트 스탠드',
    shortLabel: 'F',
    title: '리그 오브 레전드 퍼스트 스탠드',
    description: '국제전 토너먼트 흐름을 짧고 강하게 보여주는 글로벌 일정 카드입니다.',
    featureLabel: 'Featured Match',
    featureSummary: '한화생명과 Karmine Corp의 결승전을 중심으로 국제전 일정을 구성했습니다.',
    months: ['3월'],
    teamFilters: [
      { id: 'all', label: '전체' },
      { id: 'hle', label: 'HLE' },
      { id: 'kc', label: 'KC' },
      { id: 'tes', label: 'TES' },
      { id: 'cfo', label: 'CFO' },
    ],
    matches: [
      {
        id: 'fs-0310-1',
        month: '3월',
        dayLabel: '03월 10일 (월)',
        time: '17:00',
        status: 'FINAL',
        round: 'Opening Day',
        leftTeam: 'Hanwha Life Esports',
        rightTeam: 'Top Esports',
        leftCode: 'HLE',
        rightCode: 'TES',
        leftScore: 2,
        rightScore: 0,
        venue: '서울 국제전 스테이지',
        cta: '하이라이트',
      },
      {
        id: 'fs-0315-1',
        month: '3월',
        dayLabel: '03월 15일 (토)',
        time: '20:30',
        status: 'FINAL',
        round: 'Semifinal',
        leftTeam: 'Karmine Corp',
        rightTeam: 'CTBC Flying Oyster',
        leftCode: 'KC',
        rightCode: 'CFO',
        leftScore: 3,
        rightScore: 2,
        venue: '서울 국제전 스테이지',
        cta: '다시보기',
      },
      {
        id: 'fs-0316-1',
        month: '3월',
        dayLabel: '03월 16일 (일)',
        time: '17:00',
        status: 'FINAL',
        round: 'Grand Final',
        leftTeam: 'Hanwha Life Esports',
        rightTeam: 'Karmine Corp',
        leftCode: 'HLE',
        rightCode: 'KC',
        leftScore: 3,
        rightScore: 1,
        venue: '서울 국제전 스테이지',
        cta: '다시보기',
      },
    ],
  },
  {
    id: 'lec',
    label: 'LEC',
    shortLabel: 'E',
    title: 'LEC 일정',
    description: '유럽 리그 일정도 같은 카드 구조로 전환해 한 화면에서 리그별 흐름을 비교할 수 있습니다.',
    featureLabel: '이번 주 추천 경기',
    featureSummary: 'G2와 Fnatic의 라이벌전 중심으로 주간 경기를 보여줍니다.',
    months: ['2월', '3월'],
    teamFilters: [
      { id: 'all', label: '전체' },
      { id: 'g2', label: 'G2' },
      { id: 'fnc', label: 'Fnatic' },
      { id: 'kc', label: 'KC' },
    ],
    matches: [
      {
        id: 'lec-0222-1',
        month: '2월',
        dayLabel: '02월 22일 (토)',
        time: '20:00',
        status: 'FINAL',
        round: 'Regular Season',
        leftTeam: 'G2 Esports',
        rightTeam: 'Fnatic',
        leftCode: 'G2',
        rightCode: 'FNC',
        leftScore: 2,
        rightScore: 1,
        venue: '베를린 LEC 스튜디오',
        cta: '하이라이트',
      },
      {
        id: 'lec-0308-1',
        month: '3월',
        dayLabel: '03월 08일 (일)',
        time: '21:00',
        status: 'LIVE',
        round: 'Regular Season',
        leftTeam: 'Karmine Corp',
        rightTeam: 'G2 Esports',
        leftCode: 'KC',
        rightCode: 'G2',
        leftScore: 1,
        rightScore: 1,
        venue: '베를린 LEC 스튜디오',
        cta: '시청하기',
      },
    ],
  },
];

const allMonths = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];

function statusLabel(status: MatchStatus) {
  if (status === 'FINAL') return '종료';
  if (status === 'LIVE') return 'LIVE';
  return '예정';
}

function emblemClass(code: string) {
  const palette: Record<string, string> = {
    GEN: 'violet',
    HLE: 'mint',
    T1: 'red',
    KT: 'sky',
    BFX: 'amber',
    DRX: 'blue',
    DK: 'steel',
    NS: 'lime',
    KC: 'royal',
    TES: 'orange',
    CFO: 'teal',
    G2: 'gold',
    FNC: 'sun',
  };
  return palette[code] ?? 'neutral';
}

function scoreText(score?: number) {
  return typeof score === 'number' ? String(score) : '-';
}

function groupByDay(matches: MatchRow[]) {
  const map = new Map<string, MatchDay>();
  matches.forEach((match) => {
    const existing = map.get(match.dayLabel);
    if (existing) {
      existing.matches.push(match);
      return;
    }
    map.set(match.dayLabel, { id: match.id, label: match.dayLabel, matches: [match] });
  });
  return Array.from(map.values());
}

export default function Esports() {
  const [selectedLeagueId, setSelectedLeagueId] = useState('lck');
  const [selectedMonth, setSelectedMonth] = useState('3월');
  const [selectedTeam, setSelectedTeam] = useState('all');

  const selectedLeague = useMemo(
    () => leagueData.find((league) => league.id === selectedLeagueId) ?? leagueData[0],
    [selectedLeagueId],
  );

  const visibleMatches = useMemo(() => {
    return selectedLeague.matches.filter((match) => {
      const sameMonth = match.month === selectedMonth;
      const sameTeam =
        selectedTeam === 'all' ||
        match.leftCode.toLowerCase() === selectedTeam ||
        match.rightCode.toLowerCase() === selectedTeam;
      return sameMonth && sameTeam;
    });
  }, [selectedLeague, selectedMonth, selectedTeam]);

  const groupedDays = useMemo(() => groupByDay(visibleMatches), [visibleMatches]);
  const featureMatch = visibleMatches[0] ?? selectedLeague.matches[0];

  return (
    <Layout>
      <div className="esports-page esports-schedule-page">
        <section className="esports-schedule-hero">
          <div className="esports-schedule-hero-copy">
            <p className="esports-schedule-brand">GAMEMATCHER ESPORTS</p>
            <h1>{selectedLeague.title}</h1>
            <p>{selectedLeague.description}</p>
          </div>
          <div className="esports-schedule-feature">
            <span className="esports-schedule-chip active">{selectedLeague.featureLabel}</span>
            <strong>
              {featureMatch.leftTeam} vs {featureMatch.rightTeam}
            </strong>
            <p>
              {featureMatch.dayLabel} {featureMatch.time} · {featureMatch.round} ·{' '}
              {scoreText(featureMatch.leftScore)} : {scoreText(featureMatch.rightScore)}
            </p>
            <p>{selectedLeague.featureSummary}</p>
          </div>
        </section>

        <section className="esports-league-strip">
          {leagueData.map((league) => (
            <button
              key={league.id}
              type="button"
              className={`esports-league-pill${league.id === selectedLeague.id ? ' active' : ''}`}
              onClick={() => {
                setSelectedLeagueId(league.id);
                setSelectedMonth(league.months[0]);
                setSelectedTeam('all');
              }}
            >
              <span className={`esports-league-icon emblem-${emblemClass(league.shortLabel)}`}>{league.shortLabel}</span>
              <span>{league.label}</span>
            </button>
          ))}
          <button type="button" className="esports-league-more" aria-label="더 보기">
            &gt;
          </button>
        </section>

        <section className="esports-schedule-header">
          <div>
            <h2>{selectedLeague.title}</h2>
            <p>리그 탭과 월/팀 필터를 눌러 실제 일정 내용을 바꿔볼 수 있습니다.</p>
          </div>
          <div className="esports-season-switcher">
            <button type="button" aria-label="이전 연도">
              &lt;
            </button>
            <strong>2026</strong>
            <button type="button" aria-label="다음 연도">
              &gt;
            </button>
            <span className="esports-schedule-chip">최신</span>
          </div>
        </section>

        <section className="esports-month-tabs">
          {allMonths.map((month) => (
            <button
              key={month}
              type="button"
              className={`esports-month-tab${month === selectedMonth ? ' active' : ''}${selectedLeague.months.includes(month) ? '' : ' disabled'}`}
              onClick={() => {
                if (!selectedLeague.months.includes(month)) return;
                setSelectedMonth(month);
              }}
            >
              {month}
            </button>
          ))}
        </section>

        <section className="esports-team-filter-row">
          <button type="button" className="esports-filter-menu" aria-label="필터 메뉴">
            ≡
          </button>
          {selectedLeague.teamFilters.map((team) => (
            <button
              key={team.id}
              type="button"
              className={`esports-team-filter${team.id === selectedTeam ? ' active' : ''}`}
              onClick={() => setSelectedTeam(team.id)}
            >
              {team.label}
            </button>
          ))}
        </section>

        <section className="esports-schedule-board">
          {groupedDays.length === 0 ? (
            <article className="esports-empty-state">
              <strong>{selectedMonth}에는 조건에 맞는 경기가 없습니다.</strong>
              <p>다른 월이나 팀 탭을 눌러서 일정을 살펴보세요.</p>
            </article>
          ) : (
            groupedDays.map((day) => (
              <article key={day.label} className="esports-day-section">
                <header className="esports-day-header">
                  <h3>{day.label}</h3>
                </header>

                <div className="esports-day-list">
                  {day.matches.map((match) => (
                    <div key={match.id} className="esports-schedule-row">
                      <div className="esports-schedule-meta">
                        <strong>{match.time}</strong>
                        <span className={`esports-schedule-status${match.status === 'FINAL' ? ' final' : match.status === 'LIVE' ? ' live' : ''}`}>
                          {statusLabel(match.status)}
                        </span>
                        <span>{match.round}</span>
                      </div>

                      <div className="esports-schedule-matchup">
                        <div className="esports-schedule-team is-left">
                          <span className={`esports-team-emblem emblem-${emblemClass(match.leftCode)}`}>{match.leftCode}</span>
                          <span className="esports-team-name">{match.leftTeam}</span>
                        </div>
                        <div className="esports-schedule-score">
                          <strong>{scoreText(match.leftScore)}</strong>
                          <span>:</span>
                          <strong>{scoreText(match.rightScore)}</strong>
                        </div>
                        <div className="esports-schedule-team is-right">
                          <span className={`esports-team-emblem emblem-${emblemClass(match.rightCode)}`}>{match.rightCode}</span>
                          <span className="esports-team-name">{match.rightTeam}</span>
                        </div>
                      </div>

                      <div className="esports-schedule-actions">
                        <button type="button" className="esports-cta-button">
                          {match.cta}
                        </button>
                        <span>{match.venue}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </article>
            ))
          )}
        </section>
      </div>
    </Layout>
  );
}
