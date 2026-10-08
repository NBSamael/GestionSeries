package api;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.conn.ConnectTimeoutException;

/**
 * Traduit les erreurs des appels aux sources de données en messages
 * compréhensibles par l'utilisateur.
 */
public final class ApiErrors {

	private ApiErrors() {
	}

	/**
	 * @param source nom de la source appelée (par exemple "TVDB"), repris dans le message
	 */
	public static String describe(Throwable error, String source) {
		// Les causes sont parcourues : une erreur réseau peut être enveloppée dans une autre
		for (Throwable e = error; e != null; e = e.getCause()) {
			String message = describeOne(e, source);
			if (message != null) {
				return message;
			}
		}
		return "Erreur inattendue lors de l'appel au serveur " + source + ".";
	}

	private static String describeOne(Throwable e, String source) {
		if (e instanceof UnknownHostException) {
			return "Impossible de joindre le serveur " + source + " : vérifiez votre connexion Internet.";
		}
		if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
			return "Le serveur " + source + " ne répond pas (délai d'attente dépassé). Réessayez plus tard.";
		}
		if (e instanceof ConnectException || e instanceof NoRouteToHostException) {
			return "La connexion au serveur " + source + " a échoué : vérifiez votre connexion Internet, "
					+ "ou un éventuel pare-feu ou proxy.";
		}
		if (e instanceof SSLException) {
			return "La connexion sécurisée avec le serveur " + source + " a échoué.";
		}
		if (e instanceof Http.HttpStatusException) {
			int status = ((Http.HttpStatusException) e).getStatus();
			if (status == 401 || status == 403) {
				return "Le serveur " + source + " a refusé la connexion (erreur " + status + ") : "
						+ "la clé API est invalide ou expirée"
						// Le PIN d'abonné n'existe que pour TVDB
						+ ("TVDB".equals(source) ? ", ou un PIN d'abonné est nécessaire." : ".");
			}
			if (status == 404) {
				return "L'adresse de l'API " + source + " est introuvable (erreur 404) : l'API a peut-être changé.";
			}
			if (status == 429) {
				return "Trop de requêtes envoyées au serveur " + source + " (erreur 429). Patientez quelques instants.";
			}
			if (status >= 500) {
				return "Le serveur " + source + " rencontre un problème (erreur " + status + "). Réessayez plus tard.";
			}
			return "Réponse inattendue du serveur " + source + " (erreur " + status + ").";
		}
		if (e instanceof org.json.simple.parser.ParseException || e instanceof ClassCastException) {
			return "La réponse du serveur " + source + " est illisible : le format de l'API a peut-être changé.";
		}
		return null;
	}
}
