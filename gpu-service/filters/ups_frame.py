import time
from pathlib import Path

import numpy as np
import pycuda.autoinit  # noqa: F401
import pycuda.driver as cuda
from PIL import Image
from pycuda.compiler import SourceModule

# Colores institucionales reales extraídos del Logo_Universidad.png
_UPS_BLUE = (18,  54,  114)   # #123672 — azul marino UPS
_UPS_GOLD = (247, 191, 26)    # #F7BF1A — dorado UPS (Don Bosco / globo)

# Ruta por defecto del logo dentro del servicio
_LOGO_DEFAULT = Path(__file__).parent.parent / "assets" / "Logo_Universidad.png"

# Diseño del marco:
#  ┌────────────────────────────────────────────────┐
#  │ [Logo UPS]          AZUL MARINO #123672        │  ← banda superior
#  │════════════════════════════════════════════════│
#  │                                                │
#  │              foto original intacta             │  ← centro sin tocar
#  │                                                │
#  │════════════════════════════════════════════════│
#  │        DORADO #F7BF1A          [Logo UPS]      │  ← banda inferior
#  └────────────────────────────────────────────────┘
#
# El kernel CUDA evalúa (x,y) de cada píxel y decide:
#   - Banda superior (y < border_h): azul UPS; esquina sup-izq → logo
#   - Banda inferior (y >= h-border_h): dorado UPS; esquina inf-der → logo
#   - Centro: copia píxel original

_KERNEL = """
__global__ void ups_frame(
    unsigned char *in_img,
    unsigned char *out_img,
    unsigned char *logo,
    int width, int height,
    int border_h,
    int logo_w, int logo_h,
    unsigned char top_r, unsigned char top_g, unsigned char top_b,
    unsigned char bot_r, unsigned char bot_g, unsigned char bot_b
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= width || y >= height) return;

    int out_idx = (y * width + x) * 3;

    // --- Banda superior: azul UPS ---
    if (y < border_h) {
        // Logo en esquina superior izquierda
        if (x < logo_w && y < logo_h) {
            int logo_idx = (y * logo_w + x) * 3;
            out_img[out_idx    ] = logo[logo_idx    ];
            out_img[out_idx + 1] = logo[logo_idx + 1];
            out_img[out_idx + 2] = logo[logo_idx + 2];
        } else {
            out_img[out_idx    ] = top_r;
            out_img[out_idx + 1] = top_g;
            out_img[out_idx + 2] = top_b;
        }
        return;
    }

    // --- Banda inferior: dorado UPS ---
    if (y >= height - border_h) {
        int local_y = y - (height - border_h);
        int local_x = x - (width - logo_w);
        // Logo en esquina inferior derecha
        if (local_x >= 0 && local_x < logo_w && local_y >= 0 && local_y < logo_h) {
            int logo_idx = (local_y * logo_w + local_x) * 3;
            out_img[out_idx    ] = logo[logo_idx    ];
            out_img[out_idx + 1] = logo[logo_idx + 1];
            out_img[out_idx + 2] = logo[logo_idx + 2];
        } else {
            out_img[out_idx    ] = bot_r;
            out_img[out_idx + 1] = bot_g;
            out_img[out_idx + 2] = bot_b;
        }
        return;
    }

    // --- Centro: foto original intacta ---
    int in_idx = (y * width + x) * 3;
    out_img[out_idx    ] = in_img[in_idx    ];
    out_img[out_idx + 1] = in_img[in_idx + 1];
    out_img[out_idx + 2] = in_img[in_idx + 2];
}
"""

_mod = SourceModule(_KERNEL)
_kernel_fn = _mod.get_function("ups_frame")

_BLOCK = (16, 16, 1)


def _load_logo(logo_path: Path, target_w: int, target_h: int) -> np.ndarray:
    """
    Carga y redimensiona el logo manteniendo la proporción.
    Si el archivo no existe genera un placeholder con el nombre de la universidad.
    """
    if logo_path.exists():
        img = Image.open(logo_path).convert("RGB")
        # Redimensionar manteniendo aspect ratio dentro de (target_w, target_h)
        img.thumbnail((target_w, target_h), Image.LANCZOS)
        # Canvas del tamaño exacto con fondo azul UPS para rellenar bordes
        canvas = Image.new("RGB", (target_w, target_h), _UPS_BLUE)
        paste_x = (target_w - img.width) // 2
        paste_y = (target_h - img.height) // 2
        canvas.paste(img, (paste_x, paste_y))
        return np.ascontiguousarray(np.array(canvas, dtype=np.uint8))

    # Fallback: fondo azul con texto
    from PIL import ImageDraw, ImageFont
    canvas = Image.new("RGB", (target_w, target_h), _UPS_BLUE)
    draw = ImageDraw.Draw(canvas)
    try:
        font = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            size=max(8, target_h // 3),
        )
    except OSError:
        font = ImageFont.load_default()
    draw.text((4, target_h // 4), "UPS", fill=(255, 255, 255), font=font)
    return np.ascontiguousarray(np.array(canvas, dtype=np.uint8))


def apply(
    image: np.ndarray,
    logo_path: str | None = None,
) -> tuple[np.ndarray, dict]:
    img = np.ascontiguousarray(image, dtype=np.uint8)
    height, width, channels = img.shape
    assert channels == 3, "Se esperan imágenes RGB de 3 canales"
    result = np.empty_like(img)

    # Banda: 10% de la altura, entre 30 y 100 px
    border_h = max(30, min(100, height // 10))

    # El logo tiene proporción 897:269 ≈ 3.33:1
    # Dentro de la banda ocupa hasta 40% del ancho de la imagen
    logo_h = border_h
    logo_w = min(int(logo_h * (897 / 269)), width * 2 // 5)

    resolved_logo = Path(logo_path) if logo_path else _LOGO_DEFAULT
    logo = _load_logo(resolved_logo, logo_w, logo_h)

    grid_x = (width  + _BLOCK[0] - 1) // _BLOCK[0]
    grid_y = (height + _BLOCK[1] - 1) // _BLOCK[1]

    d_in   = cuda.mem_alloc(img.nbytes)
    d_out  = cuda.mem_alloc(result.nbytes)
    d_logo = cuda.mem_alloc(logo.nbytes)

    t0 = time.perf_counter()
    cuda.memcpy_htod(d_in,   img)
    cuda.memcpy_htod(d_logo, logo)

    start_evt = cuda.Event()
    end_evt   = cuda.Event()
    start_evt.record()
    _kernel_fn(
        d_in, d_out, d_logo,
        np.int32(width),   np.int32(height),
        np.int32(border_h),
        np.int32(logo_w),  np.int32(logo_h),
        # Banda superior: azul marino UPS
        np.uint8(_UPS_BLUE[0]), np.uint8(_UPS_BLUE[1]), np.uint8(_UPS_BLUE[2]),
        # Banda inferior: dorado UPS
        np.uint8(_UPS_GOLD[0]), np.uint8(_UPS_GOLD[1]), np.uint8(_UPS_GOLD[2]),
        block=_BLOCK, grid=(grid_x, grid_y),
    )
    end_evt.record()
    end_evt.synchronize()

    cuda.memcpy_dtoh(result, d_out)
    total_ms = (time.perf_counter() - t0) * 1000.0

    d_in.free()
    d_out.free()
    d_logo.free()

    return result, {
        "kernel_time_ms": round(start_evt.time_till(end_evt), 4),
        "total_time_ms":  round(total_ms, 4),
        "block_dim_x": _BLOCK[0],
        "block_dim_y": _BLOCK[1],
        "grid_dim_x":  grid_x,
        "grid_dim_y":  grid_y,
        "total_threads": grid_x * _BLOCK[0] * grid_y * _BLOCK[1],
    }
