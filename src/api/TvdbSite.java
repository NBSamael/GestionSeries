package api;

/**
 * Adresses des pages de saisie du site thetvdb.com (et non de l'API). La
 * saisie elle-même se fait sur le site, connecté à son compte TVDB.
 */
public final class TvdbSite {

	private static final String SITE = "https://thetvdb.com/series/";

	private TvdbSite() {
	}

	/** Page de saisie de la traduction française (titre et résumé) d'un épisode */
	public static String frenchTranslationPage(String seriesSlug, long episodeId) {
		return SITE + seriesSlug + "/episodes/" + episodeId + "/translate/fra/0/single";
	}

	/** Page d'ajout d'épisodes à une saison (ordre officiel, celui lu par l'application) */
	public static String addEpisodesPage(String seriesSlug, int season) {
		return SITE + seriesSlug + "/seasons/official/" + season + "/bulkadd";
	}
}
