package com.ups.glam.backend.domain.post;

import com.ups.glam.backend.domain.filter.FilterRepository;
import com.ups.glam.backend.domain.processing.GpuMetrics;
import com.ups.glam.backend.domain.processing.GpuMetricsRepository;
import com.ups.glam.backend.domain.processing.ProcessingHistory;
import com.ups.glam.backend.domain.processing.ProcessingRepository;
import com.ups.glam.backend.domain.profile.ProfileService;
import com.ups.glam.backend.infrastructure.cuda.CudaResult;
import com.ups.glam.backend.infrastructure.cuda.CudaServiceClient;
import com.ups.glam.backend.infrastructure.supabase.SupabaseStorageClient;
import com.ups.glam.backend.shared.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ProfileService profileService;
    private final FilterRepository filterRepository;
    private final ProcessingRepository processingRepository;
    private final GpuMetricsRepository gpuMetricsRepository;
    private final CudaServiceClient cudaServiceClient;
    private final SupabaseStorageClient supabaseStorageClient;

    public Mono<PagedResponse<PostResponse>> getFeed(int page, int size) {
        int offset = page * size;
        return postRepository.findPublishedFeed(size, offset)
            .flatMap(this::enrichWithAuthor)
            .collectList()
            .zipWith(postRepository.countPublished())
            .map(tuple -> PagedResponse.<PostResponse>builder()
                .data(tuple.getT1())
                .page(page)
                .size(size)
                .hasNext((long) (offset + tuple.getT1().size()) < tuple.getT2())
                .build());
    }

    public Mono<PostResponse> getById(UUID postId) {
        return postRepository.findById(postId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado")))
            .flatMap(this::enrichWithAuthor);
    }

    public Mono<PagedResponse<PostResponse>> getByUsername(String username, int page, int size) {
        int offset = page * size;
        return profileService.getByUsername(username)
            .flatMap(profile -> postRepository.findByUserIdPublished(profile.getId(), size, offset)
                .flatMap(this::enrichWithAuthor)
                .collectList()
                .zipWith(postRepository.countByUserId(profile.getId()))
                .map(tuple -> PagedResponse.<PostResponse>builder()
                    .data(tuple.getT1())
                    .page(page)
                    .size(size)
                    .hasNext((long) (offset + tuple.getT1().size()) < tuple.getT2())
                    .build())
            );
    }

    public Mono<PostResponse> create(UUID userId, CreatePostRequest request) {
        return filterRepository.findById(request.getFilterId())
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Filtro no encontrado")))
            .flatMap(filter -> {
                OffsetDateTime now = OffsetDateTime.now();
                ProcessingHistory history = ProcessingHistory.builder()
                    .userId(userId)
                    .filterId(filter.getId())
                    .originalImageUrl(request.getOriginalImageUrl())
                    .status("pending")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

                return processingRepository.save(history)
                    .flatMap(savedHistory -> cudaServiceClient
                        .process(request.getOriginalImageUrl(), filter.getName(), savedHistory.getId())
                        .flatMap(cudaResult -> supabaseStorageClient
                            .uploadProcessedImage(userId, savedHistory.getId(), cudaResult.getProcessedImageBytes())
                            .flatMap(processedUrl -> savePostAndMetrics(userId, request, filter.getId(), processedUrl, savedHistory.getId(), cudaResult))
                        )
                        .onErrorResume(e -> processingRepository
                            .updateFailed(savedHistory.getId(), "failed", e.getMessage())
                            .then(Mono.error(e))
                        )
                    );
            });
    }

    public Mono<Void> delete(UUID postId, UUID userId) {
        return postRepository.findById(postId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado")))
            .flatMap(post -> {
                if (!post.getUserId().equals(userId)) {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para borrar este post"));
                }
                return postRepository.deleteById(postId);
            });
    }

    private Mono<PostResponse> savePostAndMetrics(
        UUID userId, CreatePostRequest request, UUID filterId,
        String processedUrl, UUID historyId, CudaResult cudaResult
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        Post post = Post.builder()
            .userId(userId)
            .filterId(filterId)
            .caption(request.getCaption())
            .originalImageUrl(request.getOriginalImageUrl())
            .processedImageUrl(processedUrl)
            .likesCount(0)
            .commentsCount(0)
            .isPublished(true)
            .createdAt(now)
            .updatedAt(now)
            .build();

        return postRepository.save(post)
            .flatMap(savedPost -> {
                GpuMetrics metrics = buildGpuMetrics(historyId, cudaResult);
                return gpuMetricsRepository.save(metrics)
                    .then(processingRepository.updateCompleted(historyId, "completed", processedUrl, savedPost.getId()))
                    .then(profileService.getAuthorInfo(userId))
                    .map(author -> toResponse(savedPost, author));
            });
    }

    private Mono<PostResponse> enrichWithAuthor(Post post) {
        return profileService.getAuthorInfo(post.getUserId())
            .map(author -> toResponse(post, author));
    }

    private GpuMetrics buildGpuMetrics(UUID processingId, CudaResult r) {
        return GpuMetrics.builder()
            .processingId(processingId)
            .filterName(r.getFilterName())
            .imageWidth(r.getImageWidth())
            .imageHeight(r.getImageHeight())
            .blockDimX(r.getBlockDimX())
            .blockDimY(r.getBlockDimY())
            .gridDimX(r.getGridDimX())
            .gridDimY(r.getGridDimY())
            .totalThreads(r.getTotalThreads())
            .kernelTimeMs(r.getKernelTimeMs())
            .totalTimeMs(r.getTotalTimeMs())
            .memoryTransferredMb(r.getMemoryTransferredMb())
            .gpuMemoryUsedMb(r.getGpuMemoryUsedMb())
            .cudaVersion(r.getCudaVersion() != null ? r.getCudaVersion() : "12.8")
            .status("success")
            .createdAt(OffsetDateTime.now())
            .build();
    }

    PostResponse toResponse(Post post, AuthorInfo author) {
        return PostResponse.builder()
            .id(post.getId())
            .userId(post.getUserId())
            .filterId(post.getFilterId())
            .caption(post.getCaption())
            .originalImageUrl(post.getOriginalImageUrl())
            .processedImageUrl(post.getProcessedImageUrl())
            .likesCount(post.getLikesCount())
            .commentsCount(post.getCommentsCount())
            .isPublished(post.getIsPublished())
            .createdAt(post.getCreatedAt())
            .author(author)
            .build();
    }
}
