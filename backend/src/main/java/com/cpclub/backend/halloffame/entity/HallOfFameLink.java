package com.cpclub.backend.halloffame.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A link attached to a Hall of Fame entry -- a ranklist, an editorial, a news post.
 *
 * <p>The URL is checked to be http or https before it gets here (see
 * {@code HallOfFameEntryRequest}). It ends up in an {@code href}, and a
 * {@code javascript:} URL there runs in the visitor's browser when clicked.</p>
 */
@Entity
@Table(name = "hall_of_fame_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallOfFameLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private HallOfFameEntry entry;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(nullable = false)
    private Integer position;
}
