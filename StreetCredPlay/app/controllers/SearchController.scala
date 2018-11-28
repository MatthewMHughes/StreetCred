package controllers

import play.api.mvc._
import play.api.libs.streams.ActorFlow
import javax.inject.Inject
import akka.actor.ActorSystem
import akka.stream.Materializer
import actors.SearchActor
import play.api.libs.json.JsValue

class SearchController @Inject()(cc:ControllerComponents)
                                (implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {

  def index = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.ExampleSearch.render(request))
  }

  def socket: WebSocket = WebSocket.accept[JsValue, JsValue] { implicit request:RequestHeader =>
    ActorFlow.actorRef { out =>
      SearchActor.props(out, system, mat)
    }
  }
}