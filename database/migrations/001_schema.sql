-- ============================================================
-- UPSGlam 3.0 — Esquema de Base de Datos


-- UUID (Universally Unique Identifier) es un ID aleatorio como:
-- "a3f5c2d1-8b4e-4f9a-b2c7-1234567890ab"
-- Lo usamos en lugar de 1, 2, 3... porque:
--   1) Supabase Auth ya usa UUIDs para los usuarios
--   2) Son seguros 

-- Usamos TIMESTAMPTZ
-- TIMESTAMPTZ = timestamp WITH time zone
-- Guarda la fecha Y la zona horaria. Importante porque:
--   - Supabase está en São Paulo
--   - Si guardamos sin zona horaria, las fechas pueden estar desfasadas 1-5 horas
--   - Con TIMESTAMPTZ, la fecha se convierte automáticamente a la zona correcta

-- EXTENSIONES necesarias
-- ============================================================
-- uuid-ossp nos da la función uuid_generate_v4() para crear UUIDs automáticamente
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- TABLA 1: profiles, son Los datos del perfil de cada usuario
-- La hicimos ya que Supabase Auth solo guarda email + contraseña.
--   Todo lo demás (nombre, bio, foto) va aquí.
-- SE relaciona por que un profiles.id = un auth.users.id de Supabase
-- ============================================================
CREATE TABLE IF NOT EXISTS profiles (
    id          UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    -- El ID es el MISMO que el de Supabase Auth. 
    -- ON DELETE CASCADE significa:que si el usuario se borra de Auth, borra su perfil también

    username    VARCHAR(50) UNIQUE NOT NULL,
    full_name   VARCHAR(100),
    bio         TEXT,
    avatar_url  TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- TABLA 2: filters, son el catálogo de filtros CUDA disponibles en la app, esta tabla le dice cuáles existen, su nombre, descripción e ícono.
-- ============================================================
CREATE TABLE IF NOT EXISTS filters (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name          VARCHAR(100) UNIQUE NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    description   TEXT,
    icon_name     VARCHAR(50),
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Insertamos los 6 filtros que implementarás en CUDA
INSERT INTO filters (name, display_name, description, icon_name) VALUES
    ('laplaciano',    'Laplaciano',         'Resalta bordes y detalles finos detectando cambios bruscos de intensidad',      'vector-triangle'),
    ('sobel',         'Detección de Bordes','Resalta los bordes de la imagen usando el operador Sobel extendido',             'scan'),
    ('media',         'Suavizado de Media', 'Suaviza la imagen promediando los píxeles vecinos en una ventana NxN',          'blur_on'),
    ('grayscale',     'Escala de Grises',   'Convierte la imagen a blanco y negro usando pesos perceptuales RGB',            'contrast'),
    ('emboss',        'Relieve',            'Efecto artístico 3D que simula relieve usando un kernel diagonal de convolución','badge-3d'),
    ('ups_frame',     'Marco UPS',          'Agrega un marco con los colores y logo institucional de la UPS Don Bosco',      'school');

-- TABLA 3: posts, son cada publicación que hace un usuario en la red social
-- Se relacionan: 
--   posts.user_id → profiles.id (quién publicó)
--   posts.filter_id → filters.id (qué filtro usó)
-- ============================================================
CREATE TABLE IF NOT EXISTS posts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    filter_id           UUID REFERENCES filters(id) ON DELETE SET NULL,
    caption             TEXT,
    original_image_url  TEXT NOT NULL,
    processed_image_url TEXT,
    likes_count         INTEGER DEFAULT 0,
    comments_count      INTEGER DEFAULT 0,
    is_published        BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- TABLA 4: likes, registra qué usuario le dio like a qué post
--   el usuario actual ya le dio like, es importante saber que un usuario solo puede dar UN like por post
-- ============================================================
CREATE TABLE IF NOT EXISTS likes (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    user_id     UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,

    created_at  TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT likes_unique UNIQUE (user_id, post_id)
);

-- TABLA 5: comments, son los comentarios que hacen los usuarios en los posts
-- ============================================================
CREATE TABLE IF NOT EXISTS comments (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- TABLA 6: processing_history, es el registro de cada vez que alguien procesa una imagen en GPU
-- Sirve para:
--   1) El usuario puede ver el historial de sus imágenes procesadas
--   2) Es evidencia académica de que el servicio GPU funciona
--   3) El profesor lo pide en la rúbrica
-- ============================================================
CREATE TABLE IF NOT EXISTS processing_history (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    user_id             UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,

    filter_id           UUID REFERENCES filters(id) ON DELETE SET NULL,

    post_id             UUID REFERENCES posts(id) ON DELETE SET NULL,
    original_image_url  TEXT NOT NULL,
    processed_image_url TEXT,

    status              VARCHAR(20) DEFAULT 'pending',
    error_message       TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- TABLA 7: gpu_metrics, son los detalles técnicos de cada ejecución de kernel CUDA, nos dice que usamos CUDA
-- ============================================================
CREATE TABLE IF NOT EXISTS gpu_metrics (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    processing_id       UUID NOT NULL REFERENCES processing_history(id) ON DELETE CASCADE,

    filter_name         VARCHAR(100) NOT NULL,
    image_width         INTEGER NOT NULL,
    image_height        INTEGER NOT NULL,
    block_dim_x         INTEGER NOT NULL,
    block_dim_y         INTEGER NOT NULL,

    grid_dim_x          INTEGER NOT NULL,
    grid_dim_y          INTEGER NOT NULL,

    total_threads       INTEGER NOT NULL,
    kernel_time_ms      FLOAT NOT NULL,
    total_time_ms       FLOAT NOT NULL,


    memory_transferred_mb FLOAT,
    gpu_memory_used_mb  FLOAT,

    -- METADATOS
    cuda_version        VARCHAR(20) DEFAULT '12.8',
    compute_capability  VARCHAR(10) DEFAULT '12.0',


    status              VARCHAR(20) DEFAULT 'success',

    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ÍNDICES 
-- Nos permite encontrar registros rápido sin leer toda la tabla.
-- ============================================================

-- Para cargar el feed: "dame los posts ordenados por fecha"
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);

-- Para cargar el perfil: "dame los posts de este usuario"
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);

-- Para verificar si ya di like
CREATE INDEX IF NOT EXISTS idx_likes_post_user ON likes(post_id, user_id);

-- Para el historial: "dame el historial de procesamiento de este usuario"
CREATE INDEX IF NOT EXISTS idx_processing_user ON processing_history(user_id, created_at DESC);

-- Para las métricas: "dame las métricas de este procesamiento"
CREATE INDEX IF NOT EXISTS idx_gpu_metrics_processing ON gpu_metrics(processing_id);

-- ============================================================
-- SEGURIDAD A NIVEL DE FILA, es una función de Supabase/PostgreSQL que controla que filas podemos ver o modificar por usuario autenticado
-- Sin RLS, cualquier usuario autenticado podría leer o borrar los datos de OTRO usuario. Con RLS:
-- ============================================================

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE processing_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE gpu_metrics ENABLE ROW LEVEL SECURITY;

-- POLÍTICAS RLS para profiles:
-- Cualquiera puede VER perfiles
CREATE POLICY "Profiles son públicos para lectura"
    ON profiles FOR SELECT USING (true);

-- Solo el dueño puede EDITAR su perfil
CREATE POLICY "Solo el dueño puede editar su perfil"
    ON profiles FOR UPDATE USING (auth.uid() = id);

-- Supabase crea el perfil automáticamente al registrarse
CREATE POLICY "El sistema puede insertar perfiles"
    ON profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- POLÍTICAS RLS para posts:
-- Posts publicados son visibles para todos
CREATE POLICY "Posts publicados son públicos"
    ON posts FOR SELECT USING (is_published = true OR auth.uid() = user_id);

-- Solo el dueño puede crear, editar o borrar sus posts
CREATE POLICY "Solo el dueño puede insertar posts"
    ON posts FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Solo el dueño puede editar sus posts"
    ON posts FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Solo el dueño puede borrar sus posts"
    ON posts FOR DELETE USING (auth.uid() = user_id);

-- POLÍTICAS RLS para likes:
CREATE POLICY "Likes son públicos para lectura"
    ON likes FOR SELECT USING (true);

CREATE POLICY "Solo usuario autenticado puede dar like"
    ON likes FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Solo el dueño puede quitar su like"
    ON likes FOR DELETE USING (auth.uid() = user_id);

-- POLÍTICAS RLS para comments:
CREATE POLICY "Comentarios son públicos para lectura"
    ON comments FOR SELECT USING (true);

CREATE POLICY "Solo usuario autenticado puede comentar"
    ON comments FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Solo el dueño puede borrar su comentario"
    ON comments FOR DELETE USING (auth.uid() = user_id);

-- POLÍTICAS RLS para processing_history:
CREATE POLICY "Solo el dueño ve su historial"
    ON processing_history FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "El servicio GPU puede insertar historial"
    ON processing_history FOR INSERT WITH CHECK (true);
    

CREATE POLICY "El servicio GPU puede actualizar historial"
    ON processing_history FOR UPDATE USING (true);

-- POLÍTICAS RLS para gpu_metrics:
CREATE POLICY "Solo el dueño ve sus métricas GPU"
    ON gpu_metrics FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM processing_history ph
            WHERE ph.id = gpu_metrics.processing_id
            AND ph.user_id = auth.uid()
        )
    );

CREATE POLICY "El servicio GPU puede insertar métricas"
    ON gpu_metrics FOR INSERT WITH CHECK (true);

-- TRIGGER — Crear perfil automáticamente al registrarse, sirve para cuando alguien se registra en Supabase Auth, se crea
-- una fila en auth.users. Queremos que AUTOMÁTICAMENTE también
-- se cree una fila en nuestra tabla profiles.
-- ============================================================

CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, full_name, avatar_url)
    VALUES (
        NEW.id,
        -- Username: tomamos la parte antes del @ del email
        
        SPLIT_PART(NEW.email, '@', 1),
        -- Full name: del metadata del registro (si el usuario lo proporcionó)
        NEW.raw_user_meta_data->>'full_name',
        -- Avatar: del proveedor OAuth si usaron Google/GitHub (opcional)
        NEW.raw_user_meta_data->>'avatar_url'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Activamos el trigger: cada vez que se inserta en auth.users, llama a la función
CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_user();


-- FIN DEL ESQUEMA
-- ============================================================
-- Resumen de tablas creadas:
--   profiles           → datos de usuarios (1 por auth.user)
--   filters            → catálogo de 6 filtros CUDA (estático)
--   posts              → publicaciones de la red social
--   likes              → likes de usuarios en posts
--   comments           → comentarios en posts
--   processing_history → historial de procesamiento GPU por usuario
--   gpu_metrics        → métricas técnicas CUDA por procesamiento