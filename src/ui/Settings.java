package ui;

import java.io.File;
import java.util.prefs.Preferences;

import api.DataSourceType;

/**
 * Préférences de l'application, mémorisées d'un lancement à l'autre dans le
 * profil de l'utilisateur (Preferences Java : registre sous Windows). Les clés
 * d'API restent dans gestionseries.properties.
 */
final class Settings {

	private static final Preferences PREFERENCES = Preferences.userRoot().node("gestionseries");

	private static final String KEY_DATA_SOURCE = "dataSource";
	private static final DataSourceType DEFAULT_DATA_SOURCE = DataSourceType.TVDB;
	private static final String KEY_SCAN_DIRECTORY = "scanDirectory";

	private Settings() {
	}

	/** Source de données choisie ; TVDB par défaut, ou si la valeur mémorisée n'est plus reconnue */
	static DataSourceType getDataSource() {
		String value = PREFERENCES.get(KEY_DATA_SOURCE, DEFAULT_DATA_SOURCE.name());
		try {
			return DataSourceType.valueOf(value);
		} catch (IllegalArgumentException e) {
			return DEFAULT_DATA_SOURCE;
		}
	}

	static void setDataSource(DataSourceType source) {
		PREFERENCES.put(KEY_DATA_SOURCE, source.name());
	}

	/** Dossier proposé par « Scanner Dossier » ; null si aucun n'est choisi (dossier par défaut du système) */
	static File getScanDirectory() {
		String path = PREFERENCES.get(KEY_SCAN_DIRECTORY, "").trim();
		return path.isEmpty() ? null : new File(path);
	}

	/** @param directory dossier proposé par « Scanner Dossier », null pour revenir au dossier par défaut */
	static void setScanDirectory(File directory) {
		if (directory == null) {
			PREFERENCES.remove(KEY_SCAN_DIRECTORY);
		} else {
			PREFERENCES.put(KEY_SCAN_DIRECTORY, directory.getPath());
		}
	}
}
