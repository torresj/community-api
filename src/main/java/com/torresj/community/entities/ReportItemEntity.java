package com.torresj.community.entities;

import com.torresj.community.enums.ItemTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ReportItemEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(updatable = false, nullable = false)
    private Long reportId;

    @Column(nullable = false)
    private String description;

    @Column
    private String notes;

    @Column(nullable = false)
    private ItemTypeEnum type;

    @Column
    private Long votingId;
}
