package api;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class TvdbEndpoint {

	private static final String API = "https://api.thetvdb.com/";
	private static final String API_LOGIN = API + "login";

	private String API_KEY;
	private String USER_KEY;
	private String USERNAME;

	public TvdbEndpoint(String apiKey, String username, String userKey) {
		API_KEY = apiKey;
		USERNAME = username;
		USER_KEY = userKey;
	}

	public void login() {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpPost postRequest = new HttpPost(API_LOGIN);
			postRequest.setEntity(new StringEntity(JSONUtils.getLogin(API_KEY, USERNAME, USER_KEY).toJSONString()));
			postRequest.setHeader("Content-Type", "application/json");
			postRequest.setHeader("Accept", "application/json");
			System.out.println("Executing request " + postRequest.getRequestLine());

			// Create a custom response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity) : null;
					} else {
						System.out.println(response.getEntity().toString());
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}

			};
			String responseBody = httpclient.execute(postRequest, responseHandler);
			System.out.println("----------------------------------------");
			System.out.println(responseBody);

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
