package com.gamematcher.service;

import com.gamematcher.dto.recruit.RecruitPostDto;
import com.gamematcher.entity.RecruitPost;
import com.gamematcher.repository.RecruitPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitPostService {

    private final RecruitPostRepository recruitPostRepository;

    public List<RecruitPostDto> listByGame(String game) {
        if (game == null || game.isBlank() || "ALL".equalsIgnoreCase(game)) {
            return recruitPostRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return recruitPostRepository.findByGameOrderByCreatedAtDesc(game).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** 게임 + 티어/포지션/리전/모드 필터 지원 */
    public List<RecruitPostDto> listWithFilters(String game, String tier, String mainPosition,
                                                String findPosition, String region, String mode) {
        Specification<RecruitPost> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (game != null && !game.isBlank() && !"ALL".equalsIgnoreCase(game)) {
                predicates.add(cb.equal(root.get("game"), game));
            }
            if (tier != null && !tier.isBlank()) {
                predicates.add(cb.equal(root.get("tier"), tier));
            }
            if (mainPosition != null && !mainPosition.isBlank()) {
                predicates.add(cb.equal(root.get("mainPosition"), mainPosition));
            }
            if (findPosition != null && !findPosition.isBlank()) {
                predicates.add(cb.equal(root.get("findPosition"), findPosition));
            }
            if (region != null && !region.isBlank()) {
                predicates.add(cb.equal(root.get("region"), region));
            }
            if (mode != null && !mode.isBlank()) {
                predicates.add(cb.equal(root.get("mode"), mode));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return recruitPostRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecruitPostDto create(Long userId, String game, String summonerName,
                                 String mainPosition, String findPosition, String tier,
                                 String region, String mode, String memo) {
        if (userId == null) return null;
        if (game == null || game.isBlank()) game = "LEAGUE_OF_LEGENDS";
        if (summonerName == null || summonerName.isBlank()) return null;

        RecruitPost post = new RecruitPost();
        post.setUserId(userId);
        post.setGame(game);
        post.setSummonerName(summonerName.trim());
        post.setMainPosition(mainPosition != null ? mainPosition.trim() : null);
        post.setFindPosition(findPosition != null ? findPosition.trim() : null);
        post.setTier(tier != null ? tier.trim() : null);
        post.setRegion(region != null ? region.trim() : null);
        post.setMode(mode != null ? mode.trim() : null);
        post.setMemo(memo != null ? memo.trim() : null);
        recruitPostRepository.save(post);
        return toDto(post);
    }

    private RecruitPostDto toDto(RecruitPost p) {
        return RecruitPostDto.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .game(p.getGame())
                .summonerName(p.getSummonerName())
                .mainPosition(p.getMainPosition())
                .findPosition(p.getFindPosition())
                .tier(p.getTier())
                .region(p.getRegion())
                .mode(p.getMode())
                .memo(p.getMemo())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
