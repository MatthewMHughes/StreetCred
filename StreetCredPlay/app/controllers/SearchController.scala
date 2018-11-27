package controllers

import play.api.mvc._
import play.api.libs.streams.ActorFlow
import javax.inject.Inject
import akka.actor.ActorSystem
import akka.stream.Materializer
import actors.SearchActor

class SearchController @Inject()(cc:ControllerComponents)
                                (implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.ExampleSearch.render())
  }

  def socket:WebSocket = WebSocket.accept[String, String] { implicit request =>
    ActorFlow.actorRef { out =>
      SearchActor.props(out, system, mat)
    }
  }
}