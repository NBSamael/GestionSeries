package api;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONUtils {

	/* Authentication API parameters */
	private static final String LOGIN_API_KEY = "apikey";
	private static final String LOGIN_USERNAME = "username";
	private static final String LOGIN_USER_KEY = "userkey";
	private static final String LOGIN_TOKEN = "token";

	public static JSONObject getLogin(String apiKey, String username, String userKey) {
		JSONObject loginObject = new JSONObject();
		loginObject.put(LOGIN_API_KEY, apiKey);
		loginObject.put(LOGIN_USERNAME, username);
		loginObject.put(LOGIN_USER_KEY, userKey);
		return loginObject;
	}

	public static String extractToken(String jsonResponse) throws ParseException {
		JSONObject tokenObject = (JSONObject) new JSONParser().parse(jsonResponse);
		return (String) tokenObject.get(LOGIN_TOKEN);
	}
}
