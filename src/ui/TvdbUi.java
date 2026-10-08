package ui;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.JOptionPane;

import api.TvdbConfig;
import api.TvdbErrors;

/**
 * Utilitaires d'interface communs aux appels à TVDB des différents onglets.
 */
final class TvdbUi {

	private TvdbUi() {
	}

	/**
	 * Données de connexion, lues à chaque appel : une correction du fichier est
	 * prise en compte sans relancer. Retourne null après avoir expliqué l'erreur
	 * si la configuration est absente ou incomplète.
	 */
	static TvdbConfig loadConfig(Component parent) {
		try {
			return TvdbConfig.load();
		} catch (TvdbConfig.ConfigException e) {
			JOptionPane.showMessageDialog(parent, e.getMessage(), "Configuration TVDB", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	/** Explique à l'utilisateur l'échec d'un appel à TVDB ; le détail technique reste disponible en dessous */
	static void showError(Component parent, String title, Exception error) {
		String message = TvdbErrors.describe(error) + "\n\nDétail technique : " + error.getClass().getSimpleName()
				+ (error.getMessage() != null ? " - " + error.getMessage() : "");
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/** Affiche le curseur d'attente sur la fenêtre pendant les appels à TVDB (qui bloquent l'interface) */
	static void setBusy(Component window, boolean busy) {
		window.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
	}
}
