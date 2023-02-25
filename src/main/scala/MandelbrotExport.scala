package com.skdziwak.mandelbrot

import com.sun.jna.{IntegerType, Memory, Pointer}

import java.awt.image.BufferedImage

class MandelbrotExport(private val ptr: Pointer, private val width: Int, private val height: Int) {

    def addFrame(xOffset: Double, yOffset: Double, zoom: Double, iterations: Int): Unit = {
        Native.FFI.add_frame(ptr, xOffset, yOffset, zoom, UnsignedInt(iterations))
    }

    def addFrame(template: MandelbrotTemplate): Unit = {
        addFrame(template.offsetX, template.offsetY, template.zoom, template.maxIterations)
    }

    def close(): Unit = {
        Native.FFI.destroy_export(ptr)
    }
}

object MandelbrotExport {
    def apply(fileName: String, width: Int, height: Int, colorScheme: ColorScheme): MandelbrotExport = {
        val fileNameBytes = fileName.getBytes("UTF-8")
        val fileNamePointer = new Memory(fileNameBytes.length + 1)
        fileNamePointer.write(0, fileNameBytes, 0, fileNameBytes.length)
        fileNamePointer.setByte(fileNameBytes.length, 0.toByte)
        val colorsPointer: Pointer = new Memory(256 * 3)
        for (i <- 0 until 256) {
            colorsPointer.setByte(i * 3, colorScheme(i).getRed.toByte)
            colorsPointer.setByte(i * 3 + 1, colorScheme(i).getGreen.toByte)
            colorsPointer.setByte(i * 3 + 2, colorScheme(i).getBlue.toByte)
        }
        val exportPointer = Native.FFI.create_export(fileNamePointer, colorsPointer, UnsignedInt(width), UnsignedInt(height))
        new MandelbrotExport(exportPointer, width, height)
    }

    def previewTemplate(template: MandelbrotTemplate): BufferedImage = {
        val width = (template.width / 32) * 32
        val height = (template.height / 32) * 32
        val colorsPointer: Pointer = new Memory(256 * 3)
        val colorScheme = template.colorScheme
        for (i <- 0 until 256) {
            colorsPointer.setByte(i * 3, colorScheme(i).getRed.toByte)
            colorsPointer.setByte(i * 3 + 1, colorScheme(i).getGreen.toByte)
            colorsPointer.setByte(i * 3 + 2, colorScheme(i).getBlue.toByte)
        }
        val buffer: Pointer = Native.FFI.preview_frame(
            colorsPointer,
            UnsignedInt(width),
            UnsignedInt(height),
            template.offsetX,
            template.offsetY,
            template.zoom,
            UnsignedInt(template.maxIterations)
        )
        val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x <- 0 until width) {
            for (y <- 0 until height) {
                val red = buffer.getByte(y * width * 3 + x * 3)
                val green = buffer.getByte(y * width * 3 + x * 3 + 1)
                val blue = buffer.getByte(y * width * 3 + x * 3 + 2)
                image.setRGB(x, y, (red << 16) + (green << 8) + blue)
            }
        }
        Native.FFI.free_preview_frame(buffer)
        image
    }
}