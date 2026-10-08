package data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import api.TvdbBasicEpisode;

/**
 * Numéros d'épisode utilisés pour mettre Netflix et TVDB face à face : ceux
 * d'origine, ou ceux d'une source de référence sur lesquels l'autre source est
 * renumérotée. La renumérotation ne sert qu'à la comparaison : ni le CSV ni
 * TVDB ne sont modifiés.
 */
public class EpisodeNumbering {

	/** Source dont les numéros sont conservés */
	public enum Reference {
		ORIGINAL, NETFLIX, TVDB
	}

	private final Map<NetflixEpisode, Integer> netflixNumbers = new IdentityHashMap<>();
	private final Map<TvdbBasicEpisode, Integer> tvdbNumbers = new IdentityHashMap<>();
	private int anchorCount;

	private EpisodeNumbering() {
	}

	/** Numéro de l'épisode Netflix pour la comparaison */
	public int number(NetflixEpisode episode) {
		return netflixNumbers.getOrDefault(episode, episode.number);
	}

	/** Numéro de l'épisode TVDB pour la comparaison */
	public int number(TvdbBasicEpisode episode) {
		return tvdbNumbers.getOrDefault(episode, episode.airedEpisodeNumber.intValue());
	}

	/** Nombre de paires d'épisodes rapprochées par leur titre (0 : aucune renumérotation possible) */
	public int getAnchorCount() {
		return anchorCount;
	}

	/**
	 * Calcule la numérotation, saison par saison.
	 * <ol>
	 * <li>Les épisodes dont les titres correspondent (ponctuation ignorée) sont
	 * rapprochés, dans l'ordre et sans croisement : ce sont les ancrages.</li>
	 * <li>Entre deux ancrages, les épisodes restants des deux sources sont
	 * rapprochés dans l'ordre (même épisode, titre différent ou non traduit).</li>
	 * <li>L'épisode renuméroté prend le numéro de son correspondant dans la source
	 * de référence ; les épisodes en trop sont numérotés après le dernier épisode
	 * de la saison, dans leur ordre d'origine.</li>
	 * </ol>
	 * Les épisodes TVDB sans numéro de saison ou d'épisode sont ignorés.
	 */
	public static EpisodeNumbering compute(List<NetflixEpisode> netflixEpisodes, List<TvdbBasicEpisode> tvdbEpisodes,
			Reference reference) {
		EpisodeNumbering numbering = new EpisodeNumbering();
		if (reference == Reference.ORIGINAL) {
			return numbering;
		}

		// Episodes de chaque source, par saison et dans l'ordre des numéros
		Map<Integer, List<NetflixEpisode>> netflixBySeason = new TreeMap<>();
		for (NetflixEpisode episode : netflixEpisodes) {
			netflixBySeason.computeIfAbsent(episode.season, s -> new ArrayList<>()).add(episode);
		}
		Map<Integer, List<TvdbBasicEpisode>> tvdbBySeason = new TreeMap<>();
		for (TvdbBasicEpisode episode : tvdbEpisodes) {
			if (episode.airedSeason != null && episode.airedEpisodeNumber != null) {
				tvdbBySeason.computeIfAbsent(episode.airedSeason.intValue(), s -> new ArrayList<>()).add(episode);
			}
		}

		for (Map.Entry<Integer, List<NetflixEpisode>> entry : netflixBySeason.entrySet()) {
			List<NetflixEpisode> netflix = entry.getValue();
			List<TvdbBasicEpisode> tvdb = tvdbBySeason.getOrDefault(entry.getKey(), new ArrayList<>());
			netflix.sort(Comparator.comparingInt(e -> e.number));
			tvdb.sort(Comparator.comparingLong(e -> e.airedEpisodeNumber));
			numbering.renumberSeason(netflix, tvdb, reference);
		}
		return numbering;
	}

	private void renumberSeason(List<NetflixEpisode> netflix, List<TvdbBasicEpisode> tvdb, Reference reference) {
		// Paires rapprochées : indice Netflix -> indice TVDB
		Map<Integer, Integer> pairs = new TreeMap<>();

		// 1. Ancrages : pour chaque épisode Netflix, premier titre TVDB correspondant après le dernier ancrage
		List<int[]> anchors = new ArrayList<>();
		int lastTvdb = -1;
		for (int i = 0; i < netflix.size(); i++) {
			for (int j = lastTvdb + 1; j < tvdb.size(); j++) {
				if (TextComparison.looselyEquals(netflix.get(i).title, tvdb.get(j).frenchName)) {
					anchors.add(new int[] { i, j });
					lastTvdb = j;
					break;
				}
			}
		}
		anchorCount += anchors.size();

		// 2. Entre deux ancrages (et avant le premier, après le dernier), rapprochement dans l'ordre
		anchors.add(new int[] { netflix.size(), tvdb.size() }); // borne de fin
		int previousNetflix = -1;
		int previousTvdb = -1;
		for (int[] anchor : anchors) {
			int gapNetflix = anchor[0] - previousNetflix - 1;
			int gapTvdb = anchor[1] - previousTvdb - 1;
			for (int k = 0; k < Math.min(gapNetflix, gapTvdb); k++) {
				pairs.put(previousNetflix + 1 + k, previousTvdb + 1 + k);
			}
			if (anchor[0] < netflix.size()) {
				pairs.put(anchor[0], anchor[1]);
			}
			previousNetflix = anchor[0];
			previousTvdb = anchor[1];
		}

		// 3. Renumérotation de l'autre source
		if (reference == Reference.NETFLIX) {
			Map<Integer, Integer> tvdbToNetflix = new TreeMap<>();
			for (Map.Entry<Integer, Integer> pair : pairs.entrySet()) {
				tvdbToNetflix.put(pair.getValue(), pair.getKey());
			}
			int next = maxNetflixNumber(netflix) + 1;
			for (int j = 0; j < tvdb.size(); j++) {
				Integer netflixIndex = tvdbToNetflix.get(j);
				tvdbNumbers.put(tvdb.get(j), netflixIndex != null ? netflix.get(netflixIndex).number : next++);
			}
		} else {
			int next = maxTvdbNumber(tvdb) + 1;
			for (int i = 0; i < netflix.size(); i++) {
				Integer tvdbIndex = pairs.get(i);
				netflixNumbers.put(netflix.get(i),
						tvdbIndex != null ? tvdb.get(tvdbIndex).airedEpisodeNumber.intValue() : next++);
			}
		}
	}

	private static int maxNetflixNumber(List<NetflixEpisode> episodes) {
		int max = 0;
		for (NetflixEpisode episode : episodes) {
			max = Math.max(max, episode.number);
		}
		return max;
	}

	private static int maxTvdbNumber(List<TvdbBasicEpisode> episodes) {
		int max = 0;
		for (TvdbBasicEpisode episode : episodes) {
			max = Math.max(max, episode.airedEpisodeNumber.intValue());
		}
		return max;
	}
}
