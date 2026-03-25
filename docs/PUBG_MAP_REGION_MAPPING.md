# PUBG 좌표 → 지역 변환 가이드

> "강남 지역", "능선 아래" 같은 의미 텍스트를 만들려면 인게임 좌표 `(x, y, z)`를 지역명/지형 정보로 변환해야 합니다.
> 본 문서는 **좌표→지역 매핑 데이터를 어떻게 제작·구축할지**에 대한 가이드입니다.

---

## 1. PUBG 좌표 체계

| 항목 | 설명 |
|------|------|
| **단위** | Unreal Engine 월드 좌표. 텔레메트리에 따라 **cm**일 수 있음 → m 변환(÷100) 확인 필요 |
| **맵 크기** | 에란겔 8km×8km (8000m), 미라마 등 맵별 상이 |
| **텔레메트리** | `character.location` → `{ x, y, z }` (예: x: 274570, y: 234572, z: 732) |
| **그리드** | 인게임 미니맵 8×8 (A1~H8 또는 AA~HH 등) |

맵별로 좌표 범위가 다르므로, **맵 ID**를 함께 고려해야 합니다.

---

## 2. 제작 방식 (3가지)

### 2.1 그리드 기반 (Grid-based) — 권장 시작점

**개요**: 맵을 N×N 그리드로 나누고, 각 셀에 지역명을 부여.

```
에란겔 8×8 그리드 예시:
  A1  A2  A3  A4  A5  A6  A7  A8
  B1  B2  B3  B4  B5  B6  B7  B8
  ...
  H1  H2  H3  H4  H5  H6  H7  H8
```

**데이터 구조** (JSON):

```json
{
  "erangel": {
    "width": 800000,
    "height": 800000,
    "originX": 0,
    "originY": 0,
    "gridCols": 8,
    "gridRows": 8,
    "regions": [
      { "cell": "A1", "name": "Sosnovka Military Base", "nameKo": "군사기지" },
      { "cell": "B2", "name": "Georgopol", "nameKo": "게오르고폴" },
      { "cell": "D4", "name": "Pochinki", "nameKo": "포친키" },
      { "cell": "E5", "name": "Rozhok", "nameKo": "로조크" }
    ]
  }
}
```

**변환 로직**:
1. `(x, y)` → 그리드 셀 인덱스 계산
2. 셀 → `regions`에서 `name` 또는 `nameKo` 조회
3. 매칭 없으면 `"그리드 D4"` 같은 fallback

**장점**: 구현 단순, 데이터 양 적음  
**단점**: 8×8 그리드는 셀당 약 1km×1km로 "강남", "능선" 수준의 세밀한 묘사에 부족. 셀 경계 부정확, 소규모 지역 구분 어려움

---

### 2.2 POI 원형(Circle) 기반 — 폴리곤 대안

**개요**: 폴리곤 제작이 부담스러우면, 주요 도시의 **중심점 + 반경**으로 판별. 폴리곤보다 가볍고 "포친키 중심부에서 북서쪽 100m" 같은 구체적 표현 가능.

| 항목 | 설명 |
|------|------|
| **구조** | `center: [x, y]`, `radius: 250` (m) |
| **로직** | `distance(point, center) <= radius` → POI 내부. 초과 시 거리·방위 계산 |
| **우선순위** | 여러 POI에 걸치면 반경 작은 쪽 또는 우선순위 높은 쪽 선택 |

---

### 2.3 폴리곤 기반 (Polygon-based) — 정밀도 높음

**개요**: 각 지역을 다각형(polygon)으로 정의하고, 점이 어느 폴리곤 안에 있는지 판별.

**데이터 구조** (GeoJSON 스타일):

```json
{
  "erangel": {
    "regions": [
      {
        "id": "pochinki",
        "name": "Pochinki",
        "nameKo": "포친키",
        "polygon": [[x1,y1], [x2,y2], [x3,y3], ...]
      }
    ]
  }
}
```

**변환 로직**: Point-in-Polygon 알고리즘 (Ray casting 등)으로 `(x, y)`가 속한 region 탐색.

**장점**: 지역 경계 정확, 불규칙한 형태 지원  
**단점**: 폴리곤 데이터 제작 비용 큼, 연산 비용 증가

---

### 2.4 하이브리드 (POI 중심 + 그리드 Fallback) — **권장**

**개요**: 주요 교전 지역(도시, 군사기지 등)은 POI로 정밀하게, 나머지는 그리드로 처리. "POI 이름 + 방향 + 거리" 조합만으로도 AI가 충분한 분석 가능.

- **POI 우선**: 도시·마을 등 이름 있는 지역. `type`(urban, mountain, coastal)으로 지형 특성 전달
- **그리드 Fallback**: POI 미매칭 시 그리드 + 대표 지형명 (예: "황무지 (그리드 C3)")

---

## 3. 데이터 수집 방법

### 3.1 데이터 출처 (공식 우선)

| 출처 | 내용 | 라이선스 |
|------|------|----------|
| [pubg/api-assets](https://github.com/pubg/api-assets) | 맵명, 텔레메트리 사전 (mapName.json 등) | PUBG 공식, API 개발자용 |
| [PUBG Developer Portal](https://developer.pubg.com/) | API 이용약관 | ToS 준수 필요 |

**공식 리소스에 없는 그리드↔지역 매핑**은 다음을 참고할 수 있으나, **각 출처의 라이선스 확인 후** 사용할 것:

| 출처 | 활용 | 비고 |
|------|------|------|
| [PUBG Wiki (Fandom)](https://pubg.fandom.com/wiki/Erangel) | 그리드 셀 참고 | CC BY-SA 등 위키별 라이선스 확인, 출처 표기 필요 |
| [Battlegrounds.party](https://battlegrounds.party/map/) | 좌표 샘플 | 해당 사이트 이용약관 확인 |

### 3.2 빈 슬롯 (미정 슬롯) — 다이나믹 월드

**파라모(Paramo)** 등 **다이나믹 월드**가 적용된 맵은 매치마다 지형·POI 배치가 달라질 수 있어, 모든 셀 명칭을 미리 채우기 어렵습니다.

**규칙**:

| 값 | 의미 | PubgMapRegionMapper fallback |
|----|------|------------------------------|
| `"슬롯"` | 명칭 미정, 나중에 채울 예정 | `"그리드 D4"` 등 |
| `""` (빈 문자열) | 동일 | 동일 |
| `null` | 동일 | 동일 |
| 키 생략 | 해당 슬롯 미정의 | 동일 |

**예시** (`paramo_regions.json`):

```json
{
  "cells": {
    "C3": "Thermal",
    "D4": "슬롯",
    "E4": "슬롯"
  },
  "grid": {
    "names": {
      "D4": "슬롯",
      "E5": "슬롯"
    }
  }
}
```

- `"Thermal"`: 확정된 지역
- `"슬롯"`: 명칭 미정, 나중에 채울 예정
- `PubgMapRegionMapper` 구현 시: `cells.get("D4")`가 `null` 또는 `""`이면 `"그리드 D4"` fallback 반환

### 3.3 수동 제작 절차

> **라이선스**: 맵명·용어는 [pubg/api-assets](https://github.com/pubg/api-assets) 등 공식 리소스를 우선 사용. 커뮤니티 자료 활용 시 해당 출처의 라이선스와 이용약관을 확인할 것.

1. **그리드 좌표 범위 확인**
   - 텔레메트리 샘플에서 `(x, y)` min/max 확인
   - 또는 PUBG 문서/위키에서 맵별 좌표 범위 검색

2. **지역명 매핑**
   - PUBG 위키에서 에란겔/미라마 등 지역 목록 수집
   - 각 지역이 어느 그리드 셀에 해당하는지 표 작성 (위키에 "BJ, BK" 등 기재된 경우 활용)

3. **JSON/YAML로 정리**
   - `src/main/resources/maps/erangel_regions.json` 등에 저장
   - 맵 추가 시 `miramar_regions.json` 등 확장

### 3.4 Z값(수직성) 분류

**주의**: 맵·지역마다 바닥(지면) Z가 다릅니다. 군사기지 고지대와 포친키 평지의 "1층" Z가 다르므로, **전역 고정 구간**(예: z < 500 → 지면)은 부적절합니다.

| 방식 | 설명 | 한계 |
|------|------|------|
| **지역별 기준선** | 그리드 셀마다 `groundZ`(지면 기준) 정의 후, `z - groundZ`로 상대 높이 계산 | 데이터 제작 비용 큼 |
| **매치 내 상대 비교** | 같은 매치의 위치 샘플에서 해당 구역 min Z를 "지면"으로 추정 | 실시간 계산 필요 |
| **Z 생략** | 수직성 태그 없이 지역명만 사용 | "옥상 vs 1층" 구분 불가 |

**권장**: 초기에는 Z 기반 수직성 태그를 생략하고 지역명만 사용. 정밀도가 필요해지면 지역별 `groundZ`를 수동 정의하거나, 매치 단위 상대 높이 추정을 검토.

---

## 4. 구현 예시 (그리드 기반)

### 4.1 PubgMapRegionMapper 인터페이스

```java
public interface PubgMapRegionMapper {
    /**
     * @param mapName "Erangel", "Miramar" 등
     * @param x location.x
     * @param y location.y
     * @param z location.z (수직성 태그용)
     */
    String toRegionName(String mapName, double x, double y, double z);
    // 반환: "Pochinki 건물 옥상" 또는 "Georgopol" 등
}
```

### 4.2 리소스 파일

맵별로 별도 JSON. `mapId`는 텔레메트리 `mapName`(예: Erangel_Main)과 매칭.

```
src/main/resources/maps/
├── pubg_map_name.json        # 맵 ID → 표시명 (공식 api-assets)
├── erangel_regions.json      # Erangel_Main, Baltic_Main
├── miramar_regions.json      # Desert_Main
├── vikendi_regions.json      # DihorOtok_Main
├── sanhok_regions.json       # Savage_Main
├── karakin_regions.json      # Summerland_Main
├── taego_regions.json        # Tiger_Main
├── deston_regions.json       # Kiki_Main
├── paramo_regions.json       # Chimera_Main
├── haven_regions.json        # Heaven_Main
├── rondo_regions.json        # Neon_Main
└── ...
```

예시 `erangel_regions.json` (그리드 + POI 통합):

```json
{
  "mapIds": ["Erangel_Main", "Baltic_Main"],
  "mapName": "Erangel",
  "bounds": { "minX": 0, "maxX": 800000, "minY": 0, "maxY": 800000 },
  "gridCols": 8,
  "gridRows": 8,
  "cells": {
    "D4": "Pochinki",
    "D5": "Rozhok",
    "B2": "Georgopol",
    "A1": "Sosnovka Military Base"
  },
  "pois": [
    {
      "id": "pochinki",
      "nameKo": "포친키",
      "type": "urban",
      "center": [453000, 382000],
      "radius": 300,
      "nearRadius": 500
    },
    {
      "id": "rozhok",
      "nameKo": "로조크",
      "type": "urban",
      "center": [412000, 312000],
      "radius": 200,
      "nearRadius": 500
    }
  ],
  "grid": {
    "names": {
      "D4": "포친키 인근 평야",
      "E4": "농장 지대",
      "C3": "쿼리 서쪽 황무지"
    }
  }
}
```

> **참고**: `center` 좌표는 예시. 텔레메트리 샘플 또는 커뮤니티 자료로 검증 필요.

**POI type**: `urban`(시가지), `mountain`(산악), `coastal`(해안) 등으로 AI가 지형 특성 인지.

**변환 예시**: `(x, y)`가 POI 반경 내 → `"포친키 중심부"`. `nearRadius`(500m) 이내 → `"포친키 남서쪽 150m 지점"`. 그 외 → `"쿼리 서쪽 황무지 (그리드 C3)"`.

미구현 맵은 `mapIds`로 조회 시 fallback → `"그리드 X"` 또는 맵명만 반환.

### 4.3 변환 로직 (의사코드)

```
function toRegionName(mapName, x, y, z):
  x, y = ensureMeters(x, y)  // cm → m 변환 필요 시 ÷100
  config = loadMapConfig(mapName)
  nearest = findNearestPoi(config.pois, x, y)
  if nearest:
    dist = distance([x,y], nearest.center)
    if dist <= nearest.radius: return nearest.nameKo + " 중심부"
    if dist <= nearest.nearRadius: return nearest.nameKo + " " + toDirection(x,y,nearest.center) + " " + round(dist) + "m 지점"
  // Fallback: 그리드 + 지형명
  cellKey = toCellKey(x, y, config)
  terrainName = config.grid?.names?.[cellKey] ?? config.cells?.[cellKey] ?? ""
  return (terrainName ? terrainName + " " : "") + "(그리드 " + cellKey + ")"
```

**방위 계산**: `atan2(dy, dx)`로 8방위(동, 서, 남, 북, 동남, 서남 등) 텍스트 변환.

### 4.4 동적 오브젝트 (자기장, 보급상자)

지역명만으로는 부족. **자기장(Blue Zone), 보급상자**와의 상대적 거리가 AI 분석에 중요.

| 데이터 | 출처 | 활용 |
|--------|------|------|
| 자기장 안팎 | `pubg_telemetry_position.is_in_bluezone` | "자기장 밖 120m" |
| 자기장 중심/반경 | `pubg_telemetry_match.safe_zone_*` | 거리 계산 |
| 보급상자 | LogCarePackageLand 등 | (선택) "보급상자 50m" |

**프롬프트 삽입 예시**: `"자기장 경계선으로부터 50m 안쪽, 포친키 동쪽 건물군. 현재 위치: 포친키 남쪽 외곽 45m"`

### 4.5 Z값 상대 비교 (Phase 2~3)

주변 위치 샘플의 평균 Z와 비교:
- **z > 주변 평균**: `"능선 위"`, `"고지대"`
- **z < 주변 평균**: `"구덩이"`, `"저지대"`

### 4.6 지형지물 (Phase 3, 선택)

텔레메트리의 `isWater`, `isRoad` 또는 별도 메타데이터로 "도강 중 기습", "도로 위 이동 중" 등 상황 판단. 데이터 구축 비용이 크므로 후순위.

---

## 5. 실행 로드맵

| Phase | 내용 |
|-------|------|
| **Phase 1 (MVP)** | 공식 api-assets + 8×8 그리드. 주요 POI 15~20개 `center`, `radius`만 먼저 입력 |
| **Phase 2** | 상대 위치 텍스트: "자기장 50m 안쪽, 포친키 동쪽". Z값 주변 평균 대비 "능선 위/저지대" |
| **Phase 3** | `isWater`, `isRoad` 등 지형지물 태깅. "도강 중", "도로 위" 상황 문장 |

---

## 6. 체크리스트 및 팁

- [ ] **단위 확인**: 텔레메트리 좌표가 cm인지 m인지 확인. cm면 ÷100 적용
- [ ] **방위각**: `atan2(dy, dx)`로 8방위 변환
- [ ] Phase 1: 에란겔 `erangel_regions.json` (그리드 + POI 15~20개) 작성
- [ ] Phase 2: 자기장 거리 + 상대 Z ("능선 위/저지대") 결합
- [ ] `PubgMapRegionMapper` 구현 (POI → nearRadius → 그리드 fallback)
- [ ] **팁**: 폴리곤 없이 "POI 이름 + 방향 + 거리"만으로도 AI 분석에 충분
