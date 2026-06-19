package com.torresj.community.entities;

import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.enums.MeetingType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** A board meeting ("junta"). Holds both the scheduled metadata and, once HELD, the acta. */
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(updatable = false, nullable = false)
    private Long communityId;

    @Column
    private LocalDateTime dateTime;

    @Column
    private MeetingType type;

    @Column(nullable = false)
    private MeetingStatus status;

    @Column
    private String title;

    @Column
    private String location;

    @Column
    private Integer convocatoria;

    @Column
    private String presidentName;

    @Column
    private String secretaryName;

    /** Relative path to the stored original signed PDF (content lives on the volume, not the DB). */
    @Column
    private String pdfPath;

    @ElementCollection
    @Column
    private List<Long> attendeesPropertyIds;
}
