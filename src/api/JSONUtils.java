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
	private static final String LOGIN_PIN = "pin";
	private static final String LOGIN_TOKEN = "token";

	// JSONObject (json-simple) hérite d'un HashMap non typé
	@SuppressWarnings("unchecked")
	public static JSONObject getLogin(String apiKey, String pin) {
		JSONObject loginObject = new JSONObject();
		loginObject.put(LOGIN_API_KEY, apiKey);
		if (pin != null) {
			loginObject.put(LOGIN_PIN, pin);
		}
		return loginObject;
	}

	public static String extractToken(String jsonResponse) throws ParseException {
		JSONObject responseObject = (JSONObject) new JSONParser().parse(jsonResponse);
		JSONObject data = (JSONObject) responseObject.get("data");
		return (String) data.get(LOGIN_TOKEN);
	}

	/**
	 * Retourne la traduction dans la langue demandée si elle existe, sinon la
	 * valeur par défaut.
	 */
	private static String translated(JSONObject json, String translationsKey, String lang, String defaultValue) {
		JSONObject translations = (JSONObject) json.get(translationsKey);
		if (translations != null && translations.get(lang) != null) {
			return (String) translations.get(lang);
		}
		return defaultValue;
	}

	public static List<TvdbSerie> extractSeries(String response, String lang) throws ParseException {
		Map<Long, TvdbSerie> series = new HashMap<>();
		JSONObject seriesArray = (JSONObject) new JSONParser().parse(response);
		if (seriesArray != null && seriesArray.get("data") != null) {
			for (Object serie : (JSONArray) seriesArray.get("data")) {
				JSONObject serieJson = (JSONObject) serie;
				TvdbSerie s = new TvdbSerie();
				JSONArray aliases = (JSONArray) serieJson.get("aliases");
				s.aliases = aliases != null ? aliases.toArray() : new Object[0];
				s.banner = (String) serieJson.get("image_url");
				s.firstAired = (String) serieJson.get("first_air_time");
				s.id = Long.valueOf((String) serieJson.get("tvdb_id"));
				s.network = (String) serieJson.get("network");
				s.overview = translated(serieJson, "overviews", lang, (String) serieJson.get("overview"));
				s.seriesName = translated(serieJson, "translations", lang, (String) serieJson.get("name"));
				s.status = (String) serieJson.get("status");
				series.put(s.id, s);
			}
		}

		return new ArrayList<>(series.values());
	}

	private static JSONArray getEpisodesArray(JSONObject response) {
		if (response == null || response.get("data") == null) {
			return new JSONArray();
		}
		JSONArray episodes = (JSONArray) ((JSONObject) response.get("data")).get("episodes");
		return episodes != null ? episodes : new JSONArray();
	}

	public static TvdbSeriesEpisodes extractEpisodes(String frenchResponse, String originalResponse)
			throws ParseException {
		TvdbSeriesEpisodes episodes = new TvdbSeriesEpisodes();
		episodes.tvdbBasicEpisodes = new HashMap<>();
		episodes.tvdblinks = new TvdbLink();

		if (originalResponse == null) {
			return episodes;
		}

		JSONObject originalEpisodesArray = (JSONObject) new JSONParser().parse(originalResponse);

		for (Object episode : getEpisodesArray(originalEpisodesArray)) {
			JSONObject episodeJson = (JSONObject) episode;
			TvdbBasicEpisode tvdbBasicEpisode = new TvdbBasicEpisode();
			tvdbBasicEpisode.absoluteNumber = (Long) episodeJson.get("absoluteNumber");
			tvdbBasicEpisode.airedEpisodeNumber = (Long) episodeJson.get("number");
			tvdbBasicEpisode.airedSeason = (Long) episodeJson.get("seasonNumber");
			tvdbBasicEpisode.episodeName = (String) episodeJson.get("name");
			tvdbBasicEpisode.firstAired = (String) episodeJson.get("aired");
			tvdbBasicEpisode.id = (Long) episodeJson.get("id");
			tvdbBasicEpisode.lastUpdated = (String) episodeJson.get("lastUpdated");
			tvdbBasicEpisode.overview = (String) episodeJson.get("overview");
			episodes.tvdbBasicEpisodes.put(tvdbBasicEpisode.id, tvdbBasicEpisode);
		}

		if (frenchResponse != null) {
			JSONObject frenchEpisodesArray = (JSONObject) new JSONParser().parse(frenchResponse);
			for (Object episode : getEpisodesArray(frenchEpisodesArray)) {
				JSONObject episodeJson = (JSONObject) episode;
				TvdbBasicEpisode tvdbBasicEpisode = episodes.tvdbBasicEpisodes.get((Long) episodeJson.get("id"));
				if (tvdbBasicEpisode == null) {
					continue;
				}
				String episodeName = (String) episodeJson.get("name");
				String overview = (String) episodeJson.get("overview");
				if (episodeName != null) {
					tvdbBasicEpisode.episodeName = episodeName;
				}
				if (overview != null) {
					tvdbBasicEpisode.overview = overview;
				}
			}
		}

		JSONObject linksJson = (JSONObject) originalEpisodesArray.get("links");
		if (linksJson != null) {
			episodes.tvdblinks.prev = (String) linksJson.get("prev");
			episodes.tvdblinks.self = (String) linksJson.get("self");
			episodes.tvdblinks.next = (String) linksJson.get("next");
			episodes.tvdblinks.totalItems = (Long) linksJson.get("total_items");
			episodes.tvdblinks.pageSize = (Long) linksJson.get("page_size");
		}

		return episodes;
	}
}
