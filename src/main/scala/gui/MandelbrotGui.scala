package com.skdziwak.mandelbrot
package gui
import java.awt.Color
import java.awt.event.{ComponentAdapter, InputEvent}
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.JLabel
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.swing.*
import scala.swing.event.{ColorChanged, ListSelectionChanged, MouseClicked, UIElementResized}

class MandelbrotGui {
    private val componentsToSwitch: ListBuffer[Component] = new ListBuffer[Component]()
    val renderPanel = new RenderPanel(this)
    val controlPanel = new ControlPanel(this)
    private val statusBar = new Label("")
    val frame = new MainFrame {
        title = "Mandelbrot"
        menuBar = new MenuBar {
            contents += new Menu("File") {
                contents += new MenuItem(Action("Exit") {
                    sys.exit(0)
                })
            }
        }
        contents = new BorderPanel {
            preferredSize = new Dimension(1648, 800)
            add(renderPanel, BorderPanel.Position.Center)
            add(controlPanel, BorderPanel.Position.West)
            add(statusBar, BorderPanel.Position.South)
        }
        centerOnScreen()
    }

    def show(): Unit = frame.visible = true

    def message(msg: String): Unit = {
        statusBar.text = msg
    }
    def addComponentToSwitch(component: Component): Unit = {
        componentsToSwitch += component
    }

    def disableSwitchableComponents(): Unit = {
        componentsToSwitch.foreach(_.enabled = false)
    }

    def enableSwitchableComponents(): Unit = {
        componentsToSwitch.foreach(_.enabled = true)
    }
}

class ControlPanel(gui: MandelbrotGui) extends BorderPanel {
    preferredSize = new Dimension(200, 600)
    val iterationsField: TextField = new TextField("1000") {
        columns = 5
    }
    val widthField: TextField = new TextField("1920") {
        columns = 5
    }
    val heightField: TextField = new TextField("1024") {
        columns = 5
    }
    val outputField: TextField = new TextField("output/render_$.mp4") {
        columns = 10
    }
    val zoomMultiplierField: TextField = new TextField("1.04") {
        columns = 5
    }
    val endFramesField: TextField = new TextField("100") {
        columns = 5
    }
    private val colorSchemeEditor = new ColorSchemeEditor(gui)
    private val controls = new BoxPanel(Orientation.Vertical) {
        // Width
        contents += new Label("Width:") {
            border = Swing.EmptyBorder(0, 0, 5, 0)
        }
        contents += widthField
        gui addComponentToSwitch widthField
        // Height
        contents += new Label("Height:") {
            border = Swing.EmptyBorder(0, 0, 5, 0)
        }
        contents += heightField
        gui addComponentToSwitch heightField
        // Iterations
        contents += new Label("Iterations:") {
            border = Swing.EmptyBorder(0, 0, 5, 0)
        }
        contents += iterationsField
        gui addComponentToSwitch iterationsField
        // Output file
        contents += new Label("Output file:") {
            border = Swing.EmptyBorder(0, 0, 5, 0)
        }
        contents += outputField
        gui addComponentToSwitch outputField
        // Zoom multiplier
        contents += new Label("Zoom multiplier:") {
            border = Swing.EmptyBorder(0, 0, 5, 0)
        }
        contents += zoomMultiplierField
        gui addComponentToSwitch zoomMultiplierField
        // End frames
        contents += new Label("End frames:") {
            border = Swing.EmptyBorder(0, 0, 5, 0)
        }
        contents += endFramesField
        gui addComponentToSwitch endFramesField
        // Color scheme
        contents += new Label("Color scheme:") {
            border = Swing.EmptyBorder(0, 0, 5, 0)
        }
        contents += colorSchemeEditor

        border = Swing.EmptyBorder(5, 5, 5, 5)
    }
    private val buttons = new GridBagPanel {
        add(new Button("Render") {
            gui addComponentToSwitch this
            action = Action("Render") {
                gui.message("Rendering...")
                try {
                    gui.renderPanel.renderAgain()
                    val renderer = new InteractiveRenderer(gui)
                    val framesToRender = renderer.calculateFrames()
                    val dialog = new YesNoModalDialog(gui, "Render " + framesToRender + " frames?", value => {
                        if (value) {
                            renderer.render()
                            gui.message("Rendering complete.")
                        }
                    })
                    dialog.open()
                } catch {
                    case e: Exception => {
                        gui.message("Error: " + e.getMessage)
                        e.printStackTrace()
                    }
                }
            }
        }, new Constraints {
            gridx = 0
            gridy = 0
            weightx = 1
            fill = GridBagPanel.Fill.Horizontal
        })
        add(new Button("Reset") {
            gui addComponentToSwitch this
            action = Action("Reset") {
                gui.renderPanel.restoreDefaultTemplate()
                gui.renderPanel.renderAgain()
            }
        }, new Constraints {
            gridx = 0
            gridy = 1
            weightx = 1
            fill = GridBagPanel.Fill.Horizontal
        })
        add(new Button("Update") {
            gui addComponentToSwitch this
            action = Action("Update") {
                try {
                    val iterations = iterationsField.text.toInt
                    gui.renderPanel.template = gui.renderPanel.template maxIterations iterations
                    gui.renderPanel.renderAgain()
                } catch {
                    case e: Exception => {
                        gui.message("Error: " + e.getMessage)
                        e.printStackTrace()
                    }
                }
            }
        }, new Constraints {
            gridx = 0
            gridy = 2
            weightx = 1
            fill = GridBagPanel.Fill.Horizontal
        })

        border = Swing.EmptyBorder(5, 5, 5, 5)
    }
    add(controls, BorderPanel.Position.North)
    add(buttons, BorderPanel.Position.South)
}

case class ColorSchemaElement(name: String, color: ColorScheme) {
    override def toString: String = name
}

class CustomColorScheme(val gui: MandelbrotGui) extends ColorScheme {
    private var colorScheme: Option[ColorScheme] = None
    override def getColor(brightness: Brightness): Color = {
        colorScheme match {
            case Some(scheme) => scheme.getColor(brightness)
            case None => ColorScheme.BLACK_WHITE.getColor(brightness)
        }
    }

    def showDialog(callback: Runnable): Unit = {
        println("Showing dialog")
        val dialog = new Dialog(gui.frame) {
            title = "Custom color scheme"
            modal = true
            contents = new BoxPanel(Orientation.Vertical) {
                contents += new Label("Select a color scheme:")
                // Add text field
                private val textField = new TextField("#000000;#FFFFFF;#FF0000") {
                    columns = 10
                }
                contents += textField
                // Add button
                contents += new Button("Select") {
                    action = Action("Select") {
                        try {
                            val scheme = new GradientColorScheme(textField.text).toCyclicColorScheme
                            colorScheme = Some(scheme)
                            callback.run()
                        } catch {
                            case e: NumberFormatException => {
                                gui.message("Error: " + e.getMessage)
                                e.printStackTrace()
                            }
                        }
                        close()
                    }
                }

            }
        }
        dialog.pack()
        dialog.centerOnScreen()
        dialog.open()
    }
}

class ColorSchemeEditor(val gui: MandelbrotGui) extends BoxPanel(Orientation.Vertical) {
    private val colorNames: List[ColorSchemaElement] = List(
        ColorSchemaElement("Black and white", ColorScheme.BLACK_WHITE),
        ColorSchemaElement("Twilight", ColorScheme.TWILIGHT),
        ColorSchemaElement("Bone", ColorScheme.BONE),
        ColorSchemaElement("Cool", ColorScheme.COOL),
        ColorSchemaElement("Wistia", ColorScheme.WISTIA),
        ColorSchemaElement("Hot", ColorScheme.HOT),
        ColorSchemaElement("Afmhot", ColorScheme.AFMHOT),
        ColorSchemaElement("Gist Heat", ColorScheme.GIST_HEAT),
        ColorSchemaElement("Copper", ColorScheme.COPPER),
        ColorSchemaElement("Custom", new CustomColorScheme(gui))
    )

    private val colorList = new ListView[ColorSchemaElement](colorNames) {
        selection.intervalMode = ListView.IntervalMode.Single
        border = Swing.EtchedBorder(Swing.Lowered)
        listenTo(selection)
        reactions += {
            case ListSelectionChanged(_, _, false) => {
                val selected = selection.items
                if (selected.nonEmpty) {
                    val colorScheme = selected.head.color
                    colorScheme match {
                        case scheme: CustomColorScheme =>
                            scheme.showDialog(() => {
                                gui.renderPanel.template = gui.renderPanel.template color scheme
                                gui.renderPanel.renderAgain()
                            })
                        case _ =>
                            gui.renderPanel.template = gui.renderPanel.template color colorScheme
                            gui.renderPanel.renderAgain()
                    }
                }
            }
        }
    }
    gui addComponentToSwitch colorList

    contents += colorList
}


class RenderPanel(gui: MandelbrotGui) extends Panel {
    gui addComponentToSwitch this
    var template: MandelbrotTemplate = defaultTemplate
    var image: Option[BufferedImage] = None
    private def defaultTemplate = MandelbrotTemplate.Default color ColorScheme.BLACK_WHITE zoom 0.4
    def restoreDefaultTemplate(): Unit = {
        template = defaultTemplate
    }
    private def renderImage(): BufferedImage = {
        template = template size(size.width, size.height)
        val result = MandelbrotExport.previewTemplate(template)
        result
    }
    override def paintComponent(g: Graphics2D): Unit = {
        super.paintComponent(g)
        if (image.isEmpty) {
            image = Some(renderImage())
        }
        g.drawImage(image.get, 0, 0, null)
    }

    def renderAgain(): Unit = {
        image = None
        gui.message("Rendering preview...")
        val start = System.nanoTime()
        repaint()
        val nanos = System.nanoTime() - start
        val micros = nanos / 1000
        val currentZoom = template.zoom
        gui.message("Rendering preview complete. Took " + micros + "\u00B5s. Zoom: " + currentZoom + "x")
    }

    listenTo(mouse.clicks)
    reactions += {
        case e: MouseClicked =>
            if (enabled) {
                if (e.peer.getButton == 1) {
                    template = template goTo(e.point.x, e.point.y) zoom 2
                } else {
                    val inverseX = size.width - e.point.x
                    val inverseY = size.height - e.point.y
                    template = template goTo(inverseX, inverseY) zoom 0.5
                }
                renderAgain()
            }
    }
    peer.addComponentListener(new ComponentAdapter {
        override def componentResized(e: java.awt.event.ComponentEvent): Unit = {
            renderAgain()
        }
    })
}

class YesNoModalDialog(gui: MandelbrotGui, message: String, callback: YesNoCallback) extends Dialog(gui.frame) {
    modal = true
    contents = new BoxPanel(Orientation.Vertical) {
        contents += new Label(message)
        contents += new FlowPanel {
            contents += new Button("Yes") {
                action = Action("Yes") {
                    callback.callback(true)
                    close()
                }
            }
            contents += new Button("No") {
                action = Action("No") {
                    callback.callback(false)
                    close()
                }
            }
        }
    }
    pack()
    centerOnScreen()
}

trait YesNoCallback {
    def callback(value: Boolean): Unit
}

class InteractiveRenderer(gui: MandelbrotGui) {
    private val iterations = {
        val text = gui.controlPanel.iterationsField.text
        val value = try {
            text.toInt
        } catch {
            case _: NumberFormatException => throw new IllegalArgumentException("Iterations must be a number.")
        }
        if (value < 1) {
            throw new IllegalArgumentException("Iterations must be at least 1.")
        }
        value
    }
    private val width = {
        val text = gui.controlPanel.widthField.text
        val value = try {
            text.toInt
        } catch {
            case _: NumberFormatException => throw new IllegalArgumentException("Width must be a number.")
        }
        if (value < 1) {
            throw new IllegalArgumentException("Width must be at least 1.")
        }
        value
    }
    private val height = {
        val text = gui.controlPanel.heightField.text
        val value = try {
            text.toInt
        } catch {
            case _: NumberFormatException => throw new IllegalArgumentException("Height must be a number.")
        }
        if (value < 1) {
            throw new IllegalArgumentException("Height must be at least 1.")
        }
        value
    }
    private val outputFile = {
        val text = gui.controlPanel.outputField.text
        if (text.isEmpty) {
            throw new IllegalArgumentException("Output file must be specified.")
        }
        if (!text.endsWith(".mp4")) {
            throw new IllegalArgumentException("Output file must be an mp4 file.")
        }
        // Replace $ with the current date and time
        val date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())
        text.replace("$", date)
    }
    private val zoomMultiplier: Double = {
        val text = gui.controlPanel.zoomMultiplierField.text
        val value = try {
            text.toDouble
        } catch {
            case _: NumberFormatException => throw new IllegalArgumentException("Zoom multiplier must be a number.")
        }
        if (value <= 1) {
            throw new IllegalArgumentException("Zoom multiplier must be greater than 1.")
        }
        value
    }
    private val endFrames: Int = {
        val text = gui.controlPanel.endFramesField.text
        val value = try {
            text.toInt
        } catch {
            case _: NumberFormatException => throw new IllegalArgumentException("End frames must be a number.")
        }
        if (value < 0) {
            throw new IllegalArgumentException("End frames must be at least 0.")
        }
        value
    }
    private val template = gui.renderPanel.template maxIterations iterations
    if (template.zoom <= 0.5) {
        throw new IllegalArgumentException("Zoom must be greater than 0.5.")
    }

    def calculateFrames(): Int = ZoomAnimationWithSlowEnd.calculateFrames(0.5, template, zoomMultiplier, endFrames)

    def render(): Unit = {
        implicit val context = ExecutionContext.global
        gui.disableSwitchableComponents()
        Future {
            gui.message("Rendering...")
            try {
                val start = System.currentTimeMillis()
                val exporter = MandelbrotExport(outputFile, width, height, template.colorScheme)
                val summary = ZoomAnimationWithSlowEnd.animate(0.5, template, exporter, zoomMultiplier, (progress: Int, total: Int) => {
                    gui.message("Rendering... " + progress + "/" + total)
                }, endFrames)
                exporter.close()
                val millis = System.currentTimeMillis() - start
                val seconds: String = f"${millis / 1000.0}%.2f"
                val renderedFrames = summary.totalFrames
                val avgFps = summary.totalFrames / (millis / 1000.0)
                val avgFpsString = f"$avgFps%.2f"
                gui.message("Rendering complete. Took " + seconds + "s. Rendered " + renderedFrames + " frames. Average FPS: " + avgFpsString + ".")
            } catch {
                case e: Exception => {
                    gui.message("Error: " + e.getMessage)
                    e.printStackTrace()
                }
            }
            gui.enableSwitchableComponents()
        }
    }
}