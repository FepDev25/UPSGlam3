from fastapi import FastAPI

from api.routes import router

app = FastAPI(
    title="UPSGlam3 GPU Service",
    version="1.0.0",
    description="Servicio de procesamiento de imágenes con CUDA/PyCUDA — Universidad Politécnica Salesiana",
)

app.include_router(router)


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "gpu-service"}
