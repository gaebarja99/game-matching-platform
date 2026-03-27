import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../../components/Layout';
import { GAMES, buildRecordsProfileUrl, fetchRecordsPlayerSearch } from './recordsShared';
import '../Records.css';

export default function RecordsSearch() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const urlGame = searchParams.get('game');
  const [gameId, setGameId] = useState(() =>
    urlGame && GAMES.some((g) => g.id === urlGame) ? urlGame : 'lol',
  );
  const [nickname, setNickname] = useState('');
  const [tagLine, setTagLine] = useState('');
  const [platform, setPlatform] = useState('');
  const [count, setCount] = useState(5);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [gameMenuOpen, setGameMenuOpen] = useState(false);
  const gameMenuRef = useRef<HTMLDivElement | null>(null);

  const game = useMemo(() => GAMES.find((item) => item.id === gameId) || GAMES[0], [gameId]);

  useEffect(() => {
    const g = searchParams.get('game');
    if (g && GAMES.some((x) => x.id === g)) {
      setGameId(g);
    }
  }, [searchParams]);

  useEffect(() => {
    if (!game.platformOptions?.length) {
      setPlatform('');
      return;
    }
    setPlatform((prev) => {
      const valid = game.platformOptions!.some((opt) => opt.value === prev);
      return valid ? prev : game.platformOptions![0].value;
    });
  }, [game]);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (gameMenuRef.current && !gameMenuRef.current.contains(e.target as Node)) {
        setGameMenuOpen(false);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setGameMenuOpen(false);
    };
    if (gameMenuOpen) {
      document.addEventListener('mousedown', onDoc);
      document.addEventListener('keydown', onKey);
    }
    return () => {
      document.removeEventListener('mousedown', onDoc);
      document.removeEventListener('keydown', onKey);
    };
  }, [gameMenuOpen]);

  const selectGame = (id: string) => {
    setGameId(id);
    setError(null);
    setGameMenuOpen(false);
    setSearchParams(
      (prev) => {
        const n = new URLSearchParams(prev);
        n.set('game', id);
        return n;
      },
      { replace: true },
    );
  };

  const runSearch = useCallback(async () => {
    const nick = nickname.trim();
    if (!nick) return;
    setLoading(true);
    setError(null);
    try {
      const response = await fetchRecordsPlayerSearch(gameId, nickname, tagLine, platform, count, false);
      if (!response.success) {
        setError(response.errorMessage || '검색에 실패했습니다.');
        return;
      }
      navigate(buildRecordsProfileUrl(gameId, nick, tagLine, platform, count), { replace: false });
    } catch (err) {
      setError(err instanceof Error ? err.message : '서버 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  }, [gameId, nickname, tagLine, platform, count, navigate]);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void runSearch();
  };

  return (
    <Layout>
      <div className="records-search-page">
        <section className="records-hero">
          <div className="records-hero-copy">
            <p className="records-kicker">DUO-STYLE SEARCH</p>
            <h1 className="records-title">게임 전적 검색을 더 빠르고 선명하게</h1>
            <p className="records-description">
              게임을 선택하고 닉네임을 입력하면 최근 매치 기록과 요약 통계를 확인할 수 있습니다. 검색 후 전용 전적 페이지로 이동합니다.
            </p>
          </div>
          <div className="records-hero-card">
            <div className="records-hero-meta-top">
              <span className="records-hero-badge">Live Search</span>
              <span className="records-hero-subtle">{game.label}</span>
            </div>
            <strong className="records-hero-highlight">{game.hint}</strong>
          </div>
        </section>

        <section className="records-search-shell" style={{ ['--records-accent' as string]: game.accent }}>
          <div className="records-game-dropdown" ref={gameMenuRef}>
            <span id="records-game-select-label" className="records-game-dropdown-label">
              게임
            </span>
            <button
              type="button"
              className={`records-game-dropdown-trigger ${gameMenuOpen ? 'is-open' : ''}`}
              aria-haspopup="listbox"
              aria-expanded={gameMenuOpen}
              aria-label={`게임 선택, 현재 ${game.label}`}
              onClick={() => setGameMenuOpen((o) => !o)}
            >
              <span className="records-game-short" aria-hidden>
                {game.short}
              </span>
              <span className="records-game-dropdown-trigger-text">{game.label}</span>
              <span className="records-game-dropdown-chevron" aria-hidden>
                ▾
              </span>
            </button>
            {gameMenuOpen ? (
              <ul className="records-game-dropdown-panel" role="listbox" aria-label="게임 선택">
                {GAMES.map((item) => (
                  <li key={item.id} role="presentation">
                    <button
                      type="button"
                      role="option"
                      aria-selected={item.id === gameId}
                      className={`records-game-dropdown-option ${item.id === gameId ? 'is-active' : ''}`}
                      style={{ ['--item-accent' as string]: item.accent }}
                      onClick={() => selectGame(item.id)}
                    >
                      <span className="records-game-short">{item.short}</span>
                      <span className="records-game-label">{item.label}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>

          <form className="records-search-card" onSubmit={handleSearch}>
            <div className="records-card-header">
              <div>
                <p className="records-card-kicker">Search Center</p>
                <h2 className="records-card-title">{game.label}</h2>
              </div>
              <span className="records-card-accent-chip">{game.short}</span>
            </div>

            <div className="records-search-grid">
              <label className="records-primary-field">
                <span>닉네임</span>
                <input
                  type="text"
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                  placeholder={game.placeholders.nickname}
                  autoComplete="off"
                  required
                />
              </label>

              {game.fields.includes('tag') ? (
                <label className="records-compact-field">
                  <span>{game.tagLabel || '태그'}</span>
                  <input
                    type="text"
                    value={tagLine}
                    onChange={(event) => setTagLine(event.target.value)}
                    placeholder={game.placeholders.tag || 'Tag'}
                    autoComplete="off"
                  />
                </label>
              ) : null}

              {game.fields.includes('pubg_platform') && game.platformOptions ? (
                <label className="records-compact-field">
                  <span>플랫폼</span>
                  <select value={platform} onChange={(event) => setPlatform(event.target.value)}>
                    {game.platformOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
              ) : null}

              {game.fields.includes('count') ? (
                <label className="records-compact-field">
                  <span>매치 수</span>
                  <select value={count} onChange={(event) => setCount(Number(event.target.value))}>
                    {[5, 10, 15, 20].map((option) => (
                      <option key={option} value={option}>
                        {option}게임
                      </option>
                    ))}
                  </select>
                </label>
              ) : null}
            </div>

            <div className="records-search-footer">
              <p className="records-inline-hint">{game.hint}</p>
              <div className="records-actions">
                {nickname || tagLine ? (
                  <button
                    type="button"
                    className="records-clear-button"
                    onClick={() => {
                      setNickname('');
                      setTagLine('');
                      setError(null);
                      navigate('/records', { replace: true });
                    }}
                  >
                    초기화
                  </button>
                ) : null}
                <button type="submit" className="records-search-button" disabled={loading}>
                  {loading ? '검색 중...' : '검색'}
                </button>
              </div>
            </div>
          </form>
        </section>

        {error ? (
          <section className="records-error-box">
            <strong>검색 실패</strong>
            {error.split('\n').map((line, index) => (
              <p key={`${line}-${index}`}>{line}</p>
            ))}
          </section>
        ) : null}
      </div>
    </Layout>
  );
}
