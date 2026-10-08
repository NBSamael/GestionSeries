package api;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.conn.ConnectTimeoutException;

/**
 * Traduit les erreurs des appels à TVDB en messages compréhensibles par
 * l'utilisateur.
 */
public final class TvdbErrors {

	private TvdbErrors() {
	}

	public static String describe(Throwable error) {
		// Les causes sont parcourues : une erreur réseau peut être enveloppée dans une autre
		for (Throwable e = error; e != null; e = e.getCause()) {
			String message = describeOne(e);
			if (message != null) {
				return message;
			}
		}
		return "Erreur inattendue lors de l'appel au serveur TVDB.";
	}

	private static String describeOne(Throwable e) {
		if (e instanceof UnknownHostException) {
			return "Impossible de joindre le serveur TVDB : vérifiez votre connexion Internet.";
		}
		if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
			return "Le serveur TVDB ne répond pas (délai d'attente dépassé). Réessayez plus tard.";
		}
		if (e instanceof ConnectException || e instanceof NoRouteToHostException) {
			return "La connexion au serveur TVDB a échoué : vérifiez votre connexion Internet, "
					+ "ou un éventuel pare-feu ou proxy.";
		}
		if (e instanceof SSLException) {
			return "La connexion sécurisée avec le serveur TVDB a échoué.";
		}
		if (e instanceof TvdbEndpoint.HttpStatusException) {
			int status = ((TvdbEndpoint.HttpStatusException) e).getStatus();
			if (status == 401 || status == 403) {
				return "Le serveur TVDB a refusé la connexion (erreur " + status + ") : "
						+ "la clé API est invalide ou expirée, ou un PIN d'abonné est nécessaire.";
			}
			if (status == 404) {
				return "L'adresse de l'API TVDB est introuvable (erreur 404) : l'API a peut-être changé.";
			}
			if (status == 429) {
				return "Trop de requêtes envoyées au serveur TVDB (erreur 429). Patientez quelques instants.";
			}
			if (status >= 500) {
				return "Le serveur TVDB rencontre un problème (erreur " + status + "). Réessayez plus tard.";
			}
			return "Réponse inattendue du serveur TVDB (erreur " + status + ").";
		}
		if (e instanceof org.json.simple.parser.ParseException || e instanceof ClassCastException) {
			return "La réponse du serveur TVDB est illisible : le format de l'API a peut-être changé.";
		}
		return null;
	}
}
