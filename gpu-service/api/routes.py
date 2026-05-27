import base64
import io
import traceback
import uuid as _uuid

import httpx
import numpy as np
import pycuda.driver as cuda
from fastapi import APIRouter, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from PIL import Image
from pydantic import BaseModel

from filters import emboss, grayscale, laplaciano, media, sobel, ups_frame

router = APIRouter()

_FILTERS = {
    "laplaciano": laplaciano,
    "sobel":      sobel,
    "media":      media,
    "grayscale":  grayscale,
    "emboss":     emboss,
    "ups_frame":  ups_frame,
}

_FILTER_INFO = [
    {"name": "laplaciano", "display_name": "Laplaciano",          "description": "Resalta bordes detectando cambios bruscos de intensidad"},
    {"name": "sobel",      "display_name": "Detección de Bordes", "description": "Resalta bordes con el operador Sobel en dos direcciones"},
    {"name": "media",      "display_name": "Suavizado de Media",  "description": "Suaviza la imagen promediando píxeles vecinos en ventana 3x3"},
    {"name": "grayscale",  "display_name": "Escala de Grises",    "description": "Convierte la imagen a blanco y negro con pesos de luminancia"},
    {"name": "emboss",     "display_name": "Relieve",             "description": "Efecto artístico 3D usando kernel diagonal de convolución"},
    {"name": "ups_frame",  "display_name": "Marco UPS",           "description": "Marco institucional UPS azul #123672 y dorado #F7BF1A con logo"},
]


class ProcessRequest(BaseModel):
    image_url: str
    filter_name: str
    processing_id: _uuid.UUID


def _image_to_numpy(file_bytes: bytes) -> np.ndarray:
    img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
    return np.array(img, dtype=np.uint8)


def _numpy_to_base64(arr: np.ndarray) -> str:
    img = Image.fromarray(arr.astype(np.uint8))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return base64.b64encode(buf.getvalue()).decode("utf-8")


async def _download_image(url: str) -> bytes:
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.get(url)
        if response.status_code != 200:
            raise HTTPException(
                status_code=400,
                detail=f"No se pudo descargar la imagen (HTTP {response.status_code})",
            )
        return response.content


def _cuda_info() -> tuple[str | None, float]:
    try:
        v = cuda.get_version()
        version = f"{v[0]}.{v[1]}"
        free_mem, total_mem = cuda.mem_get_info()
        used_mb = round((total_mem - free_mem) / (1024 * 1024), 2)
    except Exception:
        version, used_mb = None, 0.0
    return version, used_mb


@router.get("/filters")
def list_filters():
    return {"filters": _FILTER_INFO}


@router.post("/process")
async def process_image(request: ProcessRequest):
    """Endpoint principal — el backend Java llama aquí con image_url + filter_name."""
    if request.filter_name not in _FILTERS:
        raise HTTPException(
            status_code=400,
            detail=f"Filtro '{request.filter_name}' no existe. Disponibles: {list(_FILTERS.keys())}",
        )

    file_bytes = await _download_image(request.image_url)

    try:
        img_array = _image_to_numpy(file_bytes)
    except Exception:
        raise HTTPException(status_code=400, detail="No se pudo leer la imagen descargada.")

    height, width, _ = img_array.shape
    memory_transferred_mb = round((width * height * 3) / (1024 * 1024), 4)

    try:
        result_array, metrics = _FILTERS[request.filter_name].apply(img_array)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error en el kernel CUDA: {str(e)}\n{traceback.format_exc()}",
        )

    cuda_version, gpu_memory_used_mb = _cuda_info()

    metrics.update({
        "filter_name":           request.filter_name,
        "image_width":           width,
        "image_height":          height,
        "memory_transferred_mb": memory_transferred_mb,
        "gpu_memory_used_mb":    gpu_memory_used_mb,
        "cuda_version":          cuda_version,
        "status":                "success",
    })

    return JSONResponse({
        "processed_image_base64": _numpy_to_base64(result_array),
        "metrics":                metrics,
    })


@router.post("/filter/{filter_name}")
async def apply_filter(filter_name: str, file: UploadFile = File(...)):
    """Endpoint de prueba local con multipart — útil para Postman/curl sin backend."""
    if filter_name not in _FILTERS:
        raise HTTPException(status_code=404, detail=f"Filtro '{filter_name}' no existe.")

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(status_code=400, detail="El archivo está vacío.")

    try:
        img_array = _image_to_numpy(file_bytes)
    except Exception:
        raise HTTPException(status_code=400, detail="Envía PNG o JPEG.")

    height, width, _ = img_array.shape

    try:
        result_array, metrics = _FILTERS[filter_name].apply(img_array)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error CUDA: {str(e)}")

    return JSONResponse({
        "status":       "completed",
        "filter":       filter_name,
        "image_width":  width,
        "image_height": height,
        "image_base64": _numpy_to_base64(result_array),
        "metrics":      metrics,
    })
