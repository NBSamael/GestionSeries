package api;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Exécution des requêtes HTTP vers les API des sources de données.
 */
public final class Http {

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

	private Http() {
	}

	/**
	 * Exécute la requête et retourne le corps de la réponse, ou null si la
	 * ressource n'existe pas (404).
	 *
	 * @throws HttpStatusException pour toute autre réponse en erreur
	 */
	public static String execute(HttpUriRequest request) throws IOException {
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
}
