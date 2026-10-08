package ui;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.JOptionPane;

import api.ApiConfig;
import api.ApiErrors;
import api.DataSource;
import api.DataSourceType;

/**
 * Utilitaires d'interface communs aux appels à la source de données des
 * différents onglets.
 */
final class SourceUi {

	private SourceUi() {
	}

	/**
	 * Crée la source de données choisie à partir de la configuration, lue à chaque appel :
	 * une correction du fichier est prise en compte sans relancer. Retourne null
	 * après avoir expliqué l'erreur si la configuration est absente ou incomplète.
	 */
	static DataSource createSource(Component parent, DataSourceType type) {
		try {
			return type.create(ApiConfig.load());
		} catch (ApiConfig.ConfigException e) {
			JOptionPane.showMessageDialog(parent, e.getMessage(), "Configuration " + type.getLabel(),
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	/** Explique à l'utilisateur l'échec d'un appel à la source ; le détail technique reste disponible en dessous */
	static void showError(Component parent, String title, Exception error, DataSource source) {
		String message = ApiErrors.describe(error, source.getName()) + "\n\nDétail technique : "
				+ error.getClass().getSimpleName() + (error.getMessage() != null ? " - " + error.getMessage() : "");
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/** Affiche le curseur d'attente sur la fenêtre pendant les appels à la source (qui bloquent l'interface) */
	static void setBusy(Component window, boolean busy) {
		window.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
	}
}
