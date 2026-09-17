package com.cpclub.backend.halloffame.dto;

import com.cpclub.backend.halloffame.entity.HallOfFameEntry;
import com.cpclub.backend.halloffame.entity.HallOfFameLink;
import com.cpclub.backend.halloffame.entity.HallOfFamePhoto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A Hall of Fame entry as the public page renders it.
 *
 * <p>Carries nothing about who entered it. The page credits the people who
 * achieved something, not the admin who typed it up.</p>
 */
public record HallOfFameEntryDto(
        Long id,
        String heading,
        String subheading,
        String details,
        LocalDate achievedOn,
        List<LinkDto> links,
        List<PhotoDto> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** Call inside the transaction that loaded the entry: links and photos are lazy. */
    public static HallOfFameEntryDto fromEntity(HallOfFameEntry entry) {
        return new HallOfFameEntryDto(
                entry.getId(),
                entry.getHeading(),
                entry.getSubheading(),
                entry.getDetails(),
                entry.getAchievedOn(),
                entry.getLinks().stream().map(LinkDto::fromEntity).toList(),
                entry.getPhotos().stream().map(PhotoDto::fromEntity).toList(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    public record LinkDto(String label, String url) {
        static LinkDto fromEntity(HallOfFameLink link) {
            return new LinkDto(link.getLabel(), link.getUrl());
        }
    }

    public record PhotoDto(Long id, String imageUrl, String caption) {
        static PhotoDto fromEntity(HallOfFamePhoto photo) {
            return new PhotoDto(photo.getId(), photo.getImageUrl(), photo.getCaption());
        }
    }
}
