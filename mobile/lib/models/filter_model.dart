class FilterModel {
  const FilterModel({
    required this.id,
    required this.name,
    required this.displayName,
    required this.description,
    required this.iconName,
    required this.isActive,
  });

  final String id;
  final String name;
  final String displayName;
  final String description;
  final String iconName;
  final bool isActive;

  factory FilterModel.fromJson(Map<String, dynamic> json) {
    return FilterModel(
      id: json['id'] as String,
      name: json['name'] as String,
      displayName: json['display_name'] as String,
      description: json['description'] as String? ?? '',
      iconName: json['icon_name'] as String? ?? '',
      isActive: json['is_active'] as bool? ?? true,
    );
  }
}
