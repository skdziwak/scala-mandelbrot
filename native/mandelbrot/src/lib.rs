#![allow(dead_code)]
#![allow(unsafe_code, clippy::not_unsafe_ptr_arg_deref)]
#![deny(clippy::unwrap_used, clippy::expect_used, clippy::panic)]

extern crate alloc;
extern crate core;

use crate::errors::LibError;
use crate::mandelbrot::{create_graphic_memory, create_render, FrameParams, FrameTemplate, free_graphic_memory, free_render, render_frame, RenderWrapper, Size, copy_preview, free_preview};
use crate::video::Encoder;
use std::path::PathBuf;

mod errors;
mod mandelbrot;
mod video;

macro_rules! log {
    ($($arg:tt)*) => {
        println!($($arg)*);
    }
}

#[no_mangle]
pub extern "C" fn add(left: i32, right: i32) -> i32 {
    left - right
}

#[derive(Debug)]
pub struct Export {
    width: u32,
    height: u32,
    render: RenderWrapper,
    encoder: Encoder,
}

#[derive(Debug)]
#[repr(C)]
pub struct Color {
    r: u8,
    g: u8,
    b: u8,
}

/// Creates a new export.
#[no_mangle]
pub extern "C" fn create_export(
    name: *const i8,
    colors: *const u8,
    width: u32,
    height: u32,
) -> *mut Export {
    errors::handle_errors(
        move || {
            let name = unsafe { std::ffi::CStr::from_ptr(name) }
                .to_str()
                .map_err(|_e| LibError::from_str("Failed to convert name to string"))?;
            let path = PathBuf::from(name);
            let parent_dir = path
                .parent()
                .ok_or_else(|| LibError::from_str("Failed to get parent directory"))?;

            // Create the parent directory if it doesn't exist
            std::fs::create_dir_all(parent_dir)
                .map_err(|_e| LibError::from_str("Failed to create parent directory"))?;

            // Delete the file if it already exists
            std::fs::remove_file(name).unwrap_or(());

            let colors = (0..256)
                .map(|i| {
                    let r: u8 = unsafe { *colors.offset(i * 3) };
                    let g: u8 = unsafe { *colors.offset(i * 3 + 1) };
                    let b: u8 = unsafe { *colors.offset(i * 3 + 2) };
                    Color { r, g, b }
                })
                .collect::<Vec<_>>();

            let export = Export {
                width,
                height,
                render: RenderWrapper::new(
                    Size::new(width as i32, height as i32, 32, 32)?,
                    colors,
                )?,
                encoder: Encoder::new(name, width, height)?,
            };
            log!("Created export: {:?}", export);

            Ok(Box::into_raw(Box::new(export)))
        },
        || {
            // Return null if there was an error
            std::ptr::null_mut()
        },
    )
}

/// Adds a frame to the export.
#[no_mangle]
pub extern "C" fn add_frame(
    export: *mut Export,
    x_offset: f64,
    y_offset: f64,
    zoom: f64,
    max_iterations: u32,
) {
    errors::handle_errors(
        move || {
            let export = unsafe { &mut *export };
            let frame = FrameTemplate {
                x: x_offset,
                y: y_offset,
                zoom,
                max_iterations: max_iterations as i32,
            };
            export.render.render_frame(&frame);
            let buffer = export.render.buffer();

            export.encoder.write(buffer)?;
            Ok(())
        },
        || (),
    )
}

/// Renders a preview frame and returns a pointer to the buffer.
/// Caller is responsible for freeing the buffer.
#[no_mangle]
pub extern "C" fn preview_frame(
    colors: *const u8,
    width: u32,
    height: u32,
    x_offset: f64,
    y_offset: f64,
    zoom: f64,
    max_iterations: u32,
) -> *mut u8 {
    errors::handle_errors(
        move || unsafe {
            let size = Size::new(width as i32, height as i32, 32, 32)?;
            let render = create_render(
                size.block_width as i32,
                size.block_height as i32,
                size.grid_width as i32,
                size.grid_height as i32,
            );
            if render.is_null() {
                return Err(LibError::from_str("Failed to create render"));
            }
            std::ptr::copy(
                colors,
                (*render).colors,
                256 * 3,
            );
            let graphic_memory = create_graphic_memory(render);
            if graphic_memory.is_null() {
                return Err(LibError::from_str("Failed to create graphic memory"));
            }
            let mut params = FrameParams {
                x: x_offset,
                y: y_offset,
                zoom,
                iterations: max_iterations as i32,
            };
            let params_ptr = std::ptr::addr_of_mut!(params);
            render_frame(params_ptr, render, graphic_memory);

            let buffer = copy_preview(render) as *mut u8;

            free_graphic_memory(graphic_memory);
            free_render(render);

            Ok(buffer)
        },
        std::ptr::null_mut,
    )
}

/// Frees preview frame buffer.
#[no_mangle]
pub extern "C" fn free_preview_frame(buffer: *mut u8) {
    errors::handle_errors(
        || unsafe {
            free_preview(buffer as *mut i8);
            Ok(())
        },
        || (),
    )
}

/// Destroys an export.
#[no_mangle]
pub extern "C" fn destroy_export(export: *mut Export) {
    errors::handle_errors(
        || {
            log!("Destroyed export: {:?}", export);
            let mut export = unsafe { Box::from_raw(export) };
            export.encoder.finish()?;
            drop(export);
            Ok(())
        },
        || (),
    )
}

/// Returns the last error message as a null-terminated string.
/// The caller is responsible for freeing the string.
/// Returns null if there is no error.
#[no_mangle]
pub extern "C" fn get_last_error() -> *mut i8 {
    let error = errors::get_last_error();
    match error {
        Some(error) => {
            let c_error = std::ffi::CString::new(error);
            match c_error {
                Ok(c_error) => c_error.into_raw(),
                Err(_e) => {
                    // Failed to convert error to string but we can't return null
                    // so we allocate one zero byte and return that (using box syntax)
                    let byte: Box<i8> = Box::new(0);
                    Box::into_raw(byte)
                }
            }
        }
        None => std::ptr::null_mut(),
    }
}

/// Frees a string returned by `get_last_error`.
#[no_mangle]
pub extern "C" fn free_string(string: *mut i8) {
    if string.is_null() {
        return;
    }
    unsafe {
        let _ = std::ffi::CString::from_raw(string);
    }
}

#[test]
fn test_lib() {
    // 256 colors
    let colors = (0..256)
        .map(|i| {
            let r: u8 = i as u8;
            let g: u8 = i as u8;
            let b: u8 = i as u8;
            Color { r, g, b }
        })
        .collect::<Vec<_>>();
    // convert colors to u8 array (r, g, b) x 256
    let colors = colors
        .iter()
        .flat_map(|c| vec![c.r, c.g, c.b])
        .collect::<Vec<_>>();
    let c_name = std::ffi::CString::new("test").unwrap();
    let export = create_export(c_name.as_ptr(), colors.as_ptr(), 1024, 1024);
    add_frame(export, 0.0, 0.0, 1.0, 100);
    destroy_export(export);
}
