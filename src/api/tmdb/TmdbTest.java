package api.tmdb;

import java.io.IOException;
import java.util.List;

import org.json.simple.parser.ParseException;

import api.ApiConfig;
import api.Episode;
import api.Series;

/**
 * Vérifie la connexion à TMDB sans passer par l'interface.
 */
public class TmdbTest {

	public static void main(String[] args) {
		// Clé lue dans gestionseries.properties (voir ApiConfig)
		TmdbSource tmdb;
		try {
			tmdb = new TmdbSource(ApiConfig.load().requireTmdbApiKey());
		} catch (ApiConfig.ConfigException e) {
			System.out.println(e.getMessage());
			return;
		}
		try {
			tmdb.connect();
			List<Series> series = tmdb.searchByName("Love Is Blind UK");
			for (Series s : series) {
				System.out.println(s.id + " : \t" + s.name + " (" + s.firstAired + ")");
			}
			if (!series.isEmpty()) {
				for (Episode episode : tmdb.getAllEpisodes(series.get(0))) {
					System.out.println("S" + episode.season + "E" + episode.number + " : " + episode.name
							+ (episode.frenchName == null ? "  (pas de titre français)" : ""));
				}
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

}
