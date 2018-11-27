package controllers;

import actors.ExampleActor;
import play.*;
import play.mvc.*;
import akka.actor.*;
import akka.stream.*;
import play.libs.streams.ActorFlow;

import javax.inject.Inject;


public class ExampleSearchController extends Controller {
	
	private final ActorSystem actorSystem;
    private final Materializer materializer;

	@Inject
	public ExampleSearchController(ActorSystem actorSystem, Materializer materializer) {
		this.actorSystem = actorSystem;
        this.materializer = materializer;
	}
	
	public Result index() {
		return ok(views.html.ExampleSearch.render());
    }
	
	public WebSocket socket() {
        return WebSocket.Json.accept(request ->
                ActorFlow.actorRef(ExampleActor::props,
                    actorSystem, materializer
                )
        );
    }
}
