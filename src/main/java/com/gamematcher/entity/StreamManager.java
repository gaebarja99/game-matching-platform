package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 방송 매니저. 스트리머가 지정하며, 채팅금지/블랙리스트 등 관리 권한 보유. 얼리기 시 채팅 가능.
 */
@Entity
@Table(name = "stream_managers", uniqueConstraints = @UniqueConstraint(columnNames = {"stream_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StreamManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_id", nullable = false)
    private Long streamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
