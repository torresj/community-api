package com.torresj.community.entities;

import com.torresj.community.enums.CommunityRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Many-to-many link between a {@link UserEntity} and a {@link CommunityEntity}. The
 * per-community {@link CommunityRole} decides whether the user merely belongs to (MEMBER) or
 * administers (ADMIN) that community. System-wide SUPERADMIN is on {@code UserEntity.role}.
 */
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
public class MembershipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private Long communityId;

    @Column(nullable = false)
    private CommunityRole role;
}
