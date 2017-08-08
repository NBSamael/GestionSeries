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

	public static void main(String[] args) {
		new TvdbEndpoint().login();
	}

	public void login() {
		String loginUrl = API + "login";
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpPost postRequest = new HttpPost(loginUrl);
			String loginString = "{\"apikey\":\"D4858E1F5CC604F8\",\"username\":\"mr_flibble\",\"userkey\":\"ECF8D275D64FAADF\"}";
			postRequest.setEntity(new StringEntity(loginString));
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
