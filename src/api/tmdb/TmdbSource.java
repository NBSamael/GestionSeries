package api.tmdb;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import api.DataSource;
import api.Episode;
import api.Http;
import api.Series;

/**
 * Source de données TMDB (The Movie Database, API v3).
 *
 * TMDB ne se replie pas sur la langue d'origine quand une traduction manque :
 * chaque saison est lue en français et dans la langue d'origine de la série.
 */
public class TmdbSource implements DataSource {

	private static final String API = "https://api.themoviedb.org/3/";

	/* Pages de saisie du site (et non de l'API) */
	private static final String SITE = "https://www.themoviedb.org/tv/";

	private static final String FRENCH = "fr-FR";
	// Pages de résultats de recherche lues au plus (20 séries par page)
	private static final int MAX_SEARCH_PAGES = 3;
	// Saisons lues en une seule requête (limite de append_to_response)
	private static final int SEASONS_PER_REQUEST = 20;

	private final String apiKey;

	// Langue d'origine de chaque série déjà rencontrée, par identifiant
	private final Map<Long, String> originalLanguages = new HashMap<>();

	public TmdbSource(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String getName() {
		return "TMDB";
	}

	/*
	 * La clé est passée dans l'adresse : les adresses de l'API ne doivent pas être
	 * affichées dans la console
	 */
	private JSONObject get(String path, String query) throws IOException, ParseException {
		String uri = API + path + "?api_key=" + apiKey + (query.isEmpty() ? "" : "&" + query);
		HttpGet request = new HttpGet(uri);
		request.setHeader("Accept", "application/json");
		String response = Http.execute(request);
		return response != null ? TmdbJson.parse(response) : null;
	}

	/** Pas de session à ouvrir : vérifie seulement la clé, pour signaler tout de suite une clé invalide */
	@Override
	public void connect() throws IOException, ParseException {
		get("authentication", "");
	}

	@Override
	public List<Series> searchByName(String name) throws IOException, ParseException {
		List<Series> series = new ArrayList<>();
		String query = "language=" + FRENCH + "&query=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
		int totalPages = 1;
		for (int page = 1; page <= Math.min(totalPages, MAX_SEARCH_PAGES); page++) {
			JSONObject response = get("search/tv", query + "&page=" + page);
			if (response == null) {
				break;
			}
			series.addAll(TmdbJson.extractSeries(response));
			totalPages = TmdbJson.totalPages(response);
		}
		return series;
	}

	@Override
	public List<Episode> getAllEpisodes(Series series) throws IOException, ParseException {
		JSONObject detailsJson = get("tv/" + series.id, "language=" + FRENCH);
		if (detailsJson == null) {
			return new ArrayList<>();
		}
		TmdbJson.SeriesDetails details = TmdbJson.extractDetails(detailsJson);
		originalLanguages.put(series.id, details.originalLanguage);

		// Saisons lues par lots, en français et dans la langue d'origine
		List<Episode> episodes = new ArrayList<>();
		for (int start = 0; start < details.seasons.size(); start += SEASONS_PER_REQUEST) {
			List<Long> batch = details.seasons.subList(start,
					Math.min(start + SEASONS_PER_REQUEST, details.seasons.size()));
			StringBuilder append = new StringBuilder();
			for (Long season : batch) {
				append.append(append.length() == 0 ? "" : ",").append("season/").append(season);
			}
			JSONObject french = get("tv/" + series.id, "language=" + FRENCH + "&append_to_response=" + append);
			JSONObject original = get("tv/" + series.id,
					"language=" + details.originalLanguage + "&append_to_response=" + append);
			for (Long season : batch) {
				String key = "season/" + season;
				episodes.addAll(TmdbJson.extractSeasonEpisodes(french != null ? (JSONObject) french.get(key) : null,
						original != null ? (JSONObject) original.get(key) : null));
			}
		}
		return episodes;
	}

	@Override
	public List<Episode> getSeasonEpisodes(Series series, int season) throws IOException, ParseException {
		JSONObject french = get("tv/" + series.id + "/season/" + season, "language=" + FRENCH);
		if (french == null) {
			// Saison inexistante sur TMDB
			return new ArrayList<>();
		}
		JSONObject original = get("tv/" + series.id + "/season/" + season,
				"language=" + originalLanguage(series));
		List<Episode> episodes = TmdbJson.extractSeasonEpisodes(french, original);
		episodes.sort(Comparator.comparing((Episode e) -> e.number, Comparator.nullsLast(Long::compareTo)));
		return episodes;
	}

	// Langue d'origine de la série, lue dans sa fiche au premier besoin
	private String originalLanguage(Series series) throws IOException, ParseException {
		String language = originalLanguages.get(series.id);
		if (language == null) {
			JSONObject details = get("tv/" + series.id, "");
			language = details != null ? TmdbJson.extractDetails(details).originalLanguage : null;
			if (language == null) {
				language = "en";
			}
			originalLanguages.put(series.id, language);
		}
		return language;
	}

	/** Les pages du site désignent l'épisode par ses numéros TMDB (et non par son identifiant) */
	@Override
	public String frenchTranslationPage(Series series, Episode episode) {
		if (episode.season == null || episode.number == null) {
			return null;
		}
		return SITE + series.id + "/season/" + episode.season + "/episode/" + episode.number
				+ "/edit?active_nav_item=primary_facts&language=" + FRENCH;
	}

	@Override
	public String addEpisodesPage(Series series, int season) {
		return SITE + series.id + "/season/" + season + "/edit?active_nav_item=episodes";
	}
}
