#include "mandelbrot.hpp"
#include <stdlib.h>
#include <stdio.h>

class Complex {
public:
double r, i;
    __device__ Complex(double r, double i) : r(r), i(i) {

    }
    __device__ Complex operator*(const Complex& other) {
        return Complex(this->r * other.r - this->i * other.i, this->r * other.i + this->i * other.r);
    }
    __device__ Complex operator+(const Complex& other) {
        return Complex(this->r + other.r, this->i + other.i);
    }
    __device__ void operator=(const Complex& other) {
        this->r = other.r;
        this->i = other.i;
    }
    __device__ double dist_squared() {
        return this->i * this->i + this->r * this->r;
    }
};

typedef struct {
    unsigned char r, g, b;
} Pixel;

extern "C" {
    __device__ int getX() {
        return blockIdx.x * blockDim.x + threadIdx.x;
    }

    __device__ int getY() {
        return blockIdx.y * blockDim.y + threadIdx.y;
    }

    __device__ int getIndex2D(int x, int y) {
        return x + y * gridDim.x * blockDim.x;
    }

    __device__ int getWidth() {
        return gridDim.x * blockDim.x;
    }

    __device__ int getHeight() {
        return gridDim.y * blockDim.y;
    }

    __device__ Pixel get_color(char* buffer, int x, int y) {
        Pixel p;
        memcpy(&p, &buffer[getIndex2D(x, y) * 3], sizeof(Pixel));
        return p;
    }

    __device__ void set_color(char* buffer, int x, int y, Pixel p) {
        memcpy(&buffer[getIndex2D(x, y) * 3], &p, sizeof(Pixel));
    }

    __device__ int stability(double xc, double yc, int iterations) {
        Complex c(xc, yc);
        Complex p(0.0, 0.0);
        for (int i = 0; i < iterations; i++) {
            p = p * p + c;
            if (p.dist_squared() > 4.0) return i;
        }
        return -1;
    }

    __global__ void mandelbrot(char *dest, FrameParams *params, unsigned char *colors) {
        const int x = getX();
        const int y = getY();
        const int i = getIndex2D(x, y);

        const double width = gridDim.x * blockDim.x;
        const double height = gridDim.y * blockDim.y;
        const double max_dim = width > height ? width : height;
        const double cx = (x - width / 2) / (0.5 * params->zoom * max_dim) + params->x;
        const double cy = (y - height / 2) / (0.5 * params->zoom * max_dim) + params->y;

        int s = stability(cx, cy, params->iterations);

        int index;
        if (s == -1) {
            index = 0;
        } else {
            index = s % 255;
        }

        dest[i * 3] = colors[index * 3];
        dest[i * 3 + 1] = colors[index * 3 + 1];
        dest[i * 3 + 2] = colors[index * 3 + 2];
    }

    __global__ void blur(char* dest, char *src, int r) {
        const static double filter[3][3] = {
            {1.0 / 96, 2.0 / 96, 1.0 / 96},
            {2.0 / 96, 84.0 / 96, 2.0 / 96},
            {1.0 / 96, 2.0 / 96, 1.0 / 96}
        };

        const int x = getX();
        const int y = getY();
        const int i = getIndex2D(x, y);

        const double width = gridDim.x * blockDim.x;
        const double height = gridDim.y * blockDim.y;

        Pixel p;
        p.r = 0;
        p.g = 0;
        p.b = 0;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                if (x + dx >= 0 && x + dx < width && y + dy >= 0 && y + dy < height) {
                    Pixel p2 = get_color(src, x + dx, y + dy);
                    p.r += p2.r * filter[dx + r][dy + r];
                    p.g += p2.g * filter[dx + r][dy + r];
                    p.b += p2.b * filter[dx + r][dy + r];
                }
            }
        }

        set_color(dest, x, y, p);
    }

    void render_frame(FrameParams *frame, Render *render, GraphicMemory *graphic_memory) {
        dim3 block(render->block_width, render->block_height);
        dim3 grid(render->grid_width, render->grid_height);
        cudaMemcpy(graphic_memory->frame, frame, sizeof(FrameParams), cudaMemcpyHostToDevice);
        mandelbrot<<<grid, block>>>(graphic_memory->buffer, graphic_memory->frame, graphic_memory->colors);
        blur<<<grid, block>>>(graphic_memory->buffer2, graphic_memory->buffer, 1);
        cudaMemcpy(render->buffer, graphic_memory->buffer2, graphic_memory->size, cudaMemcpyDeviceToHost);
    }

    GraphicMemory *create_graphic_memory(Render *render) {
        printf("Creating graphic memory\n");
        GraphicMemory *graphic_memory = (GraphicMemory*) malloc(sizeof(GraphicMemory));
        size_t size = sizeof(char) * render->grid_height * render->grid_width * render->block_width * render->block_height * 3;
        graphic_memory->size = size;
        if (cudaMalloc(&graphic_memory->buffer, size) != cudaSuccess) {
            printf("Error: %s", cudaGetErrorString(cudaGetLastError()));
            return nullptr;
        }
        if (cudaMalloc(&graphic_memory->buffer2, size) != cudaSuccess) {
            printf("Error: %s", cudaGetErrorString(cudaGetLastError()));
            return nullptr;
        }
        if (cudaMalloc(&graphic_memory->colors, sizeof(char) * 256 * 3) != cudaSuccess) {
            printf("Error: %s", cudaGetErrorString(cudaGetLastError()));
            return nullptr;
        }
        if (cudaMalloc(&graphic_memory->frame, sizeof(FrameParams)) != cudaSuccess) {
            printf("Error: %s", cudaGetErrorString(cudaGetLastError()));
            return nullptr;
        }
        printf("Allocated graphic memory\n");
        cudaMemcpy(graphic_memory->colors, render->colors, sizeof(char) * 256 * 3, cudaMemcpyHostToDevice);
        return graphic_memory;
    }

    void free_graphic_memory(GraphicMemory *graphic_memory) {
        printf("Freeing graphic memory\n");
        cudaFree(graphic_memory->buffer);
        cudaFree(graphic_memory->buffer2);
        cudaFree(graphic_memory->colors);
        cudaFree(graphic_memory->frame);
        free(graphic_memory);
    }

    Render* create_render(int block_width, int block_height, int grid_width, int grid_height) {
        printf("Creating render\n");
        Render *render = (Render*) malloc(sizeof(Render));
        render->block_width = block_width;
        render->block_height = block_height;
        render->grid_width = grid_width;
        render->grid_height = grid_height;
        render->buffer = (char*) malloc(sizeof(char) * block_width * block_height * grid_width * grid_height * 3);
        render->colors = (unsigned char*) malloc(sizeof(char) * 256 * 3);
        if (render->buffer == nullptr || render->colors == nullptr) {
            return nullptr;
        }
        return render;
    }

    void free_render(Render *render) {
        printf("Freeing render\n");
        free(render->buffer);
        free(render->colors);
        free(render);
    }


    char* copy_preview(Render *render) {
        size_t size = sizeof(char) * render->block_width * render->block_height * render->grid_width * render->grid_height * 3;
        char *preview = (char*) malloc(size);
        memcpy(preview, render->buffer, size);
        return preview;
    }

    void free_preview(char *preview) {
        free(preview);
    }
}

extern "C" int assert_42_value() {
    return 42;
}