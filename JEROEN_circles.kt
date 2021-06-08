import com.badlogic.gdx.Input.Keys.G
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle


class Constants {
    companion object {
        var TIME_STEP = 1/60f
        var VELOCITY_ITERATIONS = 20
        var POSITION_ITERATIONS = 20
    }
}

class bodyRectangle(var body:Body, var width: Double, var height: Double)
class bodyCircle(var body:Body, var width: Double, var height: Double, var radius : Double)
class attractorCircle(var body:Body, var width: Double, var height: Double, var radius: Double)

fun main() = application {
    configure {
        width = 600
        height = 600
    }

    // World
    lateinit var world : World
    var accumulator = 0f

    // Physics
    fun doPhysicsStep(deltaTime: Float, bodies: ArrayList<bodyCircle>, attractors: ArrayList<attractorCircle>) {
        // fixed time step
        // max frame time to avoid spiral of death (on slow devices)
        val frameTime = Math.min(deltaTime, 0.25f)
        accumulator += frameTime
        while (accumulator >= Constants.TIME_STEP) {
            world.step(Constants.TIME_STEP, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS)
            accumulator -= Constants.TIME_STEP
             for (i in 1 until bodies.size) {
                 val moverPos: Vector2 = bodies[i].body.getWorldCenter()
                 val pos: Vector2 = attractors[0].body.getWorldCenter()

                 var force = pos.sub(moverPos)
                 var distance = kotlin.math.sqrt(pos.x * pos.x.toDouble() + pos.y * pos.y.toDouble());

                 distance.coerceIn(1.0, 2.0);
                 // normalize?

                 val strength: Double =
                     G * 1 * attractors[0].body.mass / (distance * distance) // Calculate gravitional force magnitude

                // force.mul(strength) // Get force vector --> magnitude * direction
                 // force = force.mul(strength)

                 bodies[i].body.applyForce(force, pos, false )
             }
        }
    }

    program {

        val scaleFactor = 5.0

        // Images
        val fileNumber = 2585
        val imageList:MutableList<ColorBuffer> = ArrayList()

        for(i in 1 until fileNumber) {
            var image = loadImage("data/images/1/output$i.png")
            imageList.add(image)
        }

        // Set
        world = World(Vector2(0.0f, -0.0f), false)

        val circles = arrayListOf<bodyCircle>()
        val rectangles = arrayListOf<bodyRectangle>()
        val attractors = arrayListOf<attractorCircle>()

        // Create rectangle
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
            fixtureDef.friction = 0.4f
            fixtureDef.restitution = 0.01f

            body.createFixture(fixtureDef)
            rectangles.add(bodyRectangle(body, w.toDouble(), h.toDouble()))

        }

        // Create circles
        drawer.isolated {
            imageList.take(100).forEachIndexed { index, image ->

                val bodyDef = BodyDef()

                // Position based on width
                val x = width / 2 / (index + 1)
                val y = height / 2 / (index + 1)

                bodyDef.position.set(x.toFloat(), y.toFloat())
                bodyDef.type = BodyDef.BodyType.DynamicBody

                val body = world.createBody(bodyDef)

                val fixtureDef = FixtureDef()
                fixtureDef.density = 1.0f / (index + 1)
                fixtureDef.friction = 0.4f
                fixtureDef.restitution = 0.01f
                fixtureDef.shape = CircleShape()

                if(image.width > image.height) {
                    val r: Double = image.width.toDouble() / 2.0 / scaleFactor

                    // Terrible code vVv

                    fixtureDef.shape.radius = r.toFloat()
                    body.createFixture(fixtureDef)

                    circles.add(bodyCircle(body, 1.0, 1.0, radius = r))
                } else if(image.width <= image.height) {
                    val r: Double = image.height.toDouble() / 2.0 / scaleFactor

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
            fixtureDef.density = 0.1f
            fixtureDef.friction = 0.4f
            fixtureDef.restitution = 0.01f
            fixtureDef.shape = CircleShape()


            val r: Double =  50.0

            fixtureDef.shape.radius = r.toFloat()
            body.createFixture(fixtureDef)


            attractors.add(attractorCircle(body, 1.0, 1.0, radius = r))
        }


        extend {

            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = ColorRGBa.GRAY.opacify(0.25)

            // draw floor (TODO turn into walls)
            drawer.rectangles(
                rectangles.map {
                    Rectangle(it.body.position.x.toDouble(), it.body.position.y.toDouble() - it.height / 2 - 50.0, it.width, it.height)
                }
            )

            // drawer particles
             drawer.circles(
                circles.map {
                    Circle(org.openrndr.math.Vector2(it.body.position.x.toDouble(), it.body.position.y.toDouble()), it.radius)
                }
            )

            circles.forEachIndexed { index, circle ->

               // drawer.scale(scaleFactor / 10)

                val currentPiece = imageList.elementAt(index)
                val pieceWidth = currentPiece.width.toDouble() / scaleFactor
                val pieceHeight = currentPiece.height.toDouble() / scaleFactor

                val circleX = circle.body.position.x.toDouble() - pieceWidth / 2
                val circleY = circle.body.position.y.toDouble() - pieceHeight / 2


                drawer.image(currentPiece, circleX, circleY, pieceWidth, pieceHeight)

            }



            // draw attractor
            drawer.circles(
                attractors.map {
                    Circle(org.openrndr.math.Vector2(it.body.position.x.toDouble(), it.body.position.y.toDouble()), it.radius)
                }
            )

            // update
            doPhysicsStep(seconds.toFloat(), circles, attractors)

        }
    }
}