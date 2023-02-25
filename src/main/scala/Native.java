package com.skdziwak.mandelbrot;

import com.sun.jna.*;

import java.awt.*;

public class Native {
    private static final RMandelbrot REAL_FFI = (RMandelbrot) com.sun.jna.Native.loadLibrary("mandelbrot", RMandelbrot.class);
    public static final RMandelbrot FFI = new RMandelbrotWrapper(REAL_FFI);
    public interface RMandelbrot extends Library {
        Pointer create_export(Pointer name, Pointer colors, IntegerType width, IntegerType height);
        void add_frame(Pointer p, double xOffset, double yOffset, double zoom, IntegerType maxIterations);
        Pointer preview_frame(Pointer colors, IntegerType width, IntegerType height, double xOffset, double yOffset, double zoom, IntegerType maxIterations);
        void free_preview_frame(Pointer p);
        void destroy_export(Pointer p);
        Pointer get_last_error();
        void free_string(Pointer p);
    }
    private static class RMandelbrotWrapper implements RMandelbrot {
        private final RMandelbrot impl;

        public RMandelbrotWrapper(RMandelbrot impl) {
            this.impl = impl;
        }

        @Override
        public Pointer create_export(Pointer name, Pointer colors, IntegerType width, IntegerType height) {
            Pointer ptr = impl.create_export(name, colors, width, height);
            checkError();
            return ptr;
        }

        @Override
        public void add_frame(Pointer p, double xOffset, double yOffset, double zoom, IntegerType maxIterations) {
            impl.add_frame(p, xOffset, yOffset, zoom, maxIterations);
            checkError();
        }

        @Override
        public void destroy_export(Pointer p) {
            impl.destroy_export(p);
            checkError();
        }
        
        @Override
        public Pointer preview_frame(Pointer colors, IntegerType width, IntegerType height, double xOffset, double yOffset, double zoom, IntegerType maxIterations) {
            Pointer ptr = impl.preview_frame(colors, width, height, xOffset, yOffset, zoom, maxIterations);
            checkError();
            return ptr;
        }

        @Override
        public void free_preview_frame(Pointer p) {
            impl.free_preview_frame(p);
            checkError();
        }

        @Override
        public Pointer get_last_error() {
            throw new UnsupportedOperationException("This method should not be called manually.");
        }

        @Override
        public void free_string(Pointer p) {
            throw new UnsupportedOperationException("This method should not be called manually.");
        }
    }

    public static class NativeException extends RuntimeException {
        public NativeException(String message) {
            super(message);
        }
    }
    private static void checkError() {
        Pointer p = REAL_FFI.get_last_error();
        if (p != null) {
            String s = p.getString(0);
            REAL_FFI.free_string(p);
            throw new NativeException(s);
        }
    }
}
