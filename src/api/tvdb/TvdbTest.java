package api.tvdb;

import java.io.IOException;
import java.util.List;

import org.json.simple.parser.ParseException;

import api.ApiConfig;
import api.Episode;
import api.Series;

/**
 * Vérifie la connexion à TVDB sans passer par l'interface.
 */
public class TvdbTest {

	public static void main(String[] args) {
		// Données de connexion lues dans gestionseries.properties (voir ApiConfig)
		TvdbSource tvdb;
		try {
			ApiConfig config = ApiConfig.load();
			tvdb = new TvdbSource(config.requireTvdbApiKey(), config.getTvdbPin());
		} catch (ApiConfig.ConfigException e) {
			System.out.println(e.getMessage());
			return;
		}
		try {
			tvdb.connect();
			List<Series> series = tvdb.searchByName("batman");
			for (Series s : series) {
				System.out.println(s.id + " : \t" + s.name + " (" + s.firstAired + ") - " + s.status);

				if ("Batman et Robin (1949)".equals(s.name)) {
					for (Episode episode : tvdb.getAllEpisodes(s)) {
						System.out.println("S" + episode.season + "E" + episode.number + " : " + episode.name);
					}
				}
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

}
