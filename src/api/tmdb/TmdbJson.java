package api.tmdb;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import api.Episode;
import api.Series;

/**
 * Lecture des réponses JSON de l'API TMDB v3.
 */
final class TmdbJson {

	/*
	 * Titre générique que TMDB enregistre pour un épisode non traduit (« Épisode
	 * 5 » en français, « Episode 5 » en anglais) : ce n'est pas une traduction
	 */
	private static final Pattern GENERIC_NAME = Pattern.compile("^[ÉE]pisode \\d+$", Pattern.CASE_INSENSITIVE);

	/** Informations de la série utiles au chargement des épisodes */
	static class SeriesDetails {
		// Langue d'origine (code ISO 639-1, par exemple "en")
		String originalLanguage;
		// Numéros des saisons, épisodes spéciaux (saison 0) compris
		final List<Long> seasons = new ArrayList<>();
	}

	private TmdbJson() {
	}

	static JSONObject parse(String response) throws ParseException {
		return (JSONObject) new JSONParser().parse(response);
	}

	/** Séries d'une page de résultats de recherche */
	static List<Series> extractSeries(JSONObject response) {
		List<Series> series = new ArrayList<>();
		JSONArray results = (JSONArray) response.get("results");
		if (results == null) {
			return series;
		}
		for (Object result : results) {
			JSONObject json = (JSONObject) result;
			Series s = new Series();
			s.id = (Long) json.get("id");
			// Nom en français, ou d'origine quand la traduction n'existe pas
			s.name = blankToNull((String) json.get("name"));
			if (s.name == null) {
				s.name = (String) json.get("original_name");
			}
			s.firstAired = blankToNull((String) json.get("first_air_date"));
			s.overview = blankToNull((String) json.get("overview"));
			// Réseau et statut ne figurent pas dans les résultats de recherche de TMDB
			series.add(s);
		}
		return series;
	}

	static int totalPages(JSONObject response) {
		Long totalPages = (Long) response.get("total_pages");
		return totalPages != null ? totalPages.intValue() : 1;
	}

	static SeriesDetails extractDetails(JSONObject response) {
		SeriesDetails details = new SeriesDetails();
		details.originalLanguage = (String) response.get("original_language");
		JSONArray seasons = (JSONArray) response.get("seasons");
		if (seasons != null) {
			for (Object season : seasons) {
				Long number = (Long) ((JSONObject) season).get("season_number");
				if (number != null) {
					details.seasons.add(number);
				}
			}
		}
		return details;
	}

	/**
	 * Episodes d'une saison, à partir de sa version française et de sa version
	 * dans la langue d'origine (mêmes épisodes, dans le même ordre).
	 */
	static List<Episode> extractSeasonEpisodes(JSONObject frenchSeason, JSONObject originalSeason) {
		List<Episode> episodes = new ArrayList<>();
		JSONArray frenchEpisodes = episodesOf(frenchSeason);
		JSONArray originalEpisodes = episodesOf(originalSeason);
		for (Object episode : frenchEpisodes) {
			JSONObject french = (JSONObject) episode;
			JSONObject original = findById(originalEpisodes, (Long) french.get("id"));

			Episode e = new Episode();
			e.id = (Long) french.get("id");
			e.season = (Long) french.get("season_number");
			e.number = (Long) french.get("episode_number");
			e.firstAired = blankToNull((String) french.get("air_date"));
			e.frenchName = translation((String) french.get("name"));
			e.frenchOverview = blankToNull((String) french.get("overview"));

			// Repli sur la langue d'origine quand la traduction n'existe pas
			String originalName = original != null ? blankToNull((String) original.get("name")) : null;
			String originalOverview = original != null ? blankToNull((String) original.get("overview")) : null;
			e.name = e.frenchName != null ? e.frenchName : originalName;
			e.overview = e.frenchOverview != null ? e.frenchOverview : originalOverview;
			episodes.add(e);
		}
		return episodes;
	}

	private static JSONArray episodesOf(JSONObject season) {
		JSONArray episodes = season != null ? (JSONArray) season.get("episodes") : null;
		return episodes != null ? episodes : new JSONArray();
	}

	private static JSONObject findById(JSONArray episodes, Long id) {
		for (Object episode : episodes) {
			if (id != null && id.equals(((JSONObject) episode).get("id"))) {
				return (JSONObject) episode;
			}
		}
		return null;
	}

	// Titre traduit ; null s'il est vide ou générique (« Épisode 5 »)
	private static String translation(String name) {
		String value = blankToNull(name);
		return value != null && GENERIC_NAME.matcher(value).matches() ? null : value;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
