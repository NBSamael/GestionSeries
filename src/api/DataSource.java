package api;

import java.io.IOException;
import java.util.List;

import org.json.simple.parser.ParseException;

/**
 * Source de données des séries (TVDB, etc.) : recherche, épisodes et adresses
 * des pages de saisie de son site.
 */
public interface DataSource {

	/** Nom de la source, tel qu'affiché à l'utilisateur */
	String getName();

	/** Connexion à l'API ; à appeler avant toute autre requête */
	void connect() throws IOException, ParseException;

	/** Séries dont le nom correspond ; liste vide si aucune */
	List<Series> searchByName(String name) throws IOException, ParseException;

	/** Tous les épisodes de la série (renommage) */
	List<Episode> getAllEpisodes(Series series) throws IOException, ParseException;

	/** Episodes d'une saison, triés par numéro, avec leurs textes français séparés (comparaison) */
	List<Episode> getSeasonEpisodes(Series series, int season) throws IOException, ParseException;

	/** Page de saisie de la traduction française d'un épisode, null si elle ne peut être déterminée */
	String frenchTranslationPage(Series series, Episode episode);

	/** Page d'ajout d'épisodes à une saison, null si elle ne peut être déterminée */
	String addEpisodesPage(Series series, int season);
}
