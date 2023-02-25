package com.skdziwak.mandelbrot
import java.awt.Color

class Brightness(val value: Int) {
    def this() = this(0)

    if (value < 0 || value > 255) {
        throw new IllegalArgumentException("Brightness must be between 0 and 255")
    }
}

object Brightness {
    val MIN: Brightness = Brightness(0)
    val MAX: Brightness = Brightness(255)
}

trait ColorScheme {
    def getColor(brightness: Brightness): Color
    def apply(brightness: Int): Color = getColor(Brightness(brightness))
}

object ColorScheme {
    val BLACK_WHITE = new GradientColorScheme(
        Color(0, 0, 0),
        Color(255, 255, 255),
        Color(0, 0, 0)
    )
    val TWILIGHT = new GradientColorScheme(
        Color(225, 216, 225),
        Color(107, 141, 191),
        Color(48, 19, 55),
        Color(163, 65, 79),
        Color(225, 216, 225),
    )
    val BONE: GradientColorScheme = new GradientColorScheme("#000000;#16161e;#2d2d3e;#42425d;#595c79;#707b90;#869aa6;#9db9bc;#b9d2d2;#dde9e9").toCyclicColorScheme
    val COOL: GradientColorScheme = new GradientColorScheme("#00ffff;#19e6ff;#33ccff;#4cb3ff;#6699ff;#807fff;#9966ff;#b34cff;#cc33ff;#e619ff").toCyclicColorScheme
    val WISTIA: GradientColorScheme = new GradientColorScheme("#e4ff7a;#eff654;#faed2d;#ffe015;#ffce0a;#ffbd00;#ffb100;#ffa600;#fe9900;#fd8c00").toCyclicColorScheme
    val HOT: GradientColorScheme = new GradientColorScheme("#0b0000;#4c0000;#900000;#d20000;#ff1700;#ff5c00;#ff9d00;#ffe100;#ffff36;#ffff9d").toCyclicColorScheme
    val AFMHOT: GradientColorScheme = new GradientColorScheme("#000000;#320000;#660000;#981800;#cc4d00;#ff8001;#ffb233;#ffe667;#ffff99;#ffffcd").toCyclicColorScheme
    val GIST_HEAT: GradientColorScheme = new GradientColorScheme("#000000;#260000;#4d0000;#720000;#990000;#c00100;#e53300;#ff6700;#ff9933;#ffcd9b").toCyclicColorScheme
    val COPPER: GradientColorScheme = new GradientColorScheme("#000000;#1f140c;#3f2819;#5e3b26;#7e5033;#9e6440;#bd784c;#dd8c59;#fc9f65;#ffb472").toCyclicColorScheme
}

class GradientColorScheme(val colors: List[Color]) extends ColorScheme {
    if (colors.size < 2) {
        throw new IllegalArgumentException("At least two colors are required")
    }
    private val sectionWidth: Int = 255 / (colors.size - 1)
    private val gradient: List[Color] = (0 to 256).map(x => {
        x match
            case 0 => colors.head
            case 255 => colors.last
            case _ => {
                if (x % sectionWidth == 0) {
                    colors(x / sectionWidth)
                } else if (x / sectionWidth == colors.size - 1) {
                    colors.last
                } else {
                    val section = x / sectionWidth
                    val sectionStart = colors(section)
                    val sectionEnd = colors(section + 1)
                    val sectionProgress = x % sectionWidth
                    val sectionProgressRatio = sectionProgress.toDouble / sectionWidth.toDouble
                    val red = sectionStart.getRed + (sectionEnd.getRed - sectionStart.getRed) * sectionProgressRatio
                    val green = sectionStart.getGreen + (sectionEnd.getGreen - sectionStart.getGreen) * sectionProgressRatio
                    val blue = sectionStart.getBlue + (sectionEnd.getBlue - sectionStart.getBlue) * sectionProgressRatio
                    Color(red.toInt, green.toInt, blue.toInt)
                }
            }
    }).toList

    def this(colors: Color*) = this(colors.toList)

    def this(str: String) = this(str.split(";").map(_.trim).map(Color.decode).toList)

    def getColor(brightness: Brightness): Color = {
        gradient(brightness.value)
    }

    def toCyclicColorScheme: GradientColorScheme = {
        val cyclicColors = colors ++ colors.reverse.tail
        GradientColorScheme(cyclicColors)
    }
}