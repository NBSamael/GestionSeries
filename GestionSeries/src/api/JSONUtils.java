package api;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
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

	public static List<TvdbSerie> extractSeries(String jsonResponse) throws ParseException {
		List<TvdbSerie> series = new ArrayList<>();
		JSONObject seriesArray = (JSONObject) new JSONParser().parse(jsonResponse);
		for (Object serie : (JSONArray) seriesArray.get("data")) {
			JSONObject serieJson = (JSONObject) serie;
			TvdbSerie s = new TvdbSerie();
			s.aliases = (Object[]) ((JSONArray) serieJson.get("aliases")).toArray();
			s.banner = (String) serieJson.get("banner");
			s.firstAired = (String) serieJson.get("firstAired");
			s.id = (Long) serieJson.get("id");
			s.network = (String) serieJson.get("network");
			s.overview = (String) serieJson.get("overview");
			s.seriesName = (String) serieJson.get("seriesName");
			s.status = (String) serieJson.get("status");
			series.add(s);
		}
		return series;
	}
}
