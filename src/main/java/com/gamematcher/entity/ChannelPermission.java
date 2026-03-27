package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_user_id", "manager_user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ChannelPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "manager_user_id", nullable = false)
    private Long managerUserId;

    @Column(name = "role_name", nullable = false, length = 30)
    private String roleName;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
