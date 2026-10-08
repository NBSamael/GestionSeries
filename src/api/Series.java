package api;

/**
 * Série trouvée dans une source de données (TVDB, etc.).
 */
public class Series {
	// Identifiant de la série dans la source
	public Long id;
	// Nom en français, ou dans la langue d'origine à défaut
	public String name;
	public String firstAired;
	public String network;
	public String overview;
	public String status;
	// Identifiant de la série dans les adresses du site de la source (null si la source n'en utilise pas)
	public String slug;
}
