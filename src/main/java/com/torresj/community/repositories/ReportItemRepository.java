package com.torresj.community.repositories;

import com.torresj.community.entities.ReportItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportItemRepository extends JpaRepository<ReportItemEntity, Long> {}
