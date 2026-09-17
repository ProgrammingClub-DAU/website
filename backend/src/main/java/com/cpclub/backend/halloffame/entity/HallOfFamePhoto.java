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
 * A photograph attached to a Hall of Fame entry.
 *
 * <p>Stored as a URL only; the file lives on Cloudinary, uploaded from the
 * browser. These also appear in the public gallery, beside event photos.</p>
 */
@Entity
@Table(name = "hall_of_fame_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallOfFamePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private HallOfFameEntry entry;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(length = 300)
    private String caption;

    @Column(nullable = false)
    private Integer position;
}
