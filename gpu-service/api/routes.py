import base64
import io
import traceback

import numpy as np
from fastapi import APIRouter, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from PIL import Image

from filters import emboss, grayscale, laplaciano, media, sobel, ups_frame

router = APIRouter()

# Mapa de nombre → módulo de filtro
_FILTERS = {
    "laplaciano": laplaciano,
    "sobel":      sobel,
    "media":      media,
    "grayscale":  grayscale,
    "emboss":     emboss,
    "ups_frame":  ups_frame,
}

# Metadatos que ve la app móvil y el backend Java
_FILTER_INFO = [
    {"name": "laplaciano", "display_name": "Laplaciano",          "description": "Resalta bordes detectando cambios bruscos de intensidad"},
    {"name": "sobel",      "display_name": "Detección de Bordes", "description": "Resalta bordes con el operador Sobel en dos direcciones"},
    {"name": "media",      "display_name": "Suavizado de Media",  "description": "Suaviza la imagen promediando píxeles vecinos en ventana 3x3"},
    {"name": "grayscale",  "display_name": "Escala de Grises",    "description": "Convierte la imagen a blanco y negro con pesos de luminancia"},
    {"name": "emboss",     "display_name": "Relieve",             "description": "Efecto artístico 3D usando kernel diagonal de convolución"},
    {"name": "ups_frame",  "display_name": "Marco UPS",           "description": "Marco institucional UPS azul #123672 y dorado #F7BF1A con logo"},
]


def _image_to_numpy(file_bytes: bytes) -> np.ndarray:
    img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
    return np.array(img, dtype=np.uint8)


def _numpy_to_base64(arr: np.ndarray) -> str:
    img = Image.fromarray(arr.astype(np.uint8))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return base64.b64encode(buf.getvalue()).decode("utf-8")


@router.get("/filters")
def list_filters():
    """Lista todos los filtros CUDA disponibles."""
    return {"filters": _FILTER_INFO}


@router.post("/filter/{filter_name}")
async def apply_filter(filter_name: str, file: UploadFile = File(...)):
    """
    Aplica un filtro CUDA a la imagen enviada.

    - **filter_name**: uno de laplaciano, sobel, media, grayscale, emboss, ups_frame
    - **file**: imagen en formato PNG o JPEG (multipart/form-data)

    Devuelve la imagen procesada en base64 junto con las métricas GPU.
    """
    if filter_name not in _FILTERS:
        raise HTTPException(
            status_code=404,
            detail=f"Filtro '{filter_name}' no existe. Disponibles: {list(_FILTERS.keys())}",
        )

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(status_code=400, detail="El archivo está vacío.")

    try:
        img_array = _image_to_numpy(file_bytes)
    except Exception:
        raise HTTPException(status_code=400, detail="No se pudo leer la imagen. Envía PNG o JPEG.")

    height, width, _ = img_array.shape

    try:
        filter_module = _FILTERS[filter_name]
        result_array, metrics = filter_module.apply(img_array)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error en el kernel CUDA: {str(e)}\n{traceback.format_exc()}",
        )

    return JSONResponse({
        "status":        "completed",
        "filter":        filter_name,
        "image_width":   width,
        "image_height":  height,
        "image_base64":  _numpy_to_base64(result_array),
        "metrics":       metrics,
    })
