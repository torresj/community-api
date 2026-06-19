package com.torresj.community.repositories;

import com.torresj.community.entities.MembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<MembershipEntity, Long> {
    List<MembershipEntity> findByUserId(Long userId);

    List<MembershipEntity> findByCommunityId(Long communityId);

    Optional<MembershipEntity> findByUserIdAndCommunityId(Long userId, Long communityId);
}
