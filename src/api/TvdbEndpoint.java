package api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

public class TvdbEndpoint {

	private static final String API = "https://api4.thetvdb.com/v4/";
	private static final String API_LOGIN = API + "login";
	private static final String API_SEARCH = API + "search";
	private static final String API_EPISODES_LIST = API + "series/";

	/* Ordre des épisodes (default, official, dvd, absolute...) */
	private static final String SEASON_TYPE = "default";
	private static final String FRENCH = "fra";

	/* Délais d'attente des appels réseau, pour ne pas bloquer l'application si le serveur ne répond pas */
	private static final int CONNECT_TIMEOUT_MS = 10_000;
	private static final int RESPONSE_TIMEOUT_MS = 30_000;
	private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
			.setConnectTimeout(CONNECT_TIMEOUT_MS)
			.setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
			.setSocketTimeout(RESPONSE_TIMEOUT_MS)
			.build();

	/**
	 * Réponse HTTP en erreur (hors 404, traité comme "aucun résultat") ; le code
	 * permet d'expliquer l'erreur à l'utilisateur.
	 */
	public static class HttpStatusException extends ClientProtocolException {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		private final int status;

		public HttpStatusException(int status, String response) {
			super("Unexpected response status: " + status + " - reponse: " + response);
			this.status = status;
		}

		public int getStatus() {
			return status;
		}
	}

	private String API_KEY;
	private String PIN;

	private String jwtToken;

	public TvdbEndpoint(String apiKey) {
		this(apiKey, null);
	}

	/**
	 * @param pin PIN d'abonné, uniquement nécessaire pour les clés "user-supported"
	 */
	public TvdbEndpoint(String apiKey, String pin) {
		API_KEY = apiKey;
		PIN = pin;
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

	private String execute(HttpUriRequest request) throws ParseException, IOException {
		try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(REQUEST_CONFIG).build()) {
			CloseableHttpResponse response = httpclient.execute(request);
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			String jsonResponse = (entity != null ? EntityUtils.toString(entity, "UTF-8") : null);
			if (status >= 200 && status < 300) {
				return jsonResponse;
			} else if (status == 404) {
				return null;
			} else {
				throw new HttpStatusException(status, jsonResponse);
			}
		}
	}

	public void login() throws ParseException, IOException, org.json.simple.parser.ParseException {
		HttpPost postRequest = buildPostRequest(API_LOGIN, JSONUtils.getLogin(API_KEY, PIN));
		System.out.println("Executing request " + postRequest.getRequestLine());
		String jsonResponse = execute(postRequest);
		if (jsonResponse == null) {
			// 404 sur l'URL de connexion : l'adresse de l'API a changé
			throw new HttpStatusException(404, null);
		}
		jwtToken = JSONUtils.extractToken(jsonResponse);
		// Le jeton n'est pas affiché : il donne accès à l'API avec la clé du projet
		System.out.println("Token stored");
	}

	public List<TvdbSerie> searchByName(String name)
			throws ParseException, IOException, org.json.simple.parser.ParseException {
		String searchUri = API_SEARCH + "?type=series&query=" + URLEncoder.encode(name, "UTF-8");
		String response = execute(buildGetRequest(searchUri));

		if (response == null) {
			return null;
		}

		List<TvdbSerie> result = JSONUtils.extractSeries(response, FRENCH);
		return result.isEmpty() ? null : result;
	}

	public TvdbSeriesEpisodes getEpisodesList(Long tvShowId)
			throws ParseException, IOException, org.json.simple.parser.ParseException {
		TvdbSeriesEpisodes result = null;
		long page = 0;

		do {
			String originalUri = API_EPISODES_LIST + tvShowId + "/episodes/" + SEASON_TYPE + "?page=" + page;
			String frenchUri = API_EPISODES_LIST + tvShowId + "/episodes/" + SEASON_TYPE + "/" + FRENCH + "?page="
					+ page;

			String originalResponse = execute(buildGetRequest(originalUri));
			String frenchResponse = execute(buildGetRequest(frenchUri));

			TvdbSeriesEpisodes temp = JSONUtils.extractEpisodes(frenchResponse, originalResponse);
			if (result == null) {
				result = temp;
			} else {
				result.tvdbBasicEpisodes.putAll(temp.tvdbBasicEpisodes);
				result.tvdblinks = temp.tvdblinks;
			}
			page++;
		} while (result.tvdblinks.next != null);

		return result;
	}
}
