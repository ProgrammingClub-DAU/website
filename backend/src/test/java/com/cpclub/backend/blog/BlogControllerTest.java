package com.cpclub.backend.blog;

import com.cpclub.backend.blog.controller.BlogController;
import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.entity.BlogStatus;
import com.cpclub.backend.blog.service.BlogService;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.GlobalExceptionHandler;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routing and request handling. Security is covered by BlogAuthorizationTest. */
class BlogControllerTest {

    private MockMvc mockMvc;
    private BlogService blogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        blogService = mock(BlogService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BlogController(blogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("The list passes the requested page through")
    void getPublishedBlogs_returnsRequestedPage() throws Exception {
        when(blogService.getPublishedBlogs(1, 5))
                .thenReturn(new PagedResponse<>(List.of(response(4L, "Graphs", "graphs")), 1, 5, 6, 2, true));

        mockMvc.perform(get("/api/blogs").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].slug").value("graphs"))
                .andExpect(jsonPath("$.data.content[0].readMinutes").value(1));
    }

    @Test
    @DisplayName("By id and by slug, a missing post is a 404")
    void getById_andSlug() throws Exception {
        when(blogService.getBlogById(4L)).thenReturn(response(4L, "Graphs", "graphs"));
        when(blogService.getBlogBySlug("graphs")).thenReturn(response(4L, "Graphs", "graphs"));
        when(blogService.getBlogById(99L)).thenThrow(new ResourceNotFoundException("Blog post not found with id: 99"));

        mockMvc.perform(get("/api/blogs/4")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(4));
        mockMvc.perform(get("/api/blogs/slug/graphs")).andExpect(status().isOk());
        mockMvc.perform(get("/api/blogs/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/mine and /review-queue are never read as a post id")
    void namedRoutesAreNotIds() throws Exception {
        when(blogService.mine(any())).thenReturn(List.of());
        when(blogService.reviewQueue(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/blogs/mine")).andExpect(status().isOk());
        mockMvc.perform(get("/api/blogs/review-queue")).andExpect(status().isOk());
        verify(blogService, never()).getBlogById(any());
    }

    @Test
    @DisplayName("Creating returns 201; missing fields are a 400 before the service is called")
    void create() throws Exception {
        when(blogService.create(any(BlogCreateRequest.class), any())).thenReturn(response(7L, "Graphs", "graphs"));

        mockMvc.perform(post("/api/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BlogCreateRequest("Graphs", "Content", null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(7));

        mockMvc.perform(post("/api/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Rejecting without a note fails validation")
    void rejectNeedsANote() throws Exception {
        mockMvc.perform(post("/api/blogs/3/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(blogService, never()).reject(eq(3L), any(), any());
    }

    private static BlogResponseDto response(Long id, String title, String slug) {
        LocalDateTime t = LocalDateTime.of(2026, 10, 1, 10, 0);
        return new BlogResponseDto(id, title, slug, "content", "Alice", null, true, t, t,
                BlogStatus.PUBLISHED, 1L, t, 0, 1);
    }
}
