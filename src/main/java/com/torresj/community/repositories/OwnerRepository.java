package com.torresj.community.repositories;

import com.torresj.community.entities.OwnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OwnerRepository extends JpaRepository<OwnerEntity, Long> {
    List<OwnerEntity> findByCommunityId(Long communityId);
}
