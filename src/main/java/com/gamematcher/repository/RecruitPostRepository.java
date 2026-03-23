package com.gamematcher.repository;

import com.gamematcher.entity.RecruitPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RecruitPostRepository extends JpaRepository<RecruitPost, Long>, JpaSpecificationExecutor<RecruitPost> {

    List<RecruitPost> findByGameOrderByCreatedAtDesc(String game);
}
