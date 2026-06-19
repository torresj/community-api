package com.torresj.community.entities;

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
public class PropertyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column
    private Long userId;

    @Column(nullable = false, updatable = false)
    private Long communityId;

    /**
     * Free-text unit code as it appears in the minutes (e.g. "P.1-1ºA,AP.TR.7"). Differs per
     * building/community, so it is a String rather than an enum.
     */
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private double coefficient;

    @Column
    private String description;
}
