package com.cpclub.backend.blog;

import com.cpclub.backend.blog.controller.BlogController;
import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.dto.BlogUpdateRequest;
import com.cpclub.backend.blog.service.BlogService;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.GlobalExceptionHandler;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void getPublishedBlogs_returnsRequestedPage() throws Exception {
        BlogResponseDto post = response(1L, "Dynamic Programming", "dynamic-programming");
        when(blogService.getPublishedBlogs(1, 5))
                .thenReturn(new PagedResponse<>(List.of(post), 1, 5, 6, 2, true));

        mockMvc.perform(get("/api/blogs").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.content[0].slug").value("dynamic-programming"));
    }

    @Test
    void getBlogById_andSlug_returnPost_orNotFound() throws Exception {
        BlogResponseDto post = response(4L, "Graphs", "graphs");
        when(blogService.getBlogById(4L)).thenReturn(post);
        when(blogService.getBlogBySlug("graphs")).thenReturn(post);
        when(blogService.getBlogById(99L)).thenThrow(new ResourceNotFoundException("Blog post not found with id: 99"));

        mockMvc.perform(get("/api/blogs/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(4));
        mockMvc.perform(get("/api/blogs/slug/graphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Graphs"));
        mockMvc.perform(get("/api/blogs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createBlog_returnsCreated_andRejectsMissingRequiredFields() throws Exception {
        BlogCreateRequest request = new BlogCreateRequest("Graphs", "Content", "Admin", null, true);
        when(blogService.createBlog(any(BlogCreateRequest.class), any())).thenReturn(response(7L, "Graphs", "graphs"));

        mockMvc.perform(post("/api/blogs")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(7));
        mockMvc.perform(post("/api/blogs")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void updateAndDeleteBlog_delegateToService() throws Exception {
        BlogUpdateRequest request = new BlogUpdateRequest("Updated", "Updated content", "graphs", false);
        when(blogService.updateBlog(eq(3L), any(BlogUpdateRequest.class))).thenReturn(response(3L, "Updated", "updated"));
        doNothing().when(blogService).deleteBlog(3L);
        doThrow(new ResourceNotFoundException("Blog post not found with id: 99")).when(blogService).deleteBlog(99L);

        mockMvc.perform(put("/api/blogs/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
        mockMvc.perform(delete("/api/blogs/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(delete("/api/blogs/99"))
                .andExpect(status().isNotFound());
    }

    private BlogResponseDto response(Long id, String title, String slug) {
        return new BlogResponseDto(id, title, slug, "Article content", "Admin", "algorithms", true, null, null);
    }
}
