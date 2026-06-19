package com.torresj.community.entities;

import com.torresj.community.enums.VotingResult;
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

import java.util.List;

/**
 * The voting result for an agenda item. Real actas vary: sometimes only aggregate counts are
 * recorded ("15/1/9"), sometimes "por unanimidad", sometimes a full per-property breakdown.
 * All of those are representable here — counts and breakdown lists are optional.
 */
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class VotingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column
    private Integer inFavorCount;

    @Column
    private Integer againstCount;

    @Column
    private Integer abstentionCount;

    @Column
    private VotingResult result;

    @Column
    private boolean unanimous;

    @ElementCollection
    @Column
    private List<Long> listOfYes;

    @ElementCollection
    @Column
    private List<Long> listOfNo;

    @ElementCollection
    @Column
    private List<Long> abstentions;
}
