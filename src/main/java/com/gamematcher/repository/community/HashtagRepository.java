package com.gamematcher.repository.community;

import com.gamematcher.entity.community.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    Optional<Hashtag> findByName(String name);

    List<Hashtag> findByNameIn(List<String> names);
}
