package actors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections; 



import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.*;

import akka.actor.*;
import play.libs.Json;

public class ExampleActor extends AbstractActor {

	public static Props props(ActorRef out) {
		return Props.create(ExampleActor.class, out);
	}

	private final ActorRef out;
	ObjectMapper mapper = new ObjectMapper();
	
	// user-specific data
	List<String> cdps = null;
	ObjectNode preferences = null;
	List<String> scoredCDPs = null;

	public ExampleActor(ActorRef out) {
		this.out = out;

		ObjectNode msg = Json.newObject();
		msg.put("messageType", "init");
		out.tell(msg, self());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(JsonNode.class, message -> {

					String messageType = message.get("messageType").asText();
					System.out.println(messageType);
					switch (messageType) {
						case "doSearch":
							String query = message.get("query").asText();
							
							// do something here
							
							// send response
							{
									ObjectNode msg1 = Json.newObject();
									msg1.put("messageType", "statusMessage");
									msg1.put("status", "Hello!");
									out.tell(msg1, self());
							}
							
							
							break;
					}


				}).build();
	}

	@Override
	public void postStop() throws Exception {
		// do shutdown
	}
}
