package com.gamematcher.constant.lol;

import java.util.Map;

/**
 * LoL 아이템 ID → 표시 이름(한글) 매핑.
 * 핵심 아이템 완성 시 LLM/사용자 가독성을 위해 사용.
 * Riot Data Dragon 기준 (패치별로 업데이트 필요).
 */
public final class LolItemNames {

    private static final Map<Integer, String> ITEM_NAMES = Map.ofEntries(
            Map.entry(2065, "샤엘의 불씨"),
            Map.entry(3003, "대천사의 포옹"),
            Map.entry(3036, "도미닉 경의 징표"),
            Map.entry(3074, "무한의 대검"),
            Map.entry(3078, "삼위일체"),
            Map.entry(3085, "루난의 허리케인"),
            Map.entry(3089, "라바돈의 죽음모자"),
            Map.entry(3100, "리치베인"),
            Map.entry(3116, "리안드리의 고통"),
            Map.entry(3124, "균형의 쐐기"),
            Map.entry(3153, "밴시의 장막"),
            Map.entry(3157, "존야의 모래시계"),
            Map.entry(3190, "영겁의 지팡이"),
            Map.entry(3748, "거대한 히드라"),
            Map.entry(6617, "빛나는 덕의 성배"),
            Map.entry(6662, "빙하의 손아귀"),
            Map.entry(6664, "악의"),
            Map.entry(6676, "공허의 광채"),
            Map.entry(6692, "갈라진 하늘"),
            Map.entry(6693, "탈락한 왕의 검"),
            Map.entry(6694, "불굴의 의지")
    );

    private LolItemNames() {
    }

    /**
     * 아이템 ID에 대한 표시 이름 반환. 매핑 없으면 "아이템ID {id}" 반환.
     */
    public static String getDisplayName(Integer itemId) {
        if (itemId == null) return "아이템ID ?";
        String name = ITEM_NAMES.get(itemId);
        return name != null ? "[" + name + "]" : "아이템ID " + itemId;
    }
}
