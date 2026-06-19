package com.torresj.community.repositories;

import com.torresj.community.entities.PropertyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<PropertyEntity, Long> {
    List<PropertyEntity> findByUserId(Long userId);

    List<PropertyEntity> findByCommunityId(Long communityId);
}
