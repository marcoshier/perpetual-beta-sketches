import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.transforms.transform
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.sqrt

class Constants {
    companion object {
        var TIME_STEP = 1 / 60f
        var VELOCITY_ITERATIONS = 20
        var POSITION_ITERATIONS = 20
    }
}

class bodyRectangle(var body: Body, var width: Double, var height: Double)
class bodyCircle(var body: Body, var width: Double, var height: Double, var radius: Double)
class attractorCircle(var body: Body, var width: Double, var height: Double, var radius: Double)

fun main() = application {
    configure {
        width = 1920
        height = 1080
    }
    // World preparation
    lateinit var world: World
    var accumulator = 0f
    // Physics
    fun doPhysicsStep(deltaTime: Float, bodies: ArrayList<bodyCircle>, attractors: ArrayList<attractorCircle>) {

        // max frame time to avoid spiral of death (on slow devices)
        val frameTime = Math.min(deltaTime, 0.25f)
        accumulator += frameTime

        while (accumulator >= Constants.TIME_STEP) {
            world.step(Constants.TIME_STEP, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS)
            accumulator -= Constants.TIME_STEP
            // ATTRACTOR CODE (adapted from https://github.com/shiffman/Box2D-for-Processing/blob/master/Box2D-for-Processing/dist/box2d_processing/examples/AttractionApplyForce/Attractor.pde)
            for (i in 1 until bodies.size) {



                var slower = 1.0f;
                if(deltaTime > 10.0f && deltaTime < 170.0f) {
                    slower = slower * deltaTime / 10
                }
                if(deltaTime >= 170.0f && deltaTime < 180.0f) {
                    slower = 1.0f
                }
                if(deltaTime >= 180.0f && deltaTime < 350.0f) {
                    slower = 1.0f
                    slower = slower * deltaTime / 40
                }
                if(deltaTime >= 350.0f && deltaTime < 360.0f) {
                    slower = 1.0f
                }
                if(deltaTime >= 360.0f && deltaTime < 530.0f) {
                    slower = 1.0f
                    slower = slower * deltaTime / 40
                }
                if(deltaTime >= 530.0f && deltaTime < 540.0f) {
                    slower = 1.0f
                } else if (deltaTime >= 540.0f){
                    slower = 1.0f
                    slower = slower * deltaTime / 40
                }

                println(slower)

                val G = 100 * slower; // Force strength
                val piecesPos: Vector2 = bodies[i].body.worldCenter // MOVER
                val attractorPos: Vector2 = attractors[0].body.worldCenter // MAGNET
                var force: Vector2 = attractorPos.sub(piecesPos)
                var distance = force.len();
                distance = distance.coerceIn(1.0F, 5.0F);
                force = force.nor()

                val strength = G * 1 * bodies[i].body.mass / (distance * distance)
                force = force.mulAdd(force, strength)

                bodies[i].body.applyForce(force, attractorPos, false)

            }

        }



    }

    program {
            val textRt = renderTarget(width, height) {
                colorBuffer()
            }
            extend(ScreenRecorder()) {
                maximumDuration = (1000.0 / 30.0) * 100.0
            }
            // to make circles not too big
            val scaleFactor = 2.0
            // Load Images
            val fileNumber = 1091
            val imageList: MutableList<ColorBuffer> = ArrayList()

            for (i in 1 until fileNumber) {
                var image = loadImage("data/images/1/output$i.png")
                imageList.add(image)
            }

            // Set box2d
            world = World(Vector2(0.0f, -0.0f), false)

            val circles = arrayListOf<bodyCircle>()
            val rectangles = arrayListOf<bodyRectangle>()
            val attractors = arrayListOf<attractorCircle>()


            // Create Floor
            drawer.isolated {
            val bodyDef = BodyDef()

            bodyDef.position.set(0.0f, 700.0f)
            bodyDef.type = BodyDef.BodyType.StaticBody
            val body = world.createBody(bodyDef)

            val w = 600.0f
            val h = 100.0f
            val shape = PolygonShape()
            shape.setAsBox((w / 1.0).toFloat(), (h / 1.0).toFloat())
            val fixtureDef = FixtureDef()

            fixtureDef.shape = shape
            fixtureDef.density = 0.1f
            fixtureDef.friction = 0.1f
            fixtureDef.restitution = 0.02f
            body.createFixture(fixtureDef)
            rectangles.add(bodyRectangle(body, w.toDouble(), h.toDouble()))

        }
            // Create circles
            drawer.isolated {
                var stopper = 1.0
                var restitutionValue = 0.001f
                if (seconds > 10.0) {
                    stopper = stopper / (seconds / 1000)
                }
                if (seconds >= 180 && seconds <= 185) {
                    stopper = 1.0
                    restitutionValue =  1.0f
                } else if (seconds >= 185) {
                    stopper = 1.0
                    restitutionValue = 0.001f
                    stopper = stopper / (seconds / 1000)
                }

                // limiting images for now
                imageList.take(100).forEachIndexed { index, image ->
                    val bodyDef = BodyDef()

                    // Position
                    val x = width / 2
                    val y = -height / 3
                    bodyDef.position.set(x.toFloat(), y.toFloat())

                    bodyDef.type = BodyDef.BodyType.DynamicBody
                    val body = world.createBody(bodyDef)

                    val fixtureDef = FixtureDef()
                    fixtureDef.density = 1.0f * stopper.toFloat()
                    fixtureDef.friction = 0.1f * stopper.toFloat()
                    fixtureDef.restitution = restitutionValue
                    fixtureDef.shape = CircleShape()

                    var r: Double = 0.0;
                    if(image.width > image.height) {
                        r = (image.width.toDouble() / 2.0) / scaleFactor

                        fixtureDef.shape.radius = r.toFloat()
                        body.createFixture(fixtureDef)

                        circles.add(bodyCircle(body, 1.0, 1.0, radius = r))

                    } else if (image.width <= image.height) {
                        r = (image.height.toDouble() / 2.0) / scaleFactor

                        fixtureDef.shape.radius = r.toFloat()
                        body.createFixture(fixtureDef)

                        circles.add(bodyCircle(body, 1.0, 1.0, radius = r))
                    }

                }
            }
            // Create attractor
            drawer.isolated {
                val bodyDef = BodyDef()

                bodyDef.position.set((width / 2.0).toFloat(), (height / 2.0).toFloat())
                bodyDef.type = BodyDef.BodyType.StaticBody
                val body = world.createBody(bodyDef)

                val fixtureDef = FixtureDef()
                fixtureDef.density = 2.0f
                fixtureDef.friction = 0.4f
                fixtureDef.restitution = 0.01f
                fixtureDef.shape = CircleShape()


                val r: Double = 10.0
                fixtureDef.shape.radius = r.toFloat()
                body.createFixture(fixtureDef)


                attractors.add(attractorCircle(body, 1.0, 1.0, radius = r))
            }
            // Poisson fill
            val dry = renderTarget(width, height) {
                colorBuffer(type = ColorType.FLOAT32)
            }
            val wet = colorBuffer(width, height)

            val fx = PoissonFill()
            val stepsPerEpoch = 1000
            extend {
                drawer.stroke = null
                drawer.fill = null
                // floor TODO turn into walls
                drawer.rectangles(
                    rectangles.map {
                        Rectangle(it.body.position.x.toDouble(), it.body.position.y.toDouble() - it.height / 2 - 50.0, it.width, it.height)
                    })

                // drawer particles
                drawer.circles(
                    circles.map {
                        Circle(
                            org.openrndr.math.Vector2(it.body.position.x.toDouble(), it.body.position.y.toDouble()),
                            it.radius
                        )
                    }
                )

                // draw images + poisson fill
                drawer.isolatedWithTarget(dry) {
                    clear(ColorRGBa.TRANSPARENT)

                    circles.forEachIndexed { index, circle ->
                        val currentPiece = imageList.elementAt(index)
                        val pieceWidth = currentPiece.width.toDouble() / scaleFactor
                        val pieceHeight = currentPiece.height.toDouble() / scaleFactor

                        val circleX = circle.body.position.x.toDouble() - pieceWidth / 2
                        val circleY = circle.body.position.y.toDouble() - pieceHeight / 2
                        val rotationVector = org.openrndr.math.Vector2(circleX, circleY)


                        //drawer.rotate(circle.body.angle * 1.0)
                        drawer.isolated {
                            //drawer.image(currentPiece, circleX, circleY, pieceWidth, pieceHeight)
                            drawer.image(currentPiece, rotationVector, pieceWidth, pieceHeight)
                        }
                    }
                }
                fx.apply(dry.colorBuffer(0), wet)
                drawer.image(wet)

                // draw attractor
                drawer.circles(
                    attractors.map {
                        Circle( org.openrndr.math.Vector2(it.body.position.x.toDouble(), it.body.position.y.toDouble()), it.radius)
                    })

                doPhysicsStep(seconds.toFloat(), circles, attractors)

                drawer.isolatedWithTarget(textRt) {
                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = loadFont("data/fonts/default.otf", 16.0)
                    drawer.text("EPOCH ${frameCount/stepsPerEpoch}, STEP ${frameCount%stepsPerEpoch}", 300.0, 300.0)
                }
            }
        }
    }
