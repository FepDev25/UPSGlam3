import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import 'models/filter_model.dart';
import 'models/post_result_model.dart';
import 'services/backend_api_service.dart';
import 'services/image_storage_service.dart';
import 'services/supabase_config.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await SupabaseConfig.initialize();

  runApp(const UPSGlamApp());
}

class UPSGlamApp extends StatelessWidget {
  const UPSGlamApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'UPSGlam 3.0',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF1565C0),
        useMaterial3: true,
      ),
      home: const AuthGate(),
    );
  }
}

class AuthGate extends StatefulWidget {
  const AuthGate({super.key});

  @override
  State<AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<AuthGate> {
  Session? _session;

  @override
  void initState() {
    super.initState();
    _session = Supabase.instance.client.auth.currentSession;

    Supabase.instance.client.auth.onAuthStateChange.listen((data) {
      if (!mounted) {
        return;
      }

      setState(() {
        _session = data.session;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_session == null) {
      return const AuthPage();
    }

    return const HomePage();
  }
}

class AuthPage extends StatefulWidget {
  const AuthPage({super.key});

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _isRegisterMode = false;
  bool _isLoading = false;
  String? _message;

  Future<void> _submit() async {
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();

    if (email.isEmpty || password.isEmpty) {
      setState(() {
        _message = 'Ingresa correo y contraseña.';
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _message = null;
    });

    try {
      if (_isRegisterMode) {
        await Supabase.instance.client.auth.signUp(
          email: email,
          password: password,
        );

        setState(() {
          _message = 'Usuario registrado. Si Supabase pide confirmación, valida el correo antes de iniciar sesión.';
        });
      } else {
        await Supabase.instance.client.auth.signInWithPassword(
          email: email,
          password: password,
        );
      }
    } on AuthException catch (error) {
      setState(() {
        _message = error.message;
      });
    } catch (error) {
      setState(() {
        _message = 'Error inesperado: $error';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final title = _isRegisterMode ? 'Crear cuenta' : 'Iniciar sesión';

    return Scaffold(
      appBar: AppBar(
        title: const Text('UPSGlam 3.0'),
        centerTitle: true,
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Card(
              elevation: 2,
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.auto_awesome, size: 56),
                    const SizedBox(height: 12),
                    Text(
                      title,
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 24),
                    TextField(
                      controller: _emailController,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(
                        labelText: 'Correo',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _passwordController,
                      obscureText: true,
                      decoration: const InputDecoration(
                        labelText: 'Contraseña',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 20),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: _isLoading ? null : _submit,
                        child: _isLoading
                            ? const SizedBox(
                                height: 18,
                                width: 18,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : Text(title),
                      ),
                    ),
                    TextButton(
                      onPressed: _isLoading
                          ? null
                          : () {
                              setState(() {
                                _isRegisterMode = !_isRegisterMode;
                                _message = null;
                              });
                            },
                      child: Text(
                        _isRegisterMode
                            ? 'Ya tengo cuenta'
                            : 'Crear una cuenta nueva',
                      ),
                    ),
                    if (_message != null) ...[
                      const SizedBox(height: 12),
                      Text(
                        _message!,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _backendApi = BackendApiService();
  final _imageStorage = ImageStorageService();
  final _picker = ImagePicker();
  final _captionController = TextEditingController();

  bool _isLoadingFilters = true;
  bool _isProcessing = false;
  String? _message;
  String? _filtersSource;

  List<FilterModel> _filters = [];
  FilterModel? _selectedFilter;
  XFile? _selectedImage;
  Uint8List? _selectedImageBytes;
  PostResultModel? _result;

  @override
  void initState() {
    super.initState();
    _loadFilters();
  }

  Future<void> _loadFilters() async {
    setState(() {
      _isLoadingFilters = true;
      _message = null;
    });

    try {
      final response = await _backendApi.getFilters();

      setState(() {
        _filters = response.map(FilterModel.fromJson).toList();
        _selectedFilter = _filters.isNotEmpty ? _filters.first : null;
        _filtersSource = 'Backend';
      });
    } catch (_) {
      final fallbackFilters = _demoFilters();

      setState(() {
        _filters = fallbackFilters;
        _selectedFilter = fallbackFilters.first;
        _filtersSource = 'Demo local';
        _message = 'Backend no disponible todavía. Se muestran filtros de demostración hasta tener el .env de la raíz.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingFilters = false;
        });
      }
    }
  }

  Future<void> _pickImage() async {
    final image = await _picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 90,
    );

    if (image == null) {
      return;
    }

    final bytes = await image.readAsBytes();

    setState(() {
      _selectedImage = image;
      _selectedImageBytes = bytes;
      _result = null;
      _message = null;
    });
  }

  Future<void> _processImage() async {
    if (_selectedImage == null) {
      setState(() {
        _message = 'Selecciona una imagen primero.';
      });
      return;
    }

    if (_selectedFilter == null) {
      setState(() {
        _message = 'Selecciona un filtro primero.';
      });
      return;
    }

    setState(() {
      _isProcessing = true;
      _message = null;
      _result = null;
    });

    try {
      final originalImageUrl = await _imageStorage.uploadOriginalImage(
        _selectedImage!,
      );

      final response = await _backendApi.createPost(
        originalImageUrl: originalImageUrl,
        filterId: _selectedFilter!.id,
        caption: _captionController.text.trim(),
      );

      setState(() {
        _result = PostResultModel.fromJson(response);
        _message = 'Imagen procesada correctamente.';
      });
    } catch (error) {
      setState(() {
        _message = 'No se pudo procesar todavía: $error';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
    }
  }

  Future<void> _signOut() async {
    await Supabase.instance.client.auth.signOut();
  }

  List<FilterModel> _demoFilters() {
    return const [
      FilterModel(
        id: 'e68f5947-7ada-4d88-aa6f-7e5c60bbe09b',
        name: 'sobel',
        displayName: 'Detección de Bordes',
        description: 'Resalta los bordes de la imagen usando el operador Sobel.',
        iconName: 'scan',
        isActive: true,
      ),
      FilterModel(
        id: '5ce2a9d8-de6f-4612-9a90-a1cf5dfdbf39',
        name: 'laplaciano',
        displayName: 'Laplaciano',
        description: 'Resalta bordes y detalles finos detectando cambios bruscos de intensidad.',
        iconName: 'vector-triangle',
        isActive: true,
      ),
      FilterModel(
        id: 'a9cb7518-5534-45aa-9167-9c4127c3ff4b',
        name: 'media',
        displayName: 'Suavizado de Media',
        description: 'Suaviza la imagen promediando los píxeles vecinos.',
        iconName: 'blur_on',
        isActive: true,
      ),
      FilterModel(
        id: 'e1989c3f-12b8-4b22-874f-bce15f08f701',
        name: 'grayscale',
        displayName: 'Escala de Grises',
        description: 'Convierte la imagen a blanco y negro.',
        iconName: 'contrast',
        isActive: true,
      ),
      FilterModel(
        id: 'f5fe7de2-1439-42fa-ab4d-d3bc4136b000',
        name: 'emboss',
        displayName: 'Relieve',
        description: 'Efecto artístico 3D que simula relieve.',
        iconName: 'badge-3d',
        isActive: true,
      ),
      FilterModel(
        id: 'af31afa0-3a9f-43f3-91e1-a821311ef22c',
        name: 'ups_frame',
        displayName: 'Marco UPS',
        description: 'Agrega un marco institucional UPS a la imagen.',
        iconName: 'school',
        isActive: true,
      ),
    ];
  }

  @override
  void dispose() {
    _captionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final userEmail = Supabase.instance.client.auth.currentUser?.email ?? 'Usuario';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Procesamiento GPU'),
        actions: [
          IconButton(
            onPressed: _loadFilters,
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            onPressed: _signOut,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: _isLoadingFilters
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _HeaderCard(
                  userEmail: userEmail,
                  filtersSource: _filtersSource ?? 'Sin fuente',
                ),
                const SizedBox(height: 16),
                _ImagePickerCard(
                  selectedImageBytes: _selectedImageBytes,
                  onPickImage: _pickImage,
                ),
                const SizedBox(height: 16),
                _FilterSelectorCard(
                  filters: _filters,
                  selectedFilter: _selectedFilter,
                  onChanged: (filter) {
                    setState(() {
                      _selectedFilter = filter;
                      _result = null;
                    });
                  },
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _captionController,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: 'Descripción de la publicación',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                FilledButton.icon(
                  onPressed: _isProcessing ? null : _processImage,
                  icon: _isProcessing
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.memory),
                  label: Text(_isProcessing ? 'Procesando...' : 'Procesar con GPU'),
                ),
                if (_message != null) ...[
                  const SizedBox(height: 16),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Text(_message!),
                    ),
                  ),
                ],
                if (_result != null) ...[
                  const SizedBox(height: 16),
                  _ResultCard(result: _result!),
                ],
              ],
            ),
    );
  }
}

class _HeaderCard extends StatelessWidget {
  const _HeaderCard({
    required this.userEmail,
    required this.filtersSource,
  });

  final String userEmail;
  final String filtersSource;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Sesión activa', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 4),
            Text(userEmail),
            const SizedBox(height: 8),
            Text('Filtros cargados desde: $filtersSource'),
          ],
        ),
      ),
    );
  }
}

class _ImagePickerCard extends StatelessWidget {
  const _ImagePickerCard({
    required this.selectedImageBytes,
    required this.onPickImage,
  });

  final Uint8List? selectedImageBytes;
  final VoidCallback onPickImage;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          AspectRatio(
            aspectRatio: 4 / 3,
            child: selectedImageBytes == null
                ? const ColoredBox(
                    color: Color(0xFFE3F2FD),
                    child: Center(
                      child: Icon(Icons.image, size: 64),
                    ),
                  )
                : Image.memory(
                    selectedImageBytes!,
                    fit: BoxFit.cover,
                  ),
          ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: onPickImage,
                icon: const Icon(Icons.photo_library),
                label: const Text('Seleccionar imagen'),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _FilterSelectorCard extends StatelessWidget {
  const _FilterSelectorCard({
    required this.filters,
    required this.selectedFilter,
    required this.onChanged,
  });

  final List<FilterModel> filters;
  final FilterModel? selectedFilter;
  final ValueChanged<FilterModel?> onChanged;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: DropdownButtonFormField<FilterModel>(
          initialValue: selectedFilter,
          items: filters.map((filter) {
            return DropdownMenuItem(
              value: filter,
              child: Text(filter.displayName),
            );
          }).toList(),
          onChanged: onChanged,
          decoration: const InputDecoration(
            labelText: 'Filtro CUDA',
            border: OutlineInputBorder(),
          ),
        ),
      ),
    );
  }
}

class _ResultCard extends StatelessWidget {
  const _ResultCard({required this.result});

  final PostResultModel result;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Image.network(result.processedImageUrl),
          Padding(
            padding: const EdgeInsets.all(12),
            child: SelectableText(
              'Resultado guardado:\n${result.processedImageUrl}',
            ),
          ),
        ],
      ),
    );
  }
}
