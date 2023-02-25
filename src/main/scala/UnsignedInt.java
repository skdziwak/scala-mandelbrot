package com.skdziwak.mandelbrot;

import com.sun.jna.IntegerType;

public class UnsignedInt extends IntegerType {
    public UnsignedInt() {
        this(0);
    }

    public UnsignedInt(long value) {
        super(4, value, true);
    }
}
