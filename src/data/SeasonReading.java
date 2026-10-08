package data;

/**
 * Façon de déterminer le numéro de saison des fichiers.
 */
public enum SeasonReading {
	/** Numéro de saison saisi manuellement, identique pour tous les fichiers */
	NONE("Non"),
	/** Numéro de saison lu dans le nom du fichier */
	FILE_NAME("Nom d'épisode"),
	/** Numéro de saison lu dans le nom du dossier (colonne Dossier) */
	FOLDER_NAME("Nom de dossier");

	private final String label;

	SeasonReading(String label) {
		this.label = label;
	}

	/** Indique si le numéro de saison est lu dans un texte (et donc si la position et la taille s'appliquent) */
	public boolean isRead() {
		return this != NONE;
	}

	@Override
	public String toString() {
		return label;
	}
}
