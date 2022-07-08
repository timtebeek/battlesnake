package com.github.timtebeek;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import com.sun.net.httpserver.SimpleFileServer.OutputLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HttpServerWithGameHandlerExtension.class)
class GameHandlerTest {

	static final String SAMPLE_MOVE_REQUEST = """
			{
			  "game": {
			    "id": "game-00fe20da-94ad-11ea-bb37",
			    "ruleset": {
			      "name": "standard",
			      "version": "v.1.2.3",
			      "settings": {
			        "foodSpawnChance": 25,
			        "minimumFood": 1,
			        "hazardDamagePerTurn": 14,
			        "royale": {
			          "shrinkEveryNTurns": 5
			        },
			        "squad": {
			          "allowBodyCollisions": true,
			          "sharedElimination": true,
			          "sharedHealth": true,
			          "sharedLength": true
			        }
			      }
			    },
			    "map": "standard",
			    "source": "league",
			    "timeout": 500
			  },
			  "turn": 14,
			  "board": {
			    "height": 11,
			    "width": 11,
			    "food": [
			      {"x": 5, "y": 5},
			      {"x": 9, "y": 0},
			      {"x": 2, "y": 6}
			    ],
			    "hazards": [
			      {"x": 3, "y": 2}
			    ],
			    "snakes": [
			      {
			        "id": "snake-508e96ac-94ad-11ea-bb37",
			        "name": "My Snake",
			        "health": 54,
			        "body": [
			          {"x": 0, "y": 0},
			          {"x": 1, "y": 0},
			          {"x": 2, "y": 0}
			        ],
			        "latency": "111",
			        "head": {"x": 0, "y": 0},
			        "length": 3,
			        "shout": "why are we shouting??",
			        "squad": "",
			        "customizations":{
			          "color":"#FF0000",
			          "head":"pixel",
			          "tail":"pixel"
			        }
			      },
			      {
			        "id": "snake-b67f4906-94ae-11ea-bb37",
			        "name": "Another Snake",
			        "health": 16,
			        "body": [
			          {"x": 5, "y": 4},
			          {"x": 5, "y": 3},
			          {"x": 6, "y": 3},
			          {"x": 6, "y": 2}
			        ],
			        "latency": "222",
			        "head": {"x": 5, "y": 4},
			        "length": 4,
			        "shout": "I'm not really sure...",
			        "squad": "",
			        "customizations":{
			          "color":"#26CF04",
			          "head":"silly",
			          "tail":"curled"
			        }
			      }
			    ]
			  },
			  "you": {
			    "id": "snake-508e96ac-94ad-11ea-bb37",
			    "name": "My Snake",
			    "health": 54,
			    "body": [
			      {"x": 0, "y": 0},
			      {"x": 1, "y": 0},
			      {"x": 2, "y": 0}
			    ],
			    "latency": "111",
			    "head": {"x": 0, "y": 0},
			    "length": 3,
			    "shout": "why are we shouting??",
			    "squad": "",
			    "customizations": {
			      "color":"#FF0000",
			      "head":"pixel",
			      "tail":"pixel"
			    }
			  }
			}""";

	@Test
	void parseSampleMoveRequest() throws Exception {
		RequestBody postBody = new ObjectMapper().readValue(SAMPLE_MOVE_REQUEST, RequestBody.class);
		assertThat(postBody).isNotNull();
	}

	@Test
	void postMove(HttpServer server) throws IOException, InterruptedException {
		InetSocketAddress address = server.getAddress();
		var request = HttpRequest.newBuilder(URI.create("http://localhost:%d/move".formatted(address.getPort())))
				.POST(HttpRequest.BodyPublishers.ofString(SAMPLE_MOVE_REQUEST)).build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualByComparingTo(200);
		assertThat(response.body()).isEqualTo("{\"move\": \"up\"}");
	}

}

class HttpServerWithGameHandlerExtension
		extends TypeBasedParameterResolver<HttpServer>
		implements BeforeAllCallback, AfterAllCallback {

	private HttpServer server;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		InetSocketAddress address = new InetSocketAddress(0);
		Filter filter = SimpleFileServer.createOutputFilter(System.out, OutputLevel.INFO);
		server = HttpServer.create(address, 1, "/", new GameHandler(), filter);
		server.start();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		server.stop(1);
	}

	@Override
	public HttpServer resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return server;
	}

}
