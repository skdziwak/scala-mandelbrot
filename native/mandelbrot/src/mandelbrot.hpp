#include <stddef.h>

extern "C" {
    typedef struct {
        double x;
        double y;
        double zoom;
        int iterations;
    } FrameParams;

    typedef struct {
        int grid_width;
        int block_width;
        int grid_height;
        int block_height;
        char *buffer; // (R, G, B) * width * height
        unsigned char *colors; // ((R, G, B) * 256)
    } Render;

    typedef struct {
        char *buffer; // (R, G, B) * width * height
        char *buffer2; // (R, G, B) * width * height
        size_t size; // 3 * width * height
        unsigned char *colors; // ((R, G, B) * 256)
        FrameParams *frame;
    } GraphicMemory;

    int assert_42_value();
    Render* create_render(int block_width, int block_height, int grid_width, int grid_height);
    void free_render(Render *render);
    void render_frame(FrameParams *frame, Render *render, GraphicMemory *graphic_memory);
    GraphicMemory *create_graphic_memory(Render *render);
    void free_graphic_memory(GraphicMemory *graphic_memory);
    char* copy_preview(Render *render);
    void free_preview(char *preview);
}
