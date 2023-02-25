package com.skdziwak.mandelbrot

import gui.MandelbrotGui

import java.awt.Color
import javax.imageio.ImageIO
import javax.swing.{UIManager, UnsupportedLookAndFeelException}

@main
def main(): Unit = {
    try {
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")
    } catch {
        case e: UnsupportedLookAndFeelException => e.printStackTrace()
        case e: ClassNotFoundException => e.printStackTrace()
        case e: InstantiationException => e.printStackTrace()
        case e: IllegalAccessException => e.printStackTrace()
    }
    val gui = new MandelbrotGui
    gui.show()
}
