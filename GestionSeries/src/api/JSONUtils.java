package api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public static List<TvdbSerie> extractSeries(String frenchResponse, String englishResponse) throws ParseException {
		Map<Long, TvdbSerie> series = new HashMap<>();
		JSONObject seriesArray = (JSONObject) new JSONParser().parse(englishResponse);
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
			series.put(s.id, s);
		}

		seriesArray = (JSONObject) new JSONParser().parse(frenchResponse);
		for (Object serie : (JSONArray) seriesArray.get("data")) {
			JSONObject serieJson = (JSONObject) serie;
			Long id = (Long) serieJson.get("id");
			String overview = (String) serieJson.get("overview");
			String seriesName = (String) serieJson.get("seriesName");
			String status = (String) serieJson.get("status");
			if (overview != null) {
				series.get(id).overview = overview;
			}
			if (seriesName != null) {
				series.get(id).seriesName = seriesName;
			}
			if (status != null) {
				series.get(id).status = status;
			}
		}
		return new ArrayList<>(series.values());
	}

	public static TvdbSeriesEpisodes extractEpisodes(String frenchResponse, String englishResponse)
			throws ParseException {
		TvdbSeriesEpisodes episodes = new TvdbSeriesEpisodes();
		episodes.tvdbBasicEpisodes = new HashMap<>();
		episodes.tvdblinks = new TvdbLink();

		JSONObject englishEpisodesArray = (JSONObject) new JSONParser().parse(englishResponse);

		for (Object episode : (JSONArray) englishEpisodesArray.get("data")) {
			JSONObject episodeJson = (JSONObject) episode;
			TvdbBasicEpisode tvdbBasicEpisode = new TvdbBasicEpisode();
			tvdbBasicEpisode.absoluteNumber = (Long) episodeJson.get("absoluteNumber");
			tvdbBasicEpisode.airedEpisodeNumber = (Long) episodeJson.get("airedEpisodeNumber");
			tvdbBasicEpisode.airedSeason = (Long) episodeJson.get("airedSeason");
			tvdbBasicEpisode.dvdEpisodeNumber = (Long) episodeJson.get("dvdEpisodeNumber");
			tvdbBasicEpisode.dvdSeason = (Long) episodeJson.get("dvdSeason");
			tvdbBasicEpisode.episodeName = (String) episodeJson.get("episodeName");
			tvdbBasicEpisode.firstAired = (String) episodeJson.get("firstAired");
			tvdbBasicEpisode.id = (Long) episodeJson.get("id");
			tvdbBasicEpisode.lastUpdated = (Long) episodeJson.get("lastUpdated");
			tvdbBasicEpisode.overview = (String) episodeJson.get("overview");
			episodes.tvdbBasicEpisodes.put(tvdbBasicEpisode.id, tvdbBasicEpisode);
		}

		JSONObject frenchEpisodesArray = (JSONObject) new JSONParser().parse(frenchResponse);
		for (Object episode : (JSONArray) frenchEpisodesArray.get("data")) {
			JSONObject episodeJson = (JSONObject) episode;
			Long id = (Long) episodeJson.get("id");
			String episodeName = (String) episodeJson.get("episodeName");
			String overview = (String) episodeJson.get("overview");
			if (episodeName != null) {
				episodes.tvdbBasicEpisodes.get(id).episodeName = episodeName;
			}
			if (overview != null) {
				episodes.tvdbBasicEpisodes.get(id).overview = overview;
			}
		}

		JSONObject linksJson = (JSONObject) englishEpisodesArray.get("links");
		episodes.tvdblinks.first = (Long) linksJson.get("first");
		episodes.tvdblinks.last = (Long) linksJson.get("last");
		episodes.tvdblinks.next = (Long) linksJson.get("next");
		episodes.tvdblinks.next = (Long) linksJson.get("prev");

		return episodes;
	}
}
