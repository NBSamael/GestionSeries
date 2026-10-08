package api.tvdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import api.Episode;
import api.Series;

/**
 * Lecture des réponses JSON de l'API TVDB v4.
 */
final class TvdbJson {

	/* Authentication API parameters */
	private static final String LOGIN_API_KEY = "apikey";
	private static final String LOGIN_PIN = "pin";
	private static final String LOGIN_TOKEN = "token";

	/** Une page de la liste des épisodes */
	static class EpisodesPage {
		// Episodes de la page, par identifiant
		final Map<Long, Episode> episodes = new HashMap<>();
		// Adresse de la page suivante, null si c'est la dernière
		String next;
	}

	private TvdbJson() {
	}

	// JSONObject (json-simple) hérite d'un HashMap non typé
	@SuppressWarnings("unchecked")
	static JSONObject getLogin(String apiKey, String pin) {
		JSONObject loginObject = new JSONObject();
		loginObject.put(LOGIN_API_KEY, apiKey);
		if (pin != null) {
			loginObject.put(LOGIN_PIN, pin);
		}
		return loginObject;
	}

	static String extractToken(String jsonResponse) throws ParseException {
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

	static List<Series> extractSeries(String response, String lang) throws ParseException {
		Map<Long, Series> series = new HashMap<>();
		JSONObject seriesArray = (JSONObject) new JSONParser().parse(response);
		if (seriesArray != null && seriesArray.get("data") != null) {
			for (Object serie : (JSONArray) seriesArray.get("data")) {
				JSONObject serieJson = (JSONObject) serie;
				Series s = new Series();
				s.firstAired = (String) serieJson.get("first_air_time");
				s.id = Long.valueOf((String) serieJson.get("tvdb_id"));
				s.network = (String) serieJson.get("network");
				s.overview = translated(serieJson, "overviews", lang, (String) serieJson.get("overview"));
				s.name = translated(serieJson, "translations", lang, (String) serieJson.get("name"));
				s.status = (String) serieJson.get("status");
				s.slug = (String) serieJson.get("slug");
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

	/** Episodes d'une page de la liste dans la langue d'origine ; page vide si la réponse est null */
	static EpisodesPage extractEpisodes(String originalResponse) throws ParseException {
		EpisodesPage page = new EpisodesPage();
		if (originalResponse == null) {
			return page;
		}

		JSONObject originalEpisodesArray = (JSONObject) new JSONParser().parse(originalResponse);
		for (Object episode : getEpisodesArray(originalEpisodesArray)) {
			JSONObject episodeJson = (JSONObject) episode;
			Episode e = new Episode();
			e.number = (Long) episodeJson.get("number");
			e.season = (Long) episodeJson.get("seasonNumber");
			e.name = (String) episodeJson.get("name");
			e.firstAired = (String) episodeJson.get("aired");
			e.id = (Long) episodeJson.get("id");
			e.overview = (String) episodeJson.get("overview");
			page.episodes.put(e.id, e);
		}
		page.next = nextPage(originalEpisodesArray);
		return page;
	}

	/**
	 * Renseigne les textes français des épisodes déjà lus à partir d'une page de
	 * la liste en français ; les épisodes de la page absents de la liste sont
	 * ignorés.
	 *
	 * @return l'adresse de la page suivante, null si c'est la dernière
	 */
	static String addFrenchTranslations(Map<Long, Episode> episodes, String frenchResponse) throws ParseException {
		JSONObject frenchEpisodesArray = (JSONObject) new JSONParser().parse(frenchResponse);
		for (Object episode : getEpisodesArray(frenchEpisodesArray)) {
			JSONObject episodeJson = (JSONObject) episode;
			Episode e = episodes.get((Long) episodeJson.get("id"));
			if (e == null) {
				continue;
			}
			// L'API renvoie null quand la traduction n'existe pas
			String episodeName = (String) episodeJson.get("name");
			String overview = (String) episodeJson.get("overview");
			e.frenchName = episodeName;
			e.frenchOverview = overview;
			if (episodeName != null) {
				e.name = episodeName;
			}
			if (overview != null) {
				e.overview = overview;
			}
		}
		return nextPage(frenchEpisodesArray);
	}

	private static String nextPage(JSONObject response) {
		JSONObject linksJson = (JSONObject) response.get("links");
		return linksJson != null ? (String) linksJson.get("next") : null;
	}
}
