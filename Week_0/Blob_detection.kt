import boofcv.alg.filter.binary.BinaryImageOps
import boofcv.alg.filter.binary.GThresholdImageOps
import boofcv.alg.misc.ImageStatistics
import boofcv.struct.ConnectRule
import boofcv.struct.image.GrayU8
import org.openrndr.application
import org.openrndr.boofcv.binding.resizeTo
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.boofcv.binding.toShapeContours
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import kotlin.math.sin

fun main() = application {
    configure {
        width = 1920
        height = 1080
        windowResizable = true
    }
    program {

        val rt = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        val image = loadImage("data/images/jeroen.jpg").toGrayF32();
        //val image = imageFullSize.resizeTo(width, height)
        val binary = GrayU8(image.width, image.height)



        GThresholdImageOps.threshold(image, binary,  ImageStatistics.mean(image).toDouble(), false)

        var filtered = BinaryImageOps.erode8(binary, 2, null)
        filtered = BinaryImageOps.dilate8(filtered, 5, null)

        val contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null)

        val shapes = contours.toShapeContours(true,
            internal = false, external = true)

        drawer.isolatedWithTarget(rt) {

            drawer.scale(0.2)
            clear(ColorRGBa.PINK)
            stroke = ColorRGBa.BLACK
            strokeWeight = 1.0
            for(shape in shapes) {
                shape.position((seconds * 0.1) % 1.0)
            }
            drawer.contours(shapes)
        }


        extend {



            drawer.image(rt.colorBuffer(0))
            }
        }
    }

