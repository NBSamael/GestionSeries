package data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import api.Episode;

/**
 * Numéros d'épisode utilisés pour mettre Netflix et la source face à face : ceux
 * d'origine, ou ceux d'une source de référence sur lesquels l'autre source est
 * renumérotée. La renumérotation ne sert qu'à la comparaison : ni le CSV ni
 * la source ne sont modifiés.
 */
public class EpisodeNumbering {

	/** Source dont les numéros sont conservés */
	public enum Reference {
		ORIGINAL, NETFLIX, SOURCE
	}

	private final Map<NetflixEpisode, Integer> netflixNumbers = new IdentityHashMap<>();
	private final Map<Episode, Integer> sourceNumbers = new IdentityHashMap<>();
	private int anchorCount;

	private EpisodeNumbering() {
	}

	/** Numéro de l'épisode Netflix pour la comparaison */
	public int number(NetflixEpisode episode) {
		return netflixNumbers.getOrDefault(episode, episode.number);
	}

	/** Numéro de l'épisode de la source pour la comparaison */
	public int number(Episode episode) {
		return sourceNumbers.getOrDefault(episode, episode.number.intValue());
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
	 * Les épisodes de la source sans numéro de saison ou d'épisode sont ignorés.
	 */
	public static EpisodeNumbering compute(List<NetflixEpisode> netflixEpisodes, List<Episode> sourceEpisodes,
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
		Map<Integer, List<Episode>> sourceBySeason = new TreeMap<>();
		for (Episode episode : sourceEpisodes) {
			if (episode.season != null && episode.number != null) {
				sourceBySeason.computeIfAbsent(episode.season.intValue(), s -> new ArrayList<>()).add(episode);
			}
		}

		for (Map.Entry<Integer, List<NetflixEpisode>> entry : netflixBySeason.entrySet()) {
			List<NetflixEpisode> netflix = entry.getValue();
			List<Episode> source = sourceBySeason.getOrDefault(entry.getKey(), new ArrayList<>());
			netflix.sort(Comparator.comparingInt(e -> e.number));
			source.sort(Comparator.comparingLong(e -> e.number));
			numbering.renumberSeason(netflix, source, reference);
		}
		return numbering;
	}

	private void renumberSeason(List<NetflixEpisode> netflix, List<Episode> source, Reference reference) {
		// Paires rapprochées : indice Netflix -> indice source
		Map<Integer, Integer> pairs = new TreeMap<>();

		// 1. Ancrages : pour chaque épisode Netflix, premier titre de la source correspondant après le dernier ancrage
		List<int[]> anchors = new ArrayList<>();
		int lastSource = -1;
		for (int i = 0; i < netflix.size(); i++) {
			for (int j = lastSource + 1; j < source.size(); j++) {
				if (TextComparison.looselyEquals(netflix.get(i).title, source.get(j).frenchName)) {
					anchors.add(new int[] { i, j });
					lastSource = j;
					break;
				}
			}
		}
		anchorCount += anchors.size();

		// 2. Entre deux ancrages (et avant le premier, après le dernier), rapprochement dans l'ordre
		anchors.add(new int[] { netflix.size(), source.size() }); // borne de fin
		int previousNetflix = -1;
		int previousSource = -1;
		for (int[] anchor : anchors) {
			int gapNetflix = anchor[0] - previousNetflix - 1;
			int gapSource = anchor[1] - previousSource - 1;
			for (int k = 0; k < Math.min(gapNetflix, gapSource); k++) {
				pairs.put(previousNetflix + 1 + k, previousSource + 1 + k);
			}
			if (anchor[0] < netflix.size()) {
				pairs.put(anchor[0], anchor[1]);
			}
			previousNetflix = anchor[0];
			previousSource = anchor[1];
		}

		// 3. Renumérotation de l'autre source
		if (reference == Reference.NETFLIX) {
			Map<Integer, Integer> sourceToNetflix = new TreeMap<>();
			for (Map.Entry<Integer, Integer> pair : pairs.entrySet()) {
				sourceToNetflix.put(pair.getValue(), pair.getKey());
			}
			int next = maxNetflixNumber(netflix) + 1;
			for (int j = 0; j < source.size(); j++) {
				Integer netflixIndex = sourceToNetflix.get(j);
				sourceNumbers.put(source.get(j), netflixIndex != null ? netflix.get(netflixIndex).number : next++);
			}
		} else {
			int next = maxSourceNumber(source) + 1;
			for (int i = 0; i < netflix.size(); i++) {
				Integer sourceIndex = pairs.get(i);
				netflixNumbers.put(netflix.get(i),
						sourceIndex != null ? source.get(sourceIndex).number.intValue() : next++);
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

	private static int maxSourceNumber(List<Episode> episodes) {
		int max = 0;
		for (Episode episode : episodes) {
			max = Math.max(max, episode.number.intValue());
		}
		return max;
	}
}
