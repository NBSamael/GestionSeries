package api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
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

	private static final String API = "https://api.thetvdb.com/";
	private static final String API_LOGIN = API + "login";
	private static final String API_SEARCH = API + "search/series";
	private static final String API_EPISODES_LIST = API + "series/";

	private String API_KEY;
	private String USER_KEY;
	private String USERNAME;

	private String jwtToken;

	public TvdbEndpoint(String apiKey, String username, String userKey) {
		API_KEY = apiKey;
		USERNAME = username;
		USER_KEY = userKey;
	}

	private HttpPost buildPostRequest(String uri, JSONObject json) throws UnsupportedEncodingException {
		HttpPost postRequest = new HttpPost(uri);
		postRequest.setEntity(new StringEntity(json.toJSONString()));
		postRequest.setHeader("Content-Type", "application/json");
		postRequest.setHeader("Accept", "application/json");
		return postRequest;
	}

	private HttpGet buildGetRequest(String uri) {
		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Content-Type", "application/json");
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Authorization", "Bearer " + jwtToken);
		return getRequest;
	}

	private String execute(HttpUriRequest request) throws ParseException, IOException {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			CloseableHttpResponse response = httpclient.execute(request);
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			String jsonResponse = (entity != null ? EntityUtils.toString(entity) : null);
			if (status >= 200 && status < 300) {
				return jsonResponse;
			} else {
				throw new ClientProtocolException(
						"Unexpected response status: " + status + " - reponse: " + jsonResponse);
			}
		}
	}

	public void login() throws ParseException, IOException, org.json.simple.parser.ParseException {
		HttpPost postRequest = buildPostRequest(API_LOGIN, JSONUtils.getLogin(API_KEY, USERNAME, USER_KEY));
		System.out.println("Executing request " + postRequest.getRequestLine());
		String jsonResponse = execute(postRequest);
		jwtToken = JSONUtils.extractToken(jsonResponse);
		System.out.println("Token " + jwtToken + " stored");
	}

	public List<TvdbSerie> searchByName(String name)
			throws ParseException, IOException, org.json.simple.parser.ParseException {
		String searchUri = API_SEARCH + "?name=" + name;
		HttpGet searchRequest = buildGetRequest(searchUri);
		String response = execute(searchRequest);
		return JSONUtils.extractSeries(response);
	}

	public TvdbSeriesEpisodes getEpisodesList(Long tvShowId)
			throws ParseException, IOException, org.json.simple.parser.ParseException {
		String searchUri = API_EPISODES_LIST + tvShowId + "/episodes";
		HttpGet searchRequest = buildGetRequest(searchUri);
		searchRequest.setHeader("Accept-Language", "fr");
		String frenchResponse = execute(searchRequest);

		searchRequest = buildGetRequest(searchUri);
		String englishResponse = execute(searchRequest);
		// TODO : Gérer la pagination pour récupérer tous les résultats

		return JSONUtils.extractEpisodes(frenchResponse, englishResponse);
	}
}
