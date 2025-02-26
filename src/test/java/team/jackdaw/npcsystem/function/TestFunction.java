package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.List;
import java.util.Map;

public class TestFunction extends CustomFunction {
    public TestFunction() {
        description = "Get the current weather for a location";
        properties = Map.of(
                "location", Map.of(
                        "description", "The location to get the weather for, e.g. San Francisco, CA",
                        "type", "string"
                ),
                "format", Map.of(
                        "description", "The format to return the weather in, e.g. 'celsius' or 'fahrenheit'",
                        "type", "string",
                        "enum", List.of("celsius", "fahrenheit")
                ));
        required = new String[] { "location", "format" };
    }

    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        String location = (String) args.get("location");
        String format = (String) args.get("format");
        return Map.of("weather", getWeather(location, format));
    }

    private static String getWeather(String location, String format) {
        return "The weather in " + location + " is 20 degrees " + format;
    }
}
