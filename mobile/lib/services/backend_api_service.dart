import 'dart:convert';

import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:http/http.dart' as http;
import 'package:supabase_flutter/supabase_flutter.dart';

class BackendApiService {
  BackendApiService();

  String get _baseUrl {
    final value = dotenv.env['API_BASE_URL'];

    if (value == null || value.isEmpty) {
      throw Exception('Falta API_BASE_URL en mobile/.env');
    }

    return value;
  }

  Future<List<Map<String, dynamic>>> getFilters() async {
    final uri = Uri.parse('$_baseUrl/filters');

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('Error cargando filtros: ${response.statusCode} ${response.body}');
    }

    final decoded = jsonDecode(response.body);

    return List<Map<String, dynamic>>.from(decoded);
  }

  Future<Map<String, dynamic>> createPost({
    required String originalImageUrl,
    required String filterId,
    String? caption,
  }) async {
    final session = Supabase.instance.client.auth.currentSession;

    if (session == null) {
      throw Exception('No hay sesión activa');
    }

    final uri = Uri.parse('$_baseUrl/posts');

    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ${session.accessToken}',
      },
      body: jsonEncode({
        'original_image_url': originalImageUrl,
        'filter_id': filterId,
        if (caption != null && caption.isNotEmpty) 'caption': caption,
      }),
    );

    if (response.statusCode != 201) {
      throw Exception('Error creando post: ${response.statusCode} ${response.body}');
    }

    return Map<String, dynamic>.from(jsonDecode(response.body));
  }
}
