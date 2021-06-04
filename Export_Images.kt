import boofcv.alg.filter.binary.BinaryImageOps
import boofcv.alg.filter.binary.GThresholdImageOps
import boofcv.alg.filter.binary.GThresholdImageOps.computeOtsu
import boofcv.alg.filter.binary.ThresholdImageOps
import boofcv.alg.misc.ImageStatistics
import boofcv.struct.ConnectRule
import boofcv.struct.image.GrayU8
import org.openrndr.application
import org.openrndr.boofcv.binding.resizeTo
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.boofcv.binding.toShapeContours
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extras.imageFit.imageFit
import java.lang.module.ModuleFinder.compose
import kotlin.math.sin
import org.openrndr.extra.compositor.*
import org.openrndr.shape.shape


fun main() = application {
    configure {
        width = 1280
        height = 720
        windowResizable = true
    }
    program {

        val shapesRt = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        val imageRt = renderTarget(width, height) {
            colorBuffer()
        }

        val colorImage = loadImage("data/images/jeroen_blue.jpg")
        val image = colorImage.toGrayF32();

        val binary = GrayU8(image.width, image.height)

        val meanThreshold = ImageStatistics.mean(image).toDouble()
        val entropyThreshold = GThresholdImageOps.computeEntropy(image, 0.0, 256.0);
        val otsuThreshold = computeOtsu(image, 0.0, 255.0);

        GThresholdImageOps.threshold(image, binary,  meanThreshold, false)
        var filtered = BinaryImageOps.erode8(binary, 1, null)
        filtered = BinaryImageOps.dilate8(filtered, 1, null)

        val contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null)
        val shapes = contours.toShapeContours(true,
            internal = false, external = true)


        drawer.isolatedWithTarget(shapesRt) {

            drawer.clear(ColorRGBa.TRANSPARENT)
            fill = null
            shapes.forEachIndexed { index, shapeContour ->
                drawer.isolated {

                    // scaling to see the whole picture
                    drawer.scale(0.2)


                    stroke = ColorRGBa.RED
                    strokeWeight = 1.0
                    drawer.rectangle(shapeContour.bounds)


                    fill = null
                    stroke = ColorRGBa.BLACK
                    drawer.contour(shapeContour)
                }
            }
        }

        drawer.isolatedWithTarget(imageRt) {

            drawer.clear(ColorRGBa.TRANSPARENT)
            // scaling to see the whole picture
            drawer.scale(0.2)

            drawer.image(colorImage)
            //drawer.rectangle(140.0, 140.0, 80.0, 80.0)
        }

        extend {





            drawer.image(imageRt.colorBuffer(0))

            drawer.image(shapesRt.colorBuffer(0))

        }
    }
}