package controllers

import actors.TopActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject._
import model.Spark
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TopController @Inject()(cc: ControllerComponents)(implicit system: ActorSystem, mat: Materializer)
  extends AbstractController(cc) {
  val spark = new Spark
  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.top.render(request))
  }

  def socket: WebSocket = WebSocket.accept[JsValue, JsValue] { implicit request:RequestHeader =>
    ActorFlow.actorRef { out =>
      TopActor.props(out, system, mat, spark)
    }
  }
}