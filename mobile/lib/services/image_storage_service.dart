import 'package:image_picker/image_picker.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class ImageStorageService {
  ImageStorageService();

  final _supabase = Supabase.instance.client;

  Future<String> uploadOriginalImage(XFile image) async {
    final user = _supabase.auth.currentUser;

    if (user == null) {
      throw Exception('No hay usuario autenticado');
    }

    final bytes = await image.readAsBytes();
    final extension = image.name.split('.').last.toLowerCase();
    final safeExtension = extension == 'png' ? 'png' : 'jpg';
    final fileName = '${DateTime.now().millisecondsSinceEpoch}.$safeExtension';
    final path = '${user.id}/$fileName';

    await _supabase.storage.from('original-images').uploadBinary(
          path,
          bytes,
          fileOptions: FileOptions(
            contentType: safeExtension == 'png' ? 'image/png' : 'image/jpeg',
            upsert: false,
          ),
        );

    return _supabase.storage.from('original-images').getPublicUrl(path);
  }
}
