package com.github.timtebeek;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import com.sun.net.httpserver.SimpleFileServer.OutputLevel;

import static com.github.timtebeek.BattlesnakeApplication.ok;

public class BattlesnakeApplication {

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

	private static final Headers HEADERS = Headers.of("Content-Type", "application/json");

	public static void main(String[] args) throws IOException {
		GameHandler gameHandler = new GameHandler();

		// https://docs.battlesnake.com/references/api
		var handlerChain = HttpHandlers.handleOrElse(
				match("POST", "/move"), gameHandler,
				HttpHandlers.handleOrElse(
						match("GET", "/"), ok(PERSONALIZATION),
						HttpHandlers.handleOrElse(
								match("POST", "/start"), gameHandler::start,
								HttpHandlers.handleOrElse(
										match("POST", "/end"), gameHandler::end,
										HttpHandlers.of(400, HEADERS, "{}")))));
		Filter filter = SimpleFileServer.createOutputFilter(System.out, OutputLevel.INFO);
		var server = HttpServer.create(new InetSocketAddress(8080), 10, "/", handlerChain, filter);
		server.start();
	}

	private static Predicate<Request> match(String method, String path) {
		return request -> method.equals(request.getRequestMethod())
				&& path.equals(request.getRequestURI().getPath());
	}

	static HttpHandler ok(String body) {
		return HttpHandlers.of(200, HEADERS, body);
	}

}

class GameHandler implements HttpHandler {

	private static final ObjectMapper mapper = new ObjectMapper();

	void start(HttpExchange exchange) throws IOException {
		// TODO Initialize Game state from exchange body
		ok("start").handle(exchange);
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		PostBody postBody = mapper.readValue(exchange.getRequestBody(), PostBody.class);
		System.out.println(postBody);

		// TODO Determine next move
		String move = "up";

		String responseBody = "{\"move\": \"%s\"}".formatted(move);
		ok(responseBody).handle(exchange);
	}

	void end(HttpExchange exchange) throws IOException {
		// TODO Clean Game state
		ok("end").handle(exchange);
	}

}

record PostBody(
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
