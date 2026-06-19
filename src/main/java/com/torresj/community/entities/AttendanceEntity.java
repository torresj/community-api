package com.torresj.community.entities;

import com.torresj.community.enums.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Attendance of a property at a meeting, when the minutes record that detail. A property may be
 * PRESENT, REPRESENTED by a proxy (free-text name, e.g. "D. Bruno Ortega"), or ABSENT.
 */
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class AttendanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reportId;

    @Column(nullable = false)
    private Long propertyId;

    @Column(nullable = false)
    private AttendanceStatus status;

    @Column
    private String representedBy;
}
