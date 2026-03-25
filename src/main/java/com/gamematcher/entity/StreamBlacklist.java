package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 방송 블랙리스트. 등록된 사용자는 해당 방송 시청(입장) 불가.
 */
@Entity
@Table(name = "stream_blacklist", uniqueConstraints = @UniqueConstraint(columnNames = {"stream_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StreamBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_id", nullable = false)
    private Long streamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
