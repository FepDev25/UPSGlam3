import time

import numpy as np
import pycuda.autoinit  # noqa: F401
import pycuda.driver as cuda
from pycuda.compiler import SourceModule

_KERNEL = """
__global__ void media(unsigned char *in_img, unsigned char *out_img,
                      int width, int height, int channels) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= width || y >= height) return;

    if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
        for (int c = 0; c < channels; c++)
            out_img[(y * width + x) * channels + c] =
                in_img[(y * width + x) * channels + c];
        return;
    }

    for (int c = 0; c < channels; c++) {
        int sum = 0;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                sum += in_img[((y+dy)*width + (x+dx))*channels + c];
        out_img[(y*width + x)*channels + c] = (unsigned char)(sum / 9);
    }
}
"""

_mod = SourceModule(_KERNEL)
_kernel_fn = _mod.get_function("media")

_BLOCK = (16, 16, 1)


def apply(image: np.ndarray) -> tuple[np.ndarray, dict]:
    img = np.ascontiguousarray(image, dtype=np.uint8)
    height, width, channels = img.shape
    result = np.empty_like(img)

    grid_x = (width + _BLOCK[0] - 1) // _BLOCK[0]
    grid_y = (height + _BLOCK[1] - 1) // _BLOCK[1]

    d_in = cuda.mem_alloc(img.nbytes)
    d_out = cuda.mem_alloc(result.nbytes)

    t0 = time.perf_counter()
    cuda.memcpy_htod(d_in, img)

    start_evt = cuda.Event()
    end_evt = cuda.Event()
    start_evt.record()
    _kernel_fn(
        d_in, d_out,
        np.int32(width), np.int32(height), np.int32(channels),
        block=_BLOCK, grid=(grid_x, grid_y),
    )
    end_evt.record()
    end_evt.synchronize()

    cuda.memcpy_dtoh(result, d_out)
    total_ms = (time.perf_counter() - t0) * 1000.0

    d_in.free()
    d_out.free()

    return result, {
        "kernel_time_ms": round(start_evt.time_till(end_evt), 4),
        "total_time_ms": round(total_ms, 4),
        "block_dim_x": _BLOCK[0],
        "block_dim_y": _BLOCK[1],
        "grid_dim_x": grid_x,
        "grid_dim_y": grid_y,
        "total_threads": grid_x * _BLOCK[0] * grid_y * _BLOCK[1],
    }
