package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import api.Episode;
import data.TextComparison.Result;

/**
 * Un épisode mis face à face entre Netflix et la source, rapprochés par leurs
 * numéros de saison et d'épisode.
 */
public class EpisodeComparison {
	public final int season;
	public final int number;
	// null si l'épisode est absent du CSV Netflix
	public final NetflixEpisode netflix;
	// null si l'épisode est absent de la source (ou source pas encore chargée)
	public final Episode source;

	public final Result titleResult;
	public final Result overviewResult;

	/*
	 * Décalage de numérotation : numéro de l'épisode de la source dont le titre français
	 * correspond au titre Netflix de cette ligne, et inversement (null si aucun
	 * autre numéro ne correspond)
	 */
	public Integer netflixTitleMatchesSource;
	public Integer sourceTitleMatchesNetflix;

	public EpisodeComparison(int season, int number, NetflixEpisode netflix, Episode source) {
		this.season = season;
		this.number = number;
		this.netflix = netflix;
		this.source = source;
		this.titleResult = TextComparison.compare(netflix != null ? netflix.title : null,
				source != null ? source.frenchName : null, source != null);
		this.overviewResult = TextComparison.compare(netflix != null ? netflix.synopsis : null,
				source != null ? source.frenchOverview : null, source != null);
	}

	/** Ecart à reporter sur la source (hors ponctuation) dans le titre ou le résumé */
	public boolean isToFix() {
		return titleResult.isToFix() || overviewResult.isToFix();
	}

	/** Ecart de ponctuation seulement, dans le titre ou le résumé */
	public boolean hasPunctuationOnly() {
		return !isToFix()
				&& (titleResult == Result.PUNCTUATION || overviewResult == Result.PUNCTUATION);
	}

	/** Numéros d'origine des épisodes renumérotés, en clair (vide si aucun) */
	public String describeOriginalNumbers(String sourceName) {
		List<String> numbers = new ArrayList<>();
		if (netflix != null && netflix.number != number) {
			numbers.add("Netflix E" + netflix.number);
		}
		if (source != null && source.number != number) {
			numbers.add(sourceName + " E" + source.number);
		}
		return String.join(" ; ", numbers);
	}

	/** Vrai si le titre correspond à celui d'un autre numéro, d'un côté ou de l'autre */
	public boolean hasMatches() {
		return netflixTitleMatchesSource != null || sourceTitleMatchesNetflix != null;
	}

	/** Correspondances par titre avec un autre numéro, en clair (vide si aucune) */
	public String describeMatches(String sourceName) {
		List<String> matches = new ArrayList<>();
		if (netflixTitleMatchesSource != null) {
			matches.add("Titre Netflix = " + sourceName + " E" + netflixTitleMatchesSource);
		}
		if (sourceTitleMatchesNetflix != null) {
			matches.add("Titre " + sourceName + " = Netflix E" + sourceTitleMatchesNetflix);
		}
		return String.join(" ; ", matches);
	}

	/**
	 * Met face à face les épisodes Netflix et de la source de même numéro ; un épisode
	 * présent d'un seul côté donne une ligne à lui seul. Les lignes sont triées
	 * par saison puis par épisode.
	 */
	public static List<EpisodeComparison> match(List<NetflixEpisode> netflixEpisodes,
			List<Episode> sourceEpisodes, EpisodeNumbering numbering) {
		Map<Long, NetflixEpisode> netflixByKey = new TreeMap<>();
		for (NetflixEpisode episode : netflixEpisodes) {
			netflixByKey.put(key(episode.season, numbering.number(episode)), episode);
		}
		Map<Long, Episode> sourceByKey = new TreeMap<>();
		for (Episode episode : sourceEpisodes) {
			if (episode.season != null && episode.number != null) {
				sourceByKey.put(key(episode.season, numbering.number(episode)), episode);
			}
		}

		TreeMap<Long, EpisodeComparison> rows = new TreeMap<>();
		for (Map.Entry<Long, NetflixEpisode> entry : netflixByKey.entrySet()) {
			NetflixEpisode episode = entry.getValue();
			rows.put(entry.getKey(), new EpisodeComparison(episode.season, numbering.number(episode), episode,
					sourceByKey.get(entry.getKey())));
		}
		for (Map.Entry<Long, Episode> entry : sourceByKey.entrySet()) {
			if (!rows.containsKey(entry.getKey())) {
				Episode episode = entry.getValue();
				rows.put(entry.getKey(), new EpisodeComparison(episode.season.intValue(),
						numbering.number(episode), null, episode));
			}
		}

		List<EpisodeComparison> result = new ArrayList<>(rows.values());
		findTitleMatches(result);
		return result;
	}

	/*
	 * Quand les titres d'une ligne ne correspondent pas, cherche dans la même
	 * saison l'épisode de l'autre source dont le titre correspond (ponctuation
	 * ignorée) : signale un décalage de numérotation plutôt qu'un titre à
	 * corriger.
	 */
	private static void findTitleMatches(List<EpisodeComparison> rows) {
		for (EpisodeComparison row : rows) {
			if (row.titleResult == Result.IDENTICAL || row.titleResult == Result.PUNCTUATION) {
				continue;
			}
			for (EpisodeComparison other : rows) {
				if (other == row || other.season != row.season) {
					continue;
				}
				if (row.netflix != null && other.source != null && row.netflixTitleMatchesSource == null
						&& TextComparison.looselyEquals(row.netflix.title, other.source.frenchName)) {
					row.netflixTitleMatchesSource = other.number;
				}
				if (row.source != null && other.netflix != null && row.sourceTitleMatchesNetflix == null
						&& TextComparison.looselyEquals(row.source.frenchName, other.netflix.title)) {
					row.sourceTitleMatchesNetflix = other.number;
				}
			}
		}
	}

	// Clé de tri et de rapprochement : saison puis épisode
	private static long key(long season, long number) {
		return season * 100_000 + number;
	}
}
