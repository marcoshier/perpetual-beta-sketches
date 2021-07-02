// If no-body can be the studio, every-body can be the studio

import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.timer.*
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.Vector2
import java.io.File
import kotlin.math.max
import org.openrndr.math.IntVector2
import org.openrndr.shape.*

fun main() = application {
    configure {
        width = 1080
        height = 1920
        position = IntVector2(1920, 0)
        hideWindowDecorations = true
    }
    program {
        val filesNr = File("demo-data/images/jeroen").list().size
        val fileList = MutableList(filesNr) { i -> loadImage("demo-data/images/jeroen/output$i.png") }
        //val video = VideoPlayerFFMPEG.fromDevice(deviceName = "c922 Pro Stream Webcam")
        val video = VideoPlayerFFMPEG.fromFile("demo-data/webcam.mp4")
        video.play()

        val detector = BlazePoseDetector.load()
        val landmarks = BlazePoseLandmarks.fullBody()
        val videoImage = colorBuffer(width , height - (height - video.height))

        var elementsRt = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }
        video.ended.listen {
            video.restart()
        }
        video.newFrame.listen {
            it.frame.copyTo(videoImage, targetRectangle = IntRectangle( - (video.width - width) / 2, video.height, it.frame.width, -it.frame.height))
        }
        val bodyPartsCount = 32
        val partsToTake = setOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)
        val connections = listOf(Pair(11,12),Pair(12,14),Pair(14,16),Pair(11,13),Pair(13,15),Pair(11,23),Pair(23,24),Pair(24,12),Pair(23,25),Pair(11,12),Pair(25,27),Pair(24,26),Pair(26,28))

        // To change fun
        var counter = 0
        var selectedImages  = (1..bodyPartsCount).map { (1 until filesNr - 1).random() }
        timeOut(5.0) {
            repeat(20.0) {
                //counter++
                if(counter == 6) {
                    counter = 0
                }
            }
            repeat(1.0) {
                selectedImages  = (1..bodyPartsCount).map { (1 until filesNr - 1).random() }
            }
        }
        extend {
            elementsRt.clearColor(0, ColorRGBa.TRANSPARENT)

            video.draw(drawer, blind = true)
            drawer.translate(0.0, height / 2.0 - (video.height / 2))
            //drawer.image(videoImage)
            val regions = detector.detect(videoImage)

            for (region in regions) {

                computeRoi(region)

                val lms = landmarks.extract(drawer, region,  videoImage)
                val ti = region.transform
                drawer.scale(1.5)
                drawer.translate(0.0, 0.0)

                when(counter) {
                    0 -> boxes(lms, partsToTake, elementsRt, fileList, selectedImages, connections)
                    1 -> puppet(lms, partsToTake, elementsRt, connections, video)
                    2 -> ghost("ghost")
                    3 -> poisson("poisson")
                    4 -> quadtree("quadtree")
                    5 -> points("points")
                }
            }

            drawer.defaults()
            drawer.image(elementsRt.colorBuffer(0))
        }
    }
}
private fun Program.boxes(lms: List<Landmark>, select: Set<Int>, rt: RenderTarget, images: MutableList<ColorBuffer>, imgsIndex: List<Int>, connections: List<Pair<Int, Int>>) {

    lms.slice(select).forEachIndexed { index, lm ->
        // body part properties
        val x = lm.imagePosition.x
        val y = lm.imagePosition.y
        val rectHeight = 50.0
        val rectWidth = 50.0
        var currentImageIndex = imgsIndex.elementAt(index)

        // images
        drawer.isolatedWithTarget(rt) {
            drawer.fill = null
            drawer.stroke = null
            var image = images.elementAt(currentImageIndex)

            val imageRect = Rectangle(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
            val rect = Rectangle(x - rectWidth / 2, y - rectHeight / 2, rectWidth, rectHeight)

            drawer.image(image, imageRect, rect)

        }
    }
    // draw connections
    connections.forEach { connection ->
        val firstPart = lms.elementAt(connection.first)
        val secondPart = lms.elementAt(connection.second)

        drawer.stroke = ColorRGBa.GREEN
        drawer.strokeWeight = 3.0
        drawer.lineSegment(firstPart.imagePosition, secondPart.imagePosition)

    }
}

private fun Program.puppet(lms: List<Landmark>, select: Set<Int>, rt: RenderTarget, connections: List<Pair<Int, Int>>, video: VideoPlayerFFMPEG) {

    // draw parts
    drawer.fill = ColorRGBa.BLACK
    drawer.stroke = ColorRGBa.WHITE
    drawer.strokeWeight = 1.0
    drawer.isolatedWithTarget(rt) {
        // draw connections
        connections.forEach { connection ->
            val firstPart = lms.elementAt(connection.first)
            val secondPart = lms.elementAt(connection.second)

            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 3.0
            drawer.lineSegment(firstPart.imagePosition, secondPart.imagePosition)

        }
        // draw parts
        lms.slice(select).forEachIndexed { index, lm ->
            // body part properties
            val x = lm.imagePosition.x
            val y = lm.imagePosition.y
            val rectHeight = 50.0
            val rectWidth = 50.0
            val distance = 1500
            when (index) {
                0 -> { // head
                    val c1 = contour {
                        moveTo(x - rectWidth / 2 - 700, y - distance)
                        lineTo(x - rectWidth / 2 + 700, y - distance)
                    }
                    val points1 = c1.equidistantPositions(4)


                    val bodyRect1 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints1 = bodyRect1.contour.equidistantPositions(10)


                    drawer.rectangle(bodyRect1)
                    points1.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints1.elementAt(index))

                        })
                    }
                }
                1 -> { // right shoulder
                    val c2 = contour {
                        moveTo(x - distance, y - rectWidth / 2 - 300)
                        lineTo(x - distance + 400, y - rectWidth / 2 - distance)
                    }
                    val points2 = c2.equidistantPositions(4)

                    val bodyRect2 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints2 = bodyRect2.contour.equidistantPositions(18)


                    drawer.rectangle(bodyRect2)
                    points2.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints2.elementAt(14 + index))

                        })
                    }
                }
                2 -> { // left shoulder
                }
                3 -> { // right elbow
                    val c3 = contour {
                        moveTo(x + distance, y - rectWidth / 2 - 600)
                        lineTo(x + distance, y - rectWidth / 2 + 100)
                    }
                    val points3 = c3.equidistantPositions(4)

                    val bodyRect3 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints3 = bodyRect3.contour.equidistantPositions(18)

                    drawer.rectangle(bodyRect3)
                    points3.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints3.elementAt( 5 + index))

                        })
                    }
                }
                4 -> { // left elbow
                    val c2 = contour {
                        moveTo(x - distance, y - rectWidth / 2)
                        lineTo(x - distance, y - rectWidth / 2 - 400)
                    }
                    val points2 = c2.equidistantPositions(4)

                    val bodyRect2 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints2 = bodyRect2.contour.equidistantPositions(18)


                    drawer.rectangle(bodyRect2)
                    points2.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints2.elementAt(14 + index))

                        })
                    }
                }
                5 -> { // right hand
                    val c3 = contour {
                        moveTo(x + distance, y - rectWidth / 2)
                        lineTo(x + distance, y - rectWidth / 2 +
                                300)
                    }
                    val points3 = c3.equidistantPositions(4)

                    val bodyRect3 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints3 = bodyRect3.contour.equidistantPositions(18)

                    drawer.rectangle(bodyRect3)
                    points3.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints3.elementAt( 5 + index))

                        })
                    }
                }
                6 -> { // left hand
                    val c2 = contour {
                        moveTo(x - distance, y - rectWidth / 2)
                        lineTo(x - distance, y - rectWidth / 2 - 300)
                    }
                    val points2 = c2.equidistantPositions(4)

                    val bodyRect2 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints2 = bodyRect2.contour.equidistantPositions(18)


                    drawer.rectangle(bodyRect2)
                    points2.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints2.elementAt(14 + index))

                        })
                    }
                }
                7 -> { // right hip
                    val c3 = contour {
                        moveTo(x + distance, y - rectWidth / 2 + 200)
                        lineTo(x + distance, y - rectWidth / 2 + 600)
                    }
                    val points3 = c3.equidistantPositions(4)

                    val bodyRect3 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints3 = bodyRect3.contour.equidistantPositions(18)

                    drawer.rectangle(bodyRect3)
                    points3.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints3.elementAt( 5 + index))

                        })
                    }
                }
                8 -> { // left hip
                    val c2 = contour {
                        moveTo(x - distance, y - rectWidth / 2 + 600)
                        lineTo(x - distance, y - rectWidth / 2 + 200)
                    }
                    val points2 = c2.equidistantPositions(4)

                    val bodyRect2 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints2 = bodyRect2.contour.equidistantPositions(18)


                    drawer.rectangle(bodyRect2)
                    points2.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints2.elementAt(14 + index))

                        })
                    }
                }
                9 -> { // right knee
                    val c3 = contour {
                        moveTo(x + distance, y - rectWidth / 2)
                        lineTo(x + distance, y - rectWidth / 2 + distance)
                    }
                    val points3 = c3.equidistantPositions(4)

                    val bodyRect3 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints3 = bodyRect3.contour.equidistantPositions(18)

                    drawer.rectangle(bodyRect3)
                    points3.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints3.elementAt( 5 + index))

                        })
                    }
                }
                10 -> { // left knee
                    val c2 = contour {
                        moveTo(x - distance, y - rectWidth / 2 + distance)
                        lineTo(x - distance, y - rectWidth / 2)
                    }
                    val points2 = c2.equidistantPositions(4)

                    val bodyRect2 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints2 = bodyRect2.contour.equidistantPositions(18)


                    drawer.rectangle(bodyRect2)
                    points2.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints2.elementAt(14 + index))

                        })
                    }
                }
                11 -> { // right foot
                    val c3 = contour {
                        moveTo(x + distance + 200, y + distance)
                        lineTo(x - 300, y + distance)
                    }
                    val points3 = c3.equidistantPositions(4)

                    val bodyRect3 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints3 = bodyRect3.contour.equidistantPositions(18)

                    drawer.rectangle(bodyRect3)
                    points3.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints3.elementAt( 10 + index))

                        })
                    }
                }
                12 -> { // left foot
                    val c3 = contour {
                        moveTo(x + 200, y + distance)
                        lineTo(x - distance - 300, y + distance)
                    }
                    val points3 = c3.equidistantPositions(4)

                    val bodyRect3 = Rectangle(x - rectWidth / 2, y - rectWidth / 2, rectWidth, rectHeight)
                    val bodyRectPoints3 = bodyRect3.contour.equidistantPositions(18)

                    drawer.rectangle(bodyRect3)
                    points3.forEachIndexed{index, it ->
                        drawer.contour(contour {
                            //moveTo(it.x + cos(seconds) / 2.0, it.y - cos(seconds) * 200.0)
                            moveTo(it.x, it.y)
                            lineTo(bodyRectPoints3.elementAt( 10 + index))

                        })
                    }
                }
            }

        }
    }
}

private fun Program.ghost(arg:String) {
}

private fun Program.poisson(arg:String) {
    println(arg)
}

private fun Program.quadtree(arg:String) {
    println(arg)
}

private fun Program.points(arg:String) {
    println(arg)
}
