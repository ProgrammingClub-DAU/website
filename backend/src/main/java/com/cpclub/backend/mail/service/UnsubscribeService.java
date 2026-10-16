package com.cpclub.backend.mail.service;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.mail.dto.UnsubscribeResultDto;
import com.cpclub.backend.mail.service.UnsubscribeTokenService.Claim;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns one email preference off from a signed link, without signing in. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnsubscribeService {

    private final UnsubscribeTokenService tokens;
    private final UserRepository userRepository;

    /**
     * Idempotent: unsubscribing twice, or for an account that has since been
     * deleted, still answers with the kind -- the link did what it says.
     */
    @Transactional
    public UnsubscribeResultDto unsubscribe(String token) {
        Claim claim = tokens.verify(token)
                .orElseThrow(() -> new BadRequestException("This unsubscribe link is invalid"));
        userRepository.findById(claim.userId()).ifPresent(user -> {
            switch (claim.kind()) {
                case EVENTS -> user.setNotifyEvents(false);
                case CONTESTS -> user.setNotifyContests(false);
            }
            userRepository.save(user);
            log.info("User {} unsubscribed from {} emails", user.getId(), claim.kind());
        });
        return new UnsubscribeResultDto(claim.kind().name());
    }
}
