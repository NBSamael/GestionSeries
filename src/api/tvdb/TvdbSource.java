package api.tvdb;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import api.DataSource;
import api.Episode;
import api.Http;
import api.Series;

/**
 * Source de données TheTVDB (API v4).
 */
public class TvdbSource implements DataSource {

	private static final String API = "https://api4.thetvdb.com/v4/";
	private static final String API_LOGIN = API + "login";
	private static final String API_SEARCH = API + "search";
	private static final String API_EPISODES_LIST = API + "series/";

	/* Pages de saisie du site (et non de l'API) */
	private static final String SITE = "https://thetvdb.com/series/";

	/* Ordre des épisodes (default, official, dvd, absolute...) */
	private static final String SEASON_TYPE = "default";
	private static final String FRENCH = "fra";

	private final String apiKey;
	private final String pin;

	private String jwtToken;

	/**
	 * @param pin PIN d'abonné, uniquement nécessaire pour les clés "user-supported" (null sinon)
	 */
	public TvdbSource(String apiKey, String pin) {
		this.apiKey = apiKey;
		this.pin = pin;
	}

	@Override
	public String getName() {
		return "TVDB";
	}

	private HttpPost buildPostRequest(String uri, JSONObject json) throws UnsupportedEncodingException {
		HttpPost postRequest = new HttpPost(uri);
		postRequest.setEntity(new StringEntity(json.toJSONString(), "UTF-8"));
		postRequest.setHeader("Content-Type", "application/json");
		postRequest.setHeader("Accept", "application/json");
		return postRequest;
	}

	private HttpGet buildGetRequest(String uri) {
		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Authorization", "Bearer " + jwtToken);
		return getRequest;
	}

	@Override
	public void connect() throws IOException, ParseException {
		HttpPost postRequest = buildPostRequest(API_LOGIN, TvdbJson.getLogin(apiKey, pin));
		System.out.println("Executing request " + postRequest.getRequestLine());
		String jsonResponse = Http.execute(postRequest);
		if (jsonResponse == null) {
			// 404 sur l'URL de connexion : l'adresse de l'API a changé
			throw new Http.HttpStatusException(404, null);
		}
		jwtToken = TvdbJson.extractToken(jsonResponse);
		// Le jeton n'est pas affiché : il donne accès à l'API avec la clé du projet
		System.out.println("Token stored");
	}

	@Override
	public List<Series> searchByName(String name) throws IOException, ParseException {
		String searchUri = API_SEARCH + "?type=series&query=" + URLEncoder.encode(name, "UTF-8");
		String response = Http.execute(buildGetRequest(searchUri));
		if (response == null) {
			return new ArrayList<>();
		}
		return TvdbJson.extractSeries(response, FRENCH);
	}

	@Override
	public List<Episode> getAllEpisodes(Series series) throws IOException, ParseException {
		Map<Long, Episode> episodes = new HashMap<>();
		long page = 0;
		String next;

		// Chaque page est lue dans la langue d'origine puis complétée par sa version française
		do {
			String originalUri = API_EPISODES_LIST + series.id + "/episodes/" + SEASON_TYPE + "?page=" + page;
			String frenchUri = API_EPISODES_LIST + series.id + "/episodes/" + SEASON_TYPE + "/" + FRENCH + "?page="
					+ page;

			TvdbJson.EpisodesPage originalPage = TvdbJson.extractEpisodes(Http.execute(buildGetRequest(originalUri)));
			String frenchResponse = Http.execute(buildGetRequest(frenchUri));
			if (frenchResponse != null) {
				TvdbJson.addFrenchTranslations(originalPage.episodes, frenchResponse);
			}
			episodes.putAll(originalPage.episodes);
			next = originalPage.next;
			page++;
		} while (next != null);

		return new ArrayList<>(episodes.values());
	}

	@Override
	public List<Episode> getSeasonEpisodes(Series series, int season) throws IOException, ParseException {
		Map<Long, Episode> episodes = new HashMap<>();
		long page = 0;
		String next;

		// Episodes de la saison, dans la langue d'origine
		do {
			String uri = API_EPISODES_LIST + series.id + "/episodes/" + SEASON_TYPE + "?page=" + page + "&season="
					+ season;
			TvdbJson.EpisodesPage originalPage = TvdbJson.extractEpisodes(Http.execute(buildGetRequest(uri)));
			episodes.putAll(originalPage.episodes);
			next = originalPage.next;
			page++;
		} while (next != null);

		// Textes français : la liste en français ignore le filtre de saison, toute la série est parcourue
		page = 0;
		do {
			String frenchUri = API_EPISODES_LIST + series.id + "/episodes/" + SEASON_TYPE + "/" + FRENCH + "?page="
					+ page;
			String frenchResponse = Http.execute(buildGetRequest(frenchUri));
			if (frenchResponse == null) {
				break;
			}
			next = TvdbJson.addFrenchTranslations(episodes, frenchResponse);
			page++;
		} while (next != null);

		List<Episode> result = new ArrayList<>(episodes.values());
		result.sort(Comparator.comparing((Episode e) -> e.number, Comparator.nullsLast(Long::compareTo)));
		return result;
	}

	@Override
	public String frenchTranslationPage(Series series, Episode episode) {
		if (series.slug == null) {
			return null;
		}
		return SITE + series.slug + "/episodes/" + episode.id + "/translate/fra/0/single";
	}

	/** Page d'ajout d'épisodes à une saison (ordre officiel, celui lu par l'application) */
	@Override
	public String addEpisodesPage(Series series, int season) {
		if (series.slug == null) {
			return null;
		}
		return SITE + series.slug + "/seasons/official/" + season + "/bulkadd";
	}
}
