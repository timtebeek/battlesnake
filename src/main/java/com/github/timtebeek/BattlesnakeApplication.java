package com.github.timtebeek;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import com.sun.net.httpserver.SimpleFileServer.OutputLevel;

public class BattlesnakeApplication {

	// TODO Personalize
	private static final String PERSONALIZATION = """
			{
			  "apiversion": "1",
			  "author": "MyUsername",
			  "color": "#888888",
			  "head": "default",
			  "tail": "default",
			  "version": "0.0.1-beta"
			}
			""";

	public static void main(String[] args) throws IOException {
		// https://docs.battlesnake.com/references/api
		HttpHandler handler = HttpHandlers.handleOrElse(
				request -> "POST".equals(request.getRequestMethod()), new GameHandler(),
				HttpHandlers.of(200, Headers.of("Content-Type", "application/json"), PERSONALIZATION));
		Filter filter = SimpleFileServer.createOutputFilter(System.out, OutputLevel.INFO);
		var server = HttpServer.create(new InetSocketAddress(8080), 10, "/", handler, filter);
		server.start();
	}

}

class GameHandler implements HttpHandler {

	private static final String EMPTY_STRING = "";
	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		// Read request body
		RequestBody postBody = mapper.readValue(exchange.getRequestBody(), RequestBody.class);
		System.out.println(postBody);

		// Determine response
		String path = exchange.getRequestURI().getPath();
		String responseBody = switch (path) {
		case "/start" -> start(postBody);
		case "/move" -> "{\"move\": \"%s\"}".formatted(move(postBody));
		case "/end" -> end(postBody);
		default -> throw new IllegalArgumentException("Unexpected value: " + path);
		};

		// Write response
		final var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		if (bytes.length == 0) {
			exchange.sendResponseHeaders(200, -1);
		} else {
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
		}
	}

	private String start(RequestBody postBody) {
		// TODO Initialize Game state from exchange body
		return EMPTY_STRING;
	}

	private Move move(RequestBody postBody) {
		// TODO Determine next move
		return Move.up;
	}

	private String end(RequestBody postBody) {
		// TODO Clean Game state
		return EMPTY_STRING;
	}

}

enum Move {
	up, right, down, left;
}

record RequestBody(
		Game game,
		int turn,
		Board board,
		Battlesnake you) {
}

record Game(
		String id,
		Ruleset ruleset,
		String map,
		int timeout,
		String source) {
}

record Ruleset(
		String name,
		String version,
		RulesetSettings settings) {
}

record RulesetSettings(
		int foodSpawnChance,
		int minimumFood,
		int hazardDamagePerTurn,
		Royale royale,
		Squad squad) {
}

record Royale(int shrinkEveryNTurns) {
}

record Squad(
		boolean allowBodyCollisions,
		boolean sharedElimination,
		boolean sharedHealth,
		boolean sharedLength) {
}

record Battlesnake(
		String id,
		String name,
		int health,
		List<Point> body,
		String latency,
		Point head,
		int length,
		String shout,
		String squad,
		Customizations customizations) {
}

record Customizations(
		String required,
		String author,
		String color,
		String head,
		String tail,
		String version) {
}

record Point(int x, int y) {
}

record Board(
		int height,
		int width,
		List<Point> food,
		List<Point> hazards,
		List<Battlesnake> snakes) {
}
