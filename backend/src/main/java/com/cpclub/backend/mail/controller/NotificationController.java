package com.cpclub.backend.mail.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.mail.dto.UnsubscribeResultDto;
import com.cpclub.backend.mail.service.UnsubscribeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Email unsubscribe links. Public by design: the signed token is the credential.
 *
 * <p>POST exists for RFC 8058 one-click unsubscribe, where the mail client posts
 * {@code List-Unsubscribe=One-Click} to the URL in the List-Unsubscribe header.
 * GET serves the link in the email body, through the site's unsubscribe page.</p>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Email preference unsubscribe links")
public class NotificationController {

    private final UnsubscribeService unsubscribeService;

    @RequestMapping(value = "/unsubscribe", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Turn off one email preference from a signed link")
    public ResponseEntity<ApiResponse<UnsubscribeResultDto>> unsubscribe(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(unsubscribeService.unsubscribe(token), "Unsubscribed"));
    }
}
