package api;

import java.io.IOException;
import java.util.List;

import org.apache.http.ParseException;

public class TvdbTest {

	private static final String API_KEY = "D4858E1F5CC604F8";
	private static final String USER_KEY = "ECF8D275D64FAADF";
	private static final String USERNAME = "mr_flibble";

	public static void main(String[] args) {
		TvdbEndpoint tvdb = new TvdbEndpoint(API_KEY, USERNAME, USER_KEY);
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

//			series = tvdb.searchByName("doctor");
//			for (TvdbSerie s : series) {
//				System.out.println(s.seriesName + " - " + s.status);
//			}
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
