fn main() {
    println!("cargo:rerun-if-changed=src/mandelbrot.h");
    println!("cargo:rerun-if-changed=build.rs");

    // Find CUDA
    let cuda_path = std::env::var("CUDA_PATH").unwrap_or("/usr/local/cuda".to_string());

    // Generate bindings
    let bindings = bindgen::Builder::default()
        .header("src/mandelbrot.hpp")
        .detect_include_paths(true)
        .parse_callbacks(Box::new(bindgen::CargoCallbacks))
        .generate()
        .expect("Unable to generate bindings");

    let out_path = std::path::PathBuf::from(std::env::var("OUT_DIR").unwrap());
    std::fs::create_dir_all(&out_path).unwrap();
    let bindings_path = out_path.join("mandelbrot.rs");
    eprintln!("BUILD: Writing bindings to {}", bindings_path.display());
    bindings
        .write_to_file(&bindings_path)
        .expect("Couldn't write bindings!");

    // Compile the CUDA code
    println!("cargo:rerun-if-changed=src/mandelbrot.cu");
    let out_dir = std::env::var("OUT_DIR").unwrap();
    let out_file = format!("{}/libmandelbrotcuda.so", out_dir);
    let nvcc_path = format!("{}/bin/nvcc", cuda_path);
    eprintln!("BUILD: Compiling CUDA code with {}", nvcc_path);
    let output = std::process::Command::new(nvcc_path)
        .args([
            "--compiler-options",
            "-fPIC",
            "-o",
            &out_file,
            "--shared",
            "src/mandelbrot.cu",
        ])
        .output()
        .expect("Failed to execute nvcc");
    if !output.status.success() {
        panic!("nvcc failed: {}", String::from_utf8_lossy(&output.stderr));
    }

    // Link built CUDA code
    println!("cargo:rustc-link-search={}", out_dir);
    println!("cargo:rustc-link-lib=dylib=mandelbrotcuda");

    // Copy the generated CUDA library to the target directory
    let profile = std::env::var("PROFILE").expect("PROFILE not set");
    let target_dir = match profile.as_str() {
        "debug" => "target/debug",
        "release" => "target/release",
        _ => panic!("Unknown profile"),
    };
    let target_file = format!("{}/libmandelbrotcuda.so", target_dir);
    eprintln!("BUILD: Copying {} to {}", out_file, target_file);
    std::fs::copy(out_file, target_file).unwrap();

    // Generate header file using cbindgen
    let crate_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
    let header_path = format!("{}/mandelbrot.h", target_dir);
    eprintln!("BUILD: Generating header file {}", header_path);
    let config = cbindgen::Config::from_file(format!("{}/cbindgen.toml", crate_dir)).unwrap();
    cbindgen::Builder::new()
        .with_crate(crate_dir)
        .with_config(config)
        .generate()
        .expect("Unable to generate header")
        .write_to_file(header_path);
}
