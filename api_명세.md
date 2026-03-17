# GameMatcher API 명세

게임별 외부 API 사용 명세 통합 문서입니다.

---

# 목차

1. [발로란트 (Valorant)](#1-발로란트-valorant)
2. [LoL (League of Legends)](#2-lol-league-of-legends)
3. [TFT (Teamfight Tactics)](#3-tft-teamfight-tactics)
4. [Steam](#4-steam)
5. [오버워치 2](#5-오버워치-2)
6. [PUBG](#6-pubg)
7. [Apex Legends](#7-apex-legends)
8. [CS2 (Counter-Strike 2)](#8-cs2-counter-strike-2)
9. [Blizzard](#9-blizzard)

---

# 1. 발로란트 (Valorant)

Henrik Dev API (https://api.henrikdev.xyz)  
인증: `?api_key={key}` 쿼리 파라미터 (또는 Authorization 헤더)

## 1.1 계정 정보 조회 (PUUID 확인용)

닉네임과 태그로 유저의 고유 ID(PUUID) 및 기본 정보를 확인합니다.

형식: `https://api.henrikdev.xyz/valorant/v1/account/{name}/{tag}?api_key={key}`

예시: `https://api.henrikdev.xyz/valorant/v1/account/TenZ/NA1?api_key=abc123`

## 1.2 간략 전적 리스트 (최근 매치 목록)

유저의 최근 전적 요약본을 가져오며, 상세 페이지 이동을 위한 match_id를 확보하는 용도입니다.

형식: `https://api.henrikdev.xyz/valorant/v1/lifetime/matches/{region}/by-puuid/{puuid}?api_key={key}`

예시: `https://api.henrikdev.xyz/valorant/v1/lifetime/matches/na/by-puuid/abc123-def456-789?api_key=abc123&size=20&mode=competitive`

주요 옵션:
- `&size={count}`: 불러올 개수 (최대 100)
- `&mode={mode}`: 게임 모드 필터 (competitive, unrated 등)

## 1.3 매치 상세 데이터

특정 매치의 전체 플레이어 스탯, 라운드별 승패 기록, 킬 로그 등 모든 상세 정보를 가져옵니다.

형식: `https://api.henrikdev.xyz/valorant/v2/match/{match_id}?api_key={key}`

예시: `https://api.henrikdev.xyz/valorant/v2/match/abc123-match-id-xyz?api_key=abc123`

참고: match_id는 1.2에서 얻은 매치 고유 ID를 사용합니다.

## 1.4 실시간 MMR 및 티어 정보

유저의 현재 티어, 랭킹 점수(RR), 가장 최근 게임의 점수 변동 폭을 확인합니다.

형식: `https://api.henrikdev.xyz/valorant/v2/by-puuid/mmr/{region}/{puuid}?api_key={key}`

예시: `https://api.henrikdev.xyz/valorant/v2/by-puuid/mmr/na/abc123-def456-789?api_key=abc123`

## 1.5 MMR 변동 이력

최근 치른 랭크 게임들의 점수 변화 기록을 리스트 형식으로 가져옵니다.

형식: `https://api.henrikdev.xyz/valorant/v1/by-puuid/mmr-history/{region}/{puuid}?api_key={key}`

예시: `https://api.henrikdev.xyz/valorant/v1/by-puuid/mmr-history/na/abc123-def456-789?api_key=abc123`

## application.properties

```
valorant.api.base-url=https://api.henrikdev.xyz
valorant.api.key={API_KEY}
```

---

# 2. LoL (League of Legends)

Riot Games 공식 API  
문서: https://developer.riotgames.com/  
인증: `X-Riot-Token` 헤더에 API 키 전달

Base URL은 리전별로 다름:
- **Regional** (asia, americas, europe, sea): 매치 조회 등
- **Platform** (kr1, na1, jp1 등): 소환사/계정 조회

## 2.1 계정 정보 조회 (PUUID 확보)

Riot ID(닉네임 + 태그)로 PUUID 및 기본 정보를 조회합니다.

형식: `{regional_base_url}/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}`

예시: `https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Hide on bush/KR1`

## 2.2 소환사 정보 조회 (레벨, 프로필아이콘)

PUUID로 소환사 상세 정보를 조회합니다.

형식: `{platform_base_url}/lol/summoner/v4/summoners/by-puuid/{puuid}`

예시: `https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/ggrdvya4vWUKvZHsaUbvX9B80tSUH6jLAwjuSxxM0AbrhIciAFne_emQXGppZ7I6mJaYAb_JKZYAFA`

응답: summonerLevel, profileIconId, id(summonerId) 등

## 2.3 매치 ID 목록 조회

PUUID로 최근 매치 ID 목록을 가져옵니다.

형식: `{regional_base_url}/lol/match/v5/matches/by-puuid/{puuid}/ids?start={start}&count={count}`

예시: `https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/ggrdvya4vWUKvZHsaUbvX9B80tSUH6jLAwjuSxxM0AbrhIciAFne_emQXGppZ7I6mJaYAb_JKZYAFA/ids?count=5`

주요 옵션:
- `start`: 시작 인덱스
- `count`: 불러올 개수 (최대 100)

## 2.4 매치 상세 데이터

매치 ID로 상세 정보(참가자, 챔피언, K/D/A 등)를 조회합니다.

형식: `{regional_base_url}/lol/match/v5/matches/{matchId}`

예시: `https://asia.api.riotgames.com/lol/match/v5/matches/KR_8136533346`

## 2.5 매치 타임라인 조회

매치 ID로 분 단위 프레임 데이터를 조회합니다. 프레임별 골드·CS·레벨 변화, 참가자별 타임라인 등이 포함됩니다.

형식: `{regional_base_url}/lol/match/v5/matches/{matchId}/timeline`

예시: `https://asia.api.riotgames.com/lol/match/v5/matches/KR_8136533346/timeline`

주요 응답 필드:
- `metadata`: matchId 등 메타 정보
- `info.frames[]`: 분 단위 프레임 배열
  - `participantFrames`: 참가자별 골드, CS, 레벨, 위치 등
  - `events`: 킬, 타워 파괴, 드래곤 획득 등 이벤트 목록

참고: matchId는 2.3 매치 ID 목록 또는 2.4 매치 상세에서 얻은 값을 사용합니다.

## 2.6 랭크 정보 조회 (솔로/자유 랭크)

소환사 ID로 랭크 정보를 조회합니다.

형식: `{platform_base_url}/lol/league/v4/entries/by-summoner/{summonerId}`

예시: `https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/abc123xyz`

응답: queueType, tier, rank, leaguePoints, wins, losses 등

## application.properties

```
riot.api.key={API_KEY}
riot.api.regional-base-url=https://asia.api.riotgames.com
riot.api.platform-base-url=https://kr.api.riotgames.com
```

---

# 3. TFT (Teamfight Tactics)

Riot Games 공식 API (LoL과 동일 Base URL)  
인증: `X-Riot-Token` 헤더에 API 키 전달

## 3.1 계정 정보 조회 (PUUID 확보)

Riot ID로 PUUID 조회. LoL과 동일 엔드포인트 사용.

형식: `{regional_base_url}/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}`

예시: `https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Hide on bush/KR1`

## 3.2 TFT 소환사 조회 (닉네임)

닉네임으로 TFT 소환사 정보를 조회합니다.

형식: `https://{platform}.api.riotgames.com/tft/summoner/v1/summoners/by-name/{summonerName}`

예시: `https://kr.api.riotgames.com/tft/summoner/v1/summoners/by-name/닉네임`

## 3.3 TFT 소환사 조회 (PUUID)

PUUID로 TFT 소환사 정보를 조회합니다.

형식: `https://{platform}.api.riotgames.com/tft/summoner/v1/summoners/by-puuid/{puuid}`

예시: `https://kr.api.riotgames.com/tft/summoner/v1/summoners/by-puuid/ggrdvya4vWUKvZHsaUbvX9B80tSUH6jLAwjuSxxM0AbrhIciAFne_emQXGppZ7I6mJaYAb_JKZYAFA`

## 3.4 TFT 매치 ID 목록 조회

PUUID로 TFT 최근 매치 ID 목록을 가져옵니다.

형식: `{regional_base_url}/tft/match/v1/matches/by-puuid/{puuid}/ids?start={start}&count={count}`

예시: `https://asia.api.riotgames.com/tft/match/v1/matches/by-puuid/ggrdvya4vWUKvZHsaUbvX9B80tSUH6jLAwjuSxxM0AbrhIciAFne_emQXGppZ7I6mJaYAb_JKZYAFA/ids?count=5`

주요 옵션:
- `start`: 시작 인덱스
- `count`: 불러올 개수

## 3.5 TFT 매치 상세 데이터

매치 ID로 TFT 매치 상세(placement, level, units, traits 등)를 조회합니다.

형식: `{regional_base_url}/tft/match/v1/matches/{matchId}`

예시: `https://asia.api.riotgames.com/tft/match/v1/matches/KR_7551308036`

응답: metadata.match_id, info.participants (placement, level, traits, units) 등

## 3.6 TFT 랭크 정보 조회

소환사 ID로 TFT 랭크를 조회합니다.

형식: `https://{platform}.api.riotgames.com/tft/league/v1/entries/by-summoner/{summonerId}`

예시: `https://kr.api.riotgames.com/tft/league/v1/entries/by-summoner/Hide on bush`

## application.properties

```
riot.api.key={API_KEY}
riot.api.regional-base-url=https://asia.api.riotgames.com
riot.api.platform-base-url=https://kr.api.riotgames.com
```

---

# 4. Steam

Steam Web API 공식 문서  
문서: https://steamcommunity.com/dev  
API 키 발급: https://steamcommunity.com/dev/apikey

모든 요청에 `key={api_key}` 쿼리 파라미터 필요

## 4.1 플레이어 프로필 요약

Steam64 ID로 프로필 정보(닉네임, 아바타 등)를 조회합니다.

형식: `https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key={key}&steamids={steamids}`

예시: `https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=abc123&steamids=76561198012345678`

주요 옵션:
- `steamids`: Steam64 ID (콤마로 여러 개 가능)

## 4.2 Vanity URL → Steam64 ID 변환

커스텀 URL(예: /id/nickname)을 Steam64 숫자 ID로 변환합니다.

형식: `https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key={key}&vanityurl={vanityurl}`

예시: `https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=abc123&vanityurl=gabelogan`

응답: `response.success == 1` 이면 `response.steamid` 에 Steam64 ID

## 4.3 최근 플레이 게임 목록

최근 플레이한 게임 목록을 조회합니다.

형식: `https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key={key}&steamid={steamid}&count={count}`

예시: `https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key=abc123&steamid=76561198012345678&count=10`

주요 옵션:
- `count`: 불러올 개수

응답: appid, name, playtime_2weeks, playtime_forever 등

## 4.4 보유 게임 목록

계정이 소유한 게임 목록과 플레이 타임을 조회합니다.

형식: `https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key={key}&steamid={steamid}&include_appinfo={0|1}`

예시: `https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=abc123&steamid=76561198012345678&include_appinfo=1`

주요 옵션:
- `include_appinfo`: 1이면 게임 이름 등 메타데이터 포함

## application.properties

```
steam.api.key={API_KEY}
```

---

# 5. 오버워치 2

ow-api.com 비공식 API 사용 (API 키 불필요)  
문서: https://ow-api.com/docs/

플레이어 프로필이 '공개(public)' 설정이어야 데이터 조회 가능.

## 5.1 프로필 조회

BattleTag로 플레이어 프로필을 조회합니다.

형식: `https://ow-api.com/v1/stats/{platform}/{region}/{battleTag}/profile`

주요 파라미터:
- `platform`: pc | psn | xbl (xbox)
- `region`: global (또는 리전 코드)
- `battleTag`: Fleta-3852 형식 (BattleTag의 # 를 - 로 변환)

예시: `https://ow-api.com/v1/stats/pc/global/Fleta-3852/profile`

응답: name, icon, level, prestige, private, competitive, quickPlayStats, competitiveStats 등

## 참고

- `private: true` 이면 데이터 조회 불가
- competitive.pc 에 tank, damage, support 역할별 티어
- quickPlayStats.careerStats 에 영웅별 플레이 시간·통계

---

# 6. PUBG

PUBG 공식 API  
문서: https://developer.pubg.com/  
인증: `Authorization: Bearer {api_key}` 헤더

Base URL: `https://api.pubg.com/shards`

## 6.1 플레이어 조회

닉네임으로 플레이어 ID(accountId)를 조회합니다.

형식: `https://api.pubg.com/shards/{platform}/players?filter[playerNames]={nickname}`

예시: `https://api.pubg.com/shards/steam/players?filter[playerNames]=8ink-`

주요 파라미터:
- `platform`: steam | kakao | psn | xbox | stadia

헤더: `Authorization: Bearer {key}`, `Accept: application/vnd.api+json`

## 6.2 매치 ID 목록 (relationships)

플레이어 응답의 `relationships.matches.data` 에 최근 매치 ID 목록이 포함됩니다.

플레이어 조회 1회로 매치 ID 목록까지 함께 획득 가능.

## 6.3 매치 상세 데이터

매치 ID로 상세 정보를 조회합니다.

형식: `https://api.pubg.com/shards/{platform}/matches/{matchId}`

예시: `https://api.pubg.com/shards/steam/matches/04192032-7e46-4d3a-a430-28817c8c5bcc`

응답: data.attributes (gameMode, mapName, createdAt), included (participant stats: kills, winPlace, damageDealt 등)

## 6.4 시즌 목록 조회

현재 시즌 ID를 확인합니다.

형식: `https://api.pubg.com/shards/{platform}/seasons`

예시: `https://api.pubg.com/shards/steam/seasons`

응답: data[].attributes.isCurrentSeason 이 true 인 시즌 ID 사용

## 6.5 시즌별 랭크 통계

플레이어의 시즌별 랭크(티어)를 조회합니다.

형식: `https://api.pubg.com/shards/{platform}/players/{accountId}/seasons/{seasonId}/ranked`

예시: `https://api.pubg.com/shards/steam/players/account.abc123/seasons/division.bro.official.2024-01/ranked`

응답: rankedGameModeStats (squad-fpp, squad 등) → currentTier

## application.properties

```
pubg.api.key={API_KEY}
```

무료 티어: 약 10 req/min

---

# 7. Apex Legends

Mozambique Here API (비공식)  
문서: https://portal.mozambiquehe.re/  
API 키 발급: https://portal.mozambiquehe.re/

인증: `Authorization: {api_key}` 헤더

Base URL: `https://api.mozambiquehe.re`

## 7.1 플레이어 통합 조회 (Bridge)

플레이어 기본 정보, 랭크, 레전드별 통계를 한 번에 조회합니다.

형식: `https://api.mozambiquehe.re/bridge?version=5&platform={platform}&player={playerName}&merge=true&removeMerged=true`

주요 옵션:
- `platform`: PC | PS4 | X1 | Switch
- `player`: Origin/EA 닉네임

예시: `https://api.mozambiquehe.re/bridge?version=5&platform=PC&player=Shroud&merge=true&removeMerged=true`

응답: global (name, level, rank, avatar), realtime (currentLegend, isOnline), legends (all → 레전드별 데이터)

## 참고

- legends.all 에서 각 레전드별 킬, 데미지, 승리 등 통계
- global.rank 에 rankName, rankScore (RP)
- 레전드별 상세는 data[] 배열 내 key/value 형태

## application.properties

```
apex.api.key={API_KEY}
```

---

# 8. CS2 (Counter-Strike 2)

Steam Web API 사용 (Steam API 키 필요)  
문서: https://steamcommunity.com/dev  
API 키 발급: https://steamcommunity.com/dev/apikey

CS2 App ID: 730

## 8.1 플레이어 프로필 요약

Steam64 ID로 프로필 정보를 조회합니다.

형식: `https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key={key}&steamids={steamids}`

예시: `https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=abc123&steamids=76561198012345678`

## 8.2 Vanity URL → Steam64 ID 변환

커스텀 URL을 Steam64 ID로 변환합니다.

형식: `https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key={key}&vanityurl={vanityurl}`

예시: `https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=abc123&vanityurl=s1mple`

## 8.3 CS2 게임 통계 조회

Steam64 ID로 CS2 누적 통계를 조회합니다.

형식: `https://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v2/?key={key}&steamid={steamid}&appid=730`

예시: `https://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v2/?key=abc123&steamid=76561198012345678&appid=730`

응답: playerstats.stats[] 배열 (name, value)
- total_kills, total_deaths, total_assists
- total_wins, total_matches_played
- total_headshot_kills, total_mvps
- total_rounds_played, total_damage_done
- total_wins_map_de_dust2 등 맵별 승리

## 참고

- Steam API는 CS2 최근 매치 상세를 직접 제공하지 않음
- GetUserStatsForGame로 누적 통계만 조회 가능
- 프로필이 비공개이면 403 반환

## application.properties

```
steam.api.key={API_KEY}
```

---

# 9. Blizzard (Battle.net)

Blizzard 공식 API  
문서: https://develop.battle.net/  
인증: OAuth2 Client Credentials Flow

지원 게임: Overwatch 2, Hearthstone, Diablo 등

## 9.1 Access Token 획득 (OAuth2)

Client Credentials 방식으로 Access Token을 발급합니다.

형식: `POST https://kr.battle.net/oauth/token` (또는 us.battle.net)

Body: `grant_type=client_credentials` (application/x-www-form-urlencoded)  
헤더: `Authorization: Basic {base64(client_id:client_secret)}`

예시: `POST https://kr.battle.net/oauth/token`

## 9.2 오버워치 2 플레이어 프로필

BattleTag로 오버워치 2 프로필을 조회합니다.

형식: `https://{region}.api.blizzard.com/ow/profile/{gameName}/{tagLine}`

예시: `https://kr.api.blizzard.com/ow/profile/Fleta/3852`

헤더: `Authorization: Bearer {access_token}`

응답: competitive (pc → tank, damage, support 티어) 등

## 참고

- Battle.net 개인정보 보호 정책상 상세 전적은 제한적
- 프로필 공개 설정이 필요함
- ow-api.com 비공식 API가 더 많은 데이터를 제공할 수 있음

## application.properties

```
blizzard.client.id={CLIENT_ID}
blizzard.client.secret={CLIENT_SECRET}
```

---
