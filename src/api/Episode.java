package api;

/**
 * Episode d'une série, lu dans une source de données (TVDB, etc.).
 */
public class Episode {
	// Identifiant de l'épisode dans la source
	public Long id;
	// Numéros de saison et d'épisode dans la source ; null s'ils ne sont pas renseignés
	public Long season;
	public Long number;
	public String firstAired;
	// Titre et résumé en français, repliés sur la langue d'origine quand la traduction n'existe pas
	public String name;
	public String overview;
	// Titre et résumé en français seuls : null quand la traduction n'existe pas
	public String frenchName;
	public String frenchOverview;
}
