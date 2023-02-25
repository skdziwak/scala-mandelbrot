#![allow(non_camel_case_types)]
#![allow(non_snake_case)]
#![allow(non_upper_case_globals)]
#![allow(dead_code)]

use crate::errors::LibError;
use crate::Color;
include!(concat!(env!("OUT_DIR"), "/mandelbrot.rs"));

#[derive(Debug)]
pub struct RenderWrapper {
    pub render: *mut Render,
    pub graphic_memory: GraphicMemoryWrapper,
    buffer_size: usize,
}

#[derive(Debug)]
pub struct GraphicMemoryWrapper {
    pub graphic_memory: *mut GraphicMemory,
}

pub struct Size {
    pub grid_width: i32,
    pub grid_height: i32,
    pub block_width: i32,
    pub block_height: i32,
}

impl Size {
    // Some intuitive constructor
    pub fn new(
        width: i32,
        height: i32,
        block_width: i32,
        block_height: i32,
    ) -> Result<Self, LibError> {
        if width % block_width != 0 || height % block_height != 0 {
            return Err(LibError::new(
                format!("Width and height must be divisible by block width and height ({}, {})", block_width, block_height),
            ));
        }
        Ok(Self {
            grid_width: width / block_width,
            grid_height: height / block_height,
            block_width,
            block_height,
        })
    }
}

impl RenderWrapper {
    pub fn new(size: Size, colors: Vec<Color>) -> Result<Self, LibError> {
        if colors.len() != 256 {
            return Err(LibError::from_str("Colors must be 256"));
        }
        let pointer = unsafe {
            create_render(
                size.block_width,
                size.block_height,
                size.grid_width,
                size.grid_height,
            )
        };
        if pointer.is_null() {
            return Err(LibError::from_str("Failed to create render"));
        }
        unsafe {
            std::ptr::copy(
                colors.as_ptr().cast::<u8>(),
                (*pointer).colors,
                colors.len() * 3,
            );
        }
        let buffer_size =
            (size.block_height * size.block_width * size.grid_width * size.grid_height) as usize
                * 3;
        Ok(Self {
            render: pointer,
            graphic_memory: GraphicMemoryWrapper::new(pointer)?,
            buffer_size,
        })
    }

    pub fn render_frame(&mut self, frame: &FrameTemplate) {
        let mut params = FrameParams {
            x: frame.x,
            y: frame.y,
            zoom: frame.zoom,
            iterations: frame.max_iterations,
        };
        let params_ptr = std::ptr::addr_of_mut!(params);
        unsafe { render_frame(params_ptr, self.render, self.graphic_memory.graphic_memory) }
    }

    pub fn buffer(&self) -> &[u8] {
        let buffer_ptr = unsafe { (*self.render).buffer as *const u8 };
        unsafe { std::slice::from_raw_parts(buffer_ptr, self.buffer_size) }
    }
}

pub struct FrameTemplate {
    pub x: f64,
    pub y: f64,
    pub zoom: f64,
    pub max_iterations: i32,
}

impl Drop for RenderWrapper {
    fn drop(&mut self) {
        unsafe { free_render(self.render) }
    }
}

impl GraphicMemoryWrapper {
    fn new(render: *mut Render) -> Result<Self, LibError> {
        let pointer = unsafe { create_graphic_memory(render) };
        if pointer.is_null() {
            return Err(LibError::from_str("GraphicMemory is null"));
        }
        Ok(Self {
            graphic_memory: pointer,
        })
    }
}

impl Drop for GraphicMemoryWrapper {
    fn drop(&mut self) {
        unsafe { free_graphic_memory(self.graphic_memory) }
    }
}

#[test]
fn test_lib() {
    let v = unsafe { assert_42_value() };
    assert_eq!(v, 42);
}
