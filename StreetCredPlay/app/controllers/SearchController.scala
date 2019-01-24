package controllers

import play.api.mvc._
import play.api.libs.streams.ActorFlow
import javax.inject.Inject
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import actors.{ModelActor, SearchActor}
import model.Spark
import play.api.libs.json.JsValue

class SearchController @Inject()(cc:ControllerComponents)
                                (implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {
  val spark = new Spark
  val model: ActorRef = system.actorOf(ModelActor.props(spark), "model")

  def index(query: String) = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.search.render(request, query))
  }

  def socket: WebSocket = WebSocket.accept[JsValue, JsValue] { implicit request:RequestHeader =>
    ActorFlow.actorRef { out =>
      SearchActor.props(out, system, mat, model, spark)
    }
  }
}