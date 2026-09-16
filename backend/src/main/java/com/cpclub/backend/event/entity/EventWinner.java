package com.cpclub.backend.event.entity;

import com.cpclub.backend.user.entity.User;
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
 * One placing on an event's podium.
 *
 * <p>A winner is a {@link User}, not a name. The club's winners are members with
 * profiles on this site, so the public event page can link straight to the
 * person, their rating and their handles -- and a member who later corrects the
 * spelling of their name does not leave a stale copy of it in an old event.</p>
 *
 * <p>The database enforces both that a placing is 1, 2 or 3 and that an event
 * has at most one member per placing and one placing per member (V10). Those are
 * checked in {@code EventService} too, so a mis-click gets a readable message
 * rather than a constraint violation, but the database is what makes them true.</p>
 */
@Entity
@Table(name = "event_winners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lazy: the podium is read through the event, which is already loaded. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * The member who placed.
     *
     * <p>Eager, unlike most associations here. Every read of a winner exists to
     * render that member -- their name, avatar and handle -- so a lazy load would
     * be a guaranteed second query immediately afterwards, three times per
     * event.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 1, 2 or 3. */
    @Column(nullable = false)
    private Integer position;
}
