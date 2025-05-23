package com.torresj.community.entities;

import com.torresj.community.enums.PropertyCodeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column
    private Long ownerId;

    @Column(nullable = false, updatable = false)
    private Long communityId;

    @Column(nullable = false, updatable = false)
    private PropertyCodeEnum code;

    @Column(nullable = false,updatable = false)
    private double coefficient;

    @Column
    private String description;
}
