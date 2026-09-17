package com.cpclub.backend.halloffame.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Creates or replaces a Hall of Fame entry, including its links and photos.
 *
 * <p>The whole entry is sent on every save. Links and photos are ordered lists
 * the admin edits as a unit, so a partial update would need its own rules for
 * reordering and removal; replacing the lists is simpler and cannot leave an
 * entry half-edited.</p>
 *
 * @param heading the achievement, e.g. "ICPC Amritapuri Regional -- Rank 42"
 * @param subheading optional second line, e.g. the team or the people
 * @param details optional write-up
 * @param achievedOn the day it happened; the page groups and orders by this
 * @param links optional, at most ten; null is treated as none
 * @param photos optional, at most thirty; null is treated as none
 */
public record HallOfFameEntryRequest(

        @NotBlank(message = "Heading is required")
        @Size(max = 200, message = "Heading must be at most 200 characters")
        String heading,

        @Size(max = 300, message = "Subheading must be at most 300 characters")
        String subheading,

        @Size(max = 5000, message = "Details must be at most 5000 characters")
        String details,

        @NotNull(message = "Date is required")
        LocalDate achievedOn,

        @Size(max = 10, message = "An entry can have at most 10 links")
        List<@Valid Link> links,

        @Size(max = 30, message = "An entry can have at most 30 photos")
        List<@Valid Photo> photos
) {

    /**
     * An http or https URL, and nothing else.
     *
     * <p>These strings are rendered into {@code href} and {@code src} attributes.
     * A {@code javascript:} or {@code data:} URL in an {@code href} executes in the
     * visitor's browser when clicked, and admin accounts are exactly the accounts
     * worth compromising to plant one. React does not strip them. So the scheme is
     * checked here, at the only place data enters.</p>
     */
    static final String WEB_URL = "^https?://\\S+$";

    public List<Link> linksOrEmpty() {
        return links == null ? List.of() : links;
    }

    public List<Photo> photosOrEmpty() {
        return photos == null ? List.of() : photos;
    }

    /**
     * @param label what the link is, shown as the chip text
     * @param url where it goes
     */
    public record Link(

            @NotBlank(message = "Every link needs a label")
            @Size(max = 100, message = "Link labels must be at most 100 characters")
            String label,

            @NotBlank(message = "Every link needs a URL")
            @Size(max = 512, message = "Link URLs must be at most 512 characters")
            @Pattern(regexp = WEB_URL, message = "Links must start with http:// or https://")
            String url
    ) {
    }

    /**
     * @param imageUrl the uploaded image
     * @param caption optional, shown under the photo and in the gallery
     */
    public record Photo(

            @NotBlank(message = "Every photo needs an image URL")
            @Size(max = 512, message = "Image URLs must be at most 512 characters")
            @Pattern(regexp = WEB_URL, message = "Image URLs must start with http:// or https://")
            String imageUrl,

            @Size(max = 300, message = "Captions must be at most 300 characters")
            String caption
    ) {
    }
}
