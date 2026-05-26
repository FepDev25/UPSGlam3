package com.ups.glam.backend.domain.comment;

import com.ups.glam.backend.domain.post.AuthorInfo;
import com.ups.glam.backend.domain.post.PostRepository;
import com.ups.glam.backend.domain.profile.ProfileService;
import com.ups.glam.backend.shared.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ProfileService profileService;

    public Mono<PagedResponse<CommentResponse>> getByPostId(UUID postId, int page, int size) {
        int offset = page * size;
        return commentRepository.findByPostIdPaged(postId, size, offset)
            .flatMap(this::enrichWithAuthor)
            .collectList()
            .zipWith(commentRepository.countByPostId(postId))
            .map(tuple -> PagedResponse.<CommentResponse>builder()
                .data(tuple.getT1())
                .page(page)
                .size(size)
                .hasNext((long) (offset + tuple.getT1().size()) < tuple.getT2())
                .build());
    }

    @Transactional
    public Mono<CommentResponse> create(UUID postId, UUID userId, CreateCommentRequest request) {
        return postRepository.existsById(postId)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
                }
                OffsetDateTime now = OffsetDateTime.now();
                Comment comment = Comment.builder()
                    .postId(postId)
                    .userId(userId)
                    .content(request.getContent())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
                return commentRepository.save(comment);
            })
            .flatMap(savedComment -> profileService.getAuthorInfo(userId)
                .map(author -> toResponse(savedComment, author))
            );
    }

    @Transactional
    public Mono<Void> delete(UUID postId, UUID commentId, UUID userId) {
        return commentRepository.findByIdAndUserId(commentId, userId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Comentario no encontrado o no tienes permiso para borrarlo"
            )))
            .flatMap(comment -> commentRepository.deleteByIdAndUserId(commentId, userId))
            .then();
    }

    private Mono<CommentResponse> enrichWithAuthor(Comment comment) {
        return profileService.getAuthorInfo(comment.getUserId())
            .map(author -> toResponse(comment, author));
    }

    private CommentResponse toResponse(Comment comment, AuthorInfo author) {
        return CommentResponse.builder()
            .id(comment.getId())
            .postId(comment.getPostId())
            .userId(comment.getUserId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .author(author)
            .build();
    }
}
