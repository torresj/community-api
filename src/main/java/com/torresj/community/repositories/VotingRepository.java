package com.torresj.community.repositories;

import com.torresj.community.entities.VotingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VotingRepository extends JpaRepository<VotingEntity, Long> {
}
