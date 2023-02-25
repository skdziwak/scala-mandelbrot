package com.skdziwak.mandelbrot

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

case class MandelbrotTemplate(
                                 width: Int,
                                 height: Int,
                                 offsetX: Double,
                                 offsetY: Double,
                                 zoom: Double,
                                 maxIterations: Int,
                                 colorScheme: ColorScheme) {
    def this(width: Int, height: Int, offsetX: Double, offsetY: Double, zoom: Double, maxIterations: Int) = {
        this(width, height, offsetX, offsetY, zoom, maxIterations, ColorScheme.BLACK_WHITE)
    }

    def color(colorScheme: ColorScheme): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def size(width: Int, height: Int): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def xSize(width: Int): MandelbrotTemplate = {
        size(width, height)
    }

    def ySize(height: Int): MandelbrotTemplate = {
        size(width, height)
    }

    def offset(offsetX: Double, offsetY: Double): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def xOffset(offsetX: Double): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def yOffset(offsetY: Double): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def zoom(zoom: Double): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom * this.zoom, maxIterations, colorScheme)
    }

    def width(width: Int): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def height(height: Int): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def maxIterations(maxIterations: Int): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }
    
    def absoluteZoom(zoom: Double): MandelbrotTemplate = {
        MandelbrotTemplate(width, height, offsetX, offsetY, zoom, maxIterations, colorScheme)
    }

    def size(size: Int): MandelbrotTemplate = {
        this.size(size, size)
    }

    def goTo(x: Int, y: Int): MandelbrotTemplate = {
        val maxDim = Math.max(width, height)
        val relativeX = offsetX + (x - width / 2) / (0.5 * zoom * maxDim)
        val relativeY = offsetY + (y - height / 2) / (0.5 * zoom * maxDim)
        MandelbrotTemplate(width, height, relativeX, relativeY, zoom, maxIterations, colorScheme)
    }

    def save(file: String): Unit = {
        val image = MandelbrotRenderer.render(this)
        ImageIO.write(image, "png", new File(file))
    }
}

object MandelbrotTemplate {
    val Default: MandelbrotTemplate = MandelbrotTemplate(1024, 1024, -0.5, 0, 1, 1000, ColorScheme.BLACK_WHITE)
}

object MandelbrotRenderer {
    def save(template: MandelbrotTemplate, file: String): Unit = {
        val image = render(template)
        ImageIO.write(image, "png", new File(file))
    }
    def render(template: MandelbrotTemplate): BufferedImage = {
        val image = new BufferedImage(template.width, template.height, BufferedImage.TYPE_INT_RGB)
        val g = image.getGraphics
        val maxIterations = template.maxIterations
        val zoom = template.zoom
        val offsetX = template.offsetX
        val offsetY = template.offsetY
        val maxDim = Math.max(template.width, template.height)
        for (y <- 0 until template.height) {
            for (x <- 0 until template.width) {
                val c = ComplexNumber(
                    (x - template.width / 2) / (0.5 * zoom * maxDim) + offsetX,
                    (y - template.height / 2) / (0.5 * zoom * maxDim) + offsetY
                )
                var z = ComplexNumber(0, 0)
                var iterations = 0
                while (z.abs < 4 && iterations < maxIterations) {
                    z = z * z + c
                    iterations += 1
                }
                val brightness = if (iterations < maxIterations) {
                    iterations % 256
                } else {
                    0
                }
                val color = template.colorScheme.getColor(Brightness(brightness))
                g.setColor(color)
                g.drawLine(x, y, x, y)
            }

            // Print progress bar
            val progress = (y.toDouble / template.height.toDouble * 100).toInt
            val bar = (0 until progress).map(_ => "=").mkString
            val spaces = (0 until (100 - progress)).map(_ => " ").mkString
            print(s"\r[ $bar$spaces ] $progress%")
        }
        println()
        image
    }
}


