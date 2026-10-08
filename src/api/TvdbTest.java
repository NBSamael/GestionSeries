package api;

import java.io.IOException;
import java.util.List;

import org.apache.http.ParseException;

public class TvdbTest {

	public static void main(String[] args) {
		// Données de connexion lues dans gestionseries.properties (voir TvdbConfig)
		TvdbConfig config;
		try {
			config = TvdbConfig.load();
		} catch (TvdbConfig.ConfigException e) {
			System.out.println(e.getMessage());
			return;
		}
		TvdbEndpoint tvdb = new TvdbEndpoint(config.getApiKey(), config.getPin());
		try {
			tvdb.login();
			List<TvdbSerie> series = tvdb.searchByName("batman");
			for (TvdbSerie s : series) {
				System.out.println(s.id + " : \t" + s.seriesName + " (" + s.firstAired + ") - " + s.status);

				if ("Batman et Robin (1949)".equals(s.seriesName)) {
					TvdbSeriesEpisodes episodes = tvdb.getEpisodesList(s.id);
					for (TvdbBasicEpisode tvdbBasicEpisode : episodes.tvdbBasicEpisodes.values()) {
						System.out.println("S" + tvdbBasicEpisode.airedSeason + "E"
								+ tvdbBasicEpisode.airedEpisodeNumber + " : " + tvdbBasicEpisode.episodeName);
					}
				}
			}

			// series = tvdb.searchByName("doctor");
			// for (TvdbSerie s : series) {
			// System.out.println(s.seriesName + " - " + s.status);
			// }
			//
			// series = tvdb.searchByName("stargate");
			// for (TvdbSerie s : series) {
			// System.out.println(s.seriesName + " - " + s.status);
			// }

			// TODO : handle exceptions
			// series = tvdb.searchByName("azeaadadz");

		} catch (ParseException | IOException | org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
