package api;

import org.json.simple.JSONObject;

public class JSONUtils {

	private static final String LOGIN_API_KEY = "apikey";
	private static final String LOGIN_USERNAME = "username";
	private static final String LOGIN_USER_KEY = "userkey";

	public static JSONObject getLogin(String apiKey, String username, String userKey) {
		JSONObject loginObject = new JSONObject();
		loginObject.put(LOGIN_API_KEY, apiKey);
		loginObject.put(LOGIN_USERNAME, username);
		loginObject.put(LOGIN_USER_KEY, userKey);
		return loginObject;
	}
}
