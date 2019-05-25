import Styles.Companion.controls
import Styles.Companion.rootClass
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.geometry.Orientation
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.control.Slider
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import tornadofx.*
import kotlin.math.absoluteValue
import kotlin.math.pow

fun main() {

    Application.launch(Appl::class.java)
}

fun readColors() = Appl::class.java.getResource("/rgb.txt").readText().lineSequence()
    .map { it.split("\t".toRegex()) }
    .map { a -> a[0] to c(a[1]) }
    .toMap()

fun euclidianDistanceSqByParts(a: Color, b: Color) =
    2 * (a.red - b.red).pow(2) + 4 * (a.green - b.green).pow(2) + 3 * (a.blue - b.blue).pow(2)

fun manhattanDistnaceByParts(a: Color, b: Color) = (a.red - b.red).absoluteValue + (a.green - b.green).absoluteValue +
        (a.blue - b.blue).absoluteValue

fun getClosest(colors: Map<String, Color>, c: Color): Pair<String, Color> {
    val (rn, rc, _) = colors
        .map { (name, co) -> Triple(name, co, euclidianDistanceSqByParts(co, c))}.minBy { it.third }!!
    return rn to rc
}

class Appl : App(MainView::class, Styles::class)

class MainView : View() {
    override val root: Parent = vbox {
            primaryStage.width = 800.0
            primaryStage.height = 600.0
            addClass(rootClass)
            c = canvas {}
            control = hbox {
                addClass(controls)
                hueSlider = slider(0.0..360.0, 0.0, Orientation.HORIZONTAL) {

                }
            }
        }

    private lateinit var c: Canvas
    private lateinit var control: HBox
    private lateinit var hueSlider: Slider
    private val colors: Map<String, Color> = readColors()

   fun bindProps() {
       with(control) {
           prefWidthProperty().bind(primaryStage.widthProperty())
           prefHeight = 100.0
           maxHeight = prefHeight
           minHeight = prefHeight
       }
       with(hueSlider) {
            prefWidthProperty().bind(control.widthProperty())
           prefHeight = 75.0
           maxHeight = prefHeight
           minHeight = prefHeight
       }
       with(c) {
           widthProperty().bind(primaryStage.widthProperty())
           heightProperty().bind(primaryStage.heightProperty().subtract(control.heightProperty()))
       }
   }

    fun draw() {
        val h = hueSlider.value
        val dx = 0.005
        with(c.graphicsContext2D) {
            save()
            scale(c.width, c.height)
            fill = Color.BLACK
            fillRect(0.0, 0.0, canvas.width, canvas.height)
            var s = 0.0
            while(s <= 1.0) {
                var b = 0.0
                while(b <= 1.0) {
                    val color = Color.hsb(h,s,b)
                    val (rn, rc) = getClosest(colors, color)
                    fill = rc
//                    fill = color
                    fillRect(s, b, dx * 1.15, dx * 1.15)
                    b += dx
                }
                s += dx
            }
            restore()
        }

    }

    private var drawRequestTime = -1L
    fun redraw() {
        drawRequestTime = System.currentTimeMillis()
    }

    inner class Drawer : AnimationTimer() {

        override fun handle(now: Long) {
            if(drawRequestTime > 0 && System.currentTimeMillis() - drawRequestTime > 300) {
                drawRequestTime = -1
                draw()
            }
        }

    }

    init {
        bindProps()
        val redrawListener = { _: Any, _: Any, _: Any -> redraw() }
        primaryStage.widthProperty().addListener(redrawListener)
        primaryStage.heightProperty().addListener(redrawListener)
        hueSlider.valueProperty().addListener(redrawListener)
        Drawer().start()
    }
}

class Styles : Stylesheet() {
    companion object {
        val rootClass by cssclass()
        val controls by cssclass()
    }

    init {
        controls {
            val stops = (0..360 step 36).map { it.toDouble() }
                .map { Stop(it / 360, Color.hsb(it, 1.0, 1.0)) }.toList()
            backgroundColor = multi(LinearGradient(0.0, 0.0, 1.0, 1.0, true, CycleMethod.NO_CYCLE, stops))
        }
    }
}