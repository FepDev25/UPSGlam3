class PostResultModel {
  const PostResultModel({
    required this.id,
    required this.originalImageUrl,
    required this.processedImageUrl,
    required this.likesCount,
    required this.commentsCount,
  });

  final String id;
  final String originalImageUrl;
  final String processedImageUrl;
  final int likesCount;
  final int commentsCount;

  factory PostResultModel.fromJson(Map<String, dynamic> json) {
    return PostResultModel(
      id: json['id'] as String,
      originalImageUrl: json['original_image_url'] as String,
      processedImageUrl: json['processed_image_url'] as String,
      likesCount: json['likes_count'] as int? ?? 0,
      commentsCount: json['comments_count'] as int? ?? 0,
    );
  }
}
