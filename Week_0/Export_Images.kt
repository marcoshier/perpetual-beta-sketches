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
import org.openrndr.draw.*
import org.openrndr.extras.imageFit.imageFit
import java.lang.module.ModuleFinder.compose
import kotlin.math.sin
import org.openrndr.extra.compositor.*
import org.openrndr.extra.gui.addTo
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.shape
import java.io.File


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
        var filtered = BinaryImageOps.erode8(binary, 16, null)
        filtered = BinaryImageOps.dilate8(filtered, 16, null)

        val contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null)
        val shapes = contours.toShapeContours(true,
            internal = false, external = true)

        drawer.isolatedWithTarget(imageRt) {

            drawer.clear(ColorRGBa.TRANSPARENT)
            // scaling to see the whole picture
            //drawer.scale(0.2)

            drawer.image(colorImage)

        }


        drawer.isolatedWithTarget(shapesRt) {

            drawer.clear(ColorRGBa.TRANSPARENT)

            fill = null
            shapes.forEachIndexed { index, shapeContour ->
                drawer.isolated {

                    drawer.clear(ColorRGBa.TRANSPARENT)

                    // scaling to see the whole picture
                    //drawer.scale(0.2)


                    // BOUNDING BOXES

                    var SaveRt = renderTarget(width, height) {
                        colorBuffer()
                    }

                    stroke = ColorRGBa.RED
                    strokeWeight = 1.0
                    val bounds = shapeContour.bounds
                    drawer.rectangle(shapeContour.bounds)


                    SaveRt.colorBuffer(0).destroy();
                    SaveRt.destroy()
                    SaveRt = renderTarget(bounds.width.toInt(), bounds.height.toInt()) {

                    }

                    SaveRt.attach(colorBuffer(bounds.width.toInt(), bounds.height.toInt()))


                    drawer.isolatedWithTarget(SaveRt) {
                        val source = Rectangle(bounds.x, bounds.y, bounds.width, bounds.height)
                        val target = Rectangle(0.0, 0.0, bounds.width, bounds.height)
                        drawer.image(imageRt.colorBuffer(0), source, target)
                    }

                    SaveRt.colorBuffer(0).saveToFile(File("data/images/exported/1/output$index.png"))


                    //imageRt.colorBuffer(0).saveToFile(File("data/images/exported/1/output$index.png"))
                    // SHAPE CONTOURS


                    /*
                    fill = null
                    stroke = ColorRGBa.BLACK
                    drawer.contour(shapeContour)*/
                }
            }
        }

        println(shapes.size)



        extend {

            //drawer.image(imageRt.colorBuffer(0))
            //drawer.image(shapesRt.colorBuffer(0))

            imageRt.colorBuffer(0).copyTo(shapesRt.colorBuffer(0))


            drawer.image(shapesRt.colorBuffer(0))



        }
    }
}

fun saveImage(boundsRect: Rectangle, ImgRt: RenderTarget) {


}
