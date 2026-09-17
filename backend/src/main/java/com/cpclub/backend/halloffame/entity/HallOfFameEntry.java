package com.cpclub.backend.halloffame.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One achievement in the club's Hall of Fame.
 *
 * <p>Links and photos are owned outright: they are created, reordered and removed
 * only by saving the entry, so they cascade and orphans are deleted. An admin
 * editing an entry sends the whole list back and the old rows go.</p>
 *
 * <p>Both collections carry {@link BatchSize}. The public page renders every
 * entry with its links and photos, and without batching that is two extra
 * queries per entry. Fetch-joining both instead is not an option -- Hibernate
 * refuses to join-fetch two ordered lists at once -- so the lazy loads are
 * grouped into a query per collection for the whole page.</p>
 */
@Entity
@Table(name = "hall_of_fame_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallOfFameEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String heading;

    @Column(length = 300)
    private String subheading;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "achieved_on", nullable = false)
    private LocalDate achievedOn;

    /** Nullable: the record outlives the admin account that entered it (V12). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<HallOfFameLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<HallOfFamePhoto> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
