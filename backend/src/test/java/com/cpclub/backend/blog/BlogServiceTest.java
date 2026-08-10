package com.cpclub.backend.blog;

import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.repository.BlogRepository;
import com.cpclub.backend.blog.service.BlogService;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository blogRepository;

    @InjectMocks
    private BlogService blogService;

    private BlogPost samplePost;

    @BeforeEach
    void setUp() {
        samplePost = BlogPost.builder()
                .id(1L)
                .title("Introduction to Dynamic Programming")
                .slug("introduction-to-dynamic-programming")
                .content("DP is an algorithmic technique...")
                .authorName("Admin")
                .published(true)
                .build();
    }

    @Test
    @DisplayName("Should fetch published blogs paginated")
    void getPublishedBlogs_Success() {
        Page<BlogPost> page = new PageImpl<>(List.of(samplePost));
        when(blogRepository.findByPublishedTrue(any(Pageable.class))).thenReturn(page);

        PagedResponse<BlogResponseDto> response = blogService.getPublishedBlogs(0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Introduction to Dynamic Programming", response.content().get(0).title());
    }

    @Test
    @DisplayName("Should fetch blog by slug")
    void getBlogBySlug_Success() {
        when(blogRepository.findBySlug("introduction-to-dynamic-programming")).thenReturn(Optional.of(samplePost));

        BlogResponseDto response = blogService.getBlogBySlug("introduction-to-dynamic-programming");

        assertNotNull(response);
        assertEquals("Introduction to Dynamic Programming", response.title());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent blog slug")
    void getBlogBySlug_NotFound() {
        when(blogRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> blogService.getBlogBySlug("non-existent"));
    }

    @Test
    @DisplayName("Should create new blog post with generated slug")
    void createBlog_Success() {
        BlogCreateRequest request = new BlogCreateRequest(
                "Graph Theory Basics",
                "Graph theory content here...",
                "Lead Author",
                "graphs,algorithms",
                true
        );

        when(blogRepository.existsBySlug("graph-theory-basics")).thenReturn(false);
        when(blogRepository.save(any(BlogPost.class))).thenAnswer(i -> {
            BlogPost p = i.getArgument(0);
            p.setId(2L);
            return p;
        });

        BlogResponseDto created = blogService.createBlog(request, "Default");

        assertNotNull(created);
        assertEquals("Graph Theory Basics", created.title());
        assertEquals("graph-theory-basics", created.slug());
    }
}
