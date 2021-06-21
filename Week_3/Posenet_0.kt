import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.runway.PoseNetRequest
import org.openrndr.extra.runway.PoseNetResponse
import org.openrndr.extra.runway.runwayQuery
import org.openrndr.extra.runway.toData
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.shape.Rectangle
import java.io.File

fun main() = application {
    configure {
        width = 1422
        height = 800
        windowResizable = true
    }
    val keypoints: List<Pair<Int, String>> = listOf(
        Pair(0, "nose"),
        Pair(1, "leftEye"),
        Pair(2,    "rightEye"),
        Pair(3, "leftEar"),
        Pair(4, "rightEar"),
        Pair(5, "leftShoulder"),
        Pair(6, "rightShoulder"),
        Pair(7, "leftElbow"),
        Pair(8, "rightElbow"),
        Pair(9, "leftWrist"),
        Pair(10, "rightWrist"),
        Pair(11, "leftHip"),
        Pair(12, "rightHip"),
        Pair(13, "leftKnee"),
        Pair(14, "rightKnee"),
        Pair(15, "leftAnkle"),
        Pair(16, "rightAnkle")
    )

    val connections:List<Pair<String, String>> = listOf(
        Pair("rightHip", "leftHip"),
        Pair("rightHip", "rightShoulder"),
        Pair("leftHip", "leftShoulder"),
        Pair("leftShoulder", "rightShoulder"),
        Pair("leftShoulder", "leftElbow"),
        Pair("leftElbow", "leftWrist"),
        Pair("rightShoulder", "rightElbow"),
        Pair("rightElbow", "rightWrist"),
        Pair("leftShoulder", "leftElbow"),
        Pair("rightHip", "rightKnee"),
        Pair("rightKnee", "rightAnkle"),
        Pair("leftHip", "leftKnee"),
        Pair("leftKnee", "leftAnkle")
    )

    program {
        val filesNr = File("data/images/1").list().size
        val fileList = MutableList(filesNr) { i -> loadImage("data/images/1/output$i.png") }
        var videoRt = renderTarget(width, height){
            colorBuffer()
        }
        var rectsRt = renderTarget(width, height) {
            colorBuffer()
        }
        val video = VideoPlayerFFMPEG.fromFile("data/images/Jeroen_Poses.mp4")
        video.play()


        extend {
            drawer.withTarget(videoRt) {
                video.draw(drawer)
            }
            val result: PoseNetResponse = runwayQuery("http://localhost:8000/query", PoseNetRequest(videoRt.colorBuffer(0).toData()))
            val poses = result.poses
            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = ColorRGBa.GREEN
            poses.forEach { poses ->
                poses.forEachIndexed { index, pose ->
                    drawConnections(connections, poses, keypoints)
                    drawParts(pose, fileList, rectsRt)

                }
            }
        }
    }
}
private fun Program.drawParts(points: List<Double>, images: MutableList<ColorBuffer>, rt: RenderTarget) {
    val x = points[0] * width.toDouble()
    val y = points[1] * height.toDouble()
    val rectHeight = 80.0
    val rectWidth = 80.0
    drawer.isolatedWithTarget(rt) {
        drawer.clear(ColorRGBa.TRANSPARENT)

        val image = images.elementAt((Math.random() * images.size).toInt())

        val imageRect = Rectangle(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
        val rect = Rectangle(x  - rectWidth / 2, y - rectHeight / 2, rectWidth , rectHeight )

        drawer.image(image, imageRect, rect)
    }
    drawer.image(rt.colorBuffer(0))


    //drawer.circle(x * width.toDouble(), y * height.toDouble(), 4.0)
}

// List<List<Doubles>> = BodyPart<Positions<x, y>>
private fun Program.drawConnections(connections: List<Pair<String, String>>, bodyparts: List<List<Double>>, keypoints: List<Pair<Int, String>>) {
    for (connection in connections) {
        var start: List<Double>? = null
        var end: List<Double>? = null
        for (i in 0..(bodyparts?.size - 1)) {
            val startBodypart = bodyparts.elementAt(i)



            if (keypoints.elementAt(i).second == connection.first) {
                start = startBodypart
                for (j in 0..(bodyparts?.size - 1)) {
                    val endBodypart = bodyparts.elementAt(j)
                    if (keypoints.elementAt(j).second == connection.second) {
                        end = endBodypart
                        break
                    }
                }
                break
            }
        }

        if (start != null && end != null) {

            drawer.lineSegment(
                start[0]*width.toDouble(),
                start[1]*height.toDouble(),
                end[0]*width.toDouble(),
                end[1]*height.toDouble()
            )
        }
    }
}
