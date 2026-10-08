package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import api.TvdbBasicEpisode;
import data.TextComparison.Result;

/**
 * Un épisode mis face à face entre Netflix et TVDB, rapprochés par leurs
 * numéros de saison et d'épisode.
 */
public class EpisodeComparison {
	public final int season;
	public final int number;
	// null si l'épisode est absent du CSV Netflix
	public final NetflixEpisode netflix;
	// null si l'épisode est absent de TVDB (ou TVDB pas encore chargé)
	public final TvdbBasicEpisode tvdb;

	public final Result titleResult;
	public final Result overviewResult;

	/*
	 * Décalage de numérotation : numéro de l'épisode TVDB dont le titre français
	 * correspond au titre Netflix de cette ligne, et inversement (null si aucun
	 * autre numéro ne correspond)
	 */
	public Integer netflixTitleMatchesTvdb;
	public Integer tvdbTitleMatchesNetflix;

	public EpisodeComparison(int season, int number, NetflixEpisode netflix, TvdbBasicEpisode tvdb) {
		this.season = season;
		this.number = number;
		this.netflix = netflix;
		this.tvdb = tvdb;
		this.titleResult = TextComparison.compare(netflix != null ? netflix.title : null,
				tvdb != null ? tvdb.frenchName : null, tvdb != null);
		this.overviewResult = TextComparison.compare(netflix != null ? netflix.synopsis : null,
				tvdb != null ? tvdb.frenchOverview : null, tvdb != null);
	}

	/** Ecart à reporter sur TVDB (hors ponctuation) dans le titre ou le résumé */
	public boolean isToFix() {
		return titleResult.isToFix() || overviewResult.isToFix();
	}

	/** Ecart de ponctuation seulement, dans le titre ou le résumé */
	public boolean hasPunctuationOnly() {
		return !isToFix()
				&& (titleResult == Result.PUNCTUATION || overviewResult == Result.PUNCTUATION);
	}

	/** Numéros d'origine des épisodes renumérotés, en clair (vide si aucun) */
	public String describeOriginalNumbers() {
		List<String> numbers = new ArrayList<>();
		if (netflix != null && netflix.number != number) {
			numbers.add("Netflix E" + netflix.number);
		}
		if (tvdb != null && tvdb.airedEpisodeNumber != number) {
			numbers.add("TVDB E" + tvdb.airedEpisodeNumber);
		}
		return String.join(" ; ", numbers);
	}

	/** Correspondances par titre avec un autre numéro, en clair (vide si aucune) */
	public String describeMatches() {
		List<String> matches = new ArrayList<>();
		if (netflixTitleMatchesTvdb != null) {
			matches.add("Titre Netflix = TVDB E" + netflixTitleMatchesTvdb);
		}
		if (tvdbTitleMatchesNetflix != null) {
			matches.add("Titre TVDB = Netflix E" + tvdbTitleMatchesNetflix);
		}
		return String.join(" ; ", matches);
	}

	/**
	 * Met face à face les épisodes Netflix et TVDB de même numéro ; un épisode
	 * présent d'un seul côté donne une ligne à lui seul. Les lignes sont triées
	 * par saison puis par épisode.
	 */
	public static List<EpisodeComparison> match(List<NetflixEpisode> netflixEpisodes,
			List<TvdbBasicEpisode> tvdbEpisodes, EpisodeNumbering numbering) {
		Map<Long, NetflixEpisode> netflixByKey = new TreeMap<>();
		for (NetflixEpisode episode : netflixEpisodes) {
			netflixByKey.put(key(episode.season, numbering.number(episode)), episode);
		}
		Map<Long, TvdbBasicEpisode> tvdbByKey = new TreeMap<>();
		for (TvdbBasicEpisode episode : tvdbEpisodes) {
			if (episode.airedSeason != null && episode.airedEpisodeNumber != null) {
				tvdbByKey.put(key(episode.airedSeason, numbering.number(episode)), episode);
			}
		}

		TreeMap<Long, EpisodeComparison> rows = new TreeMap<>();
		for (Map.Entry<Long, NetflixEpisode> entry : netflixByKey.entrySet()) {
			NetflixEpisode episode = entry.getValue();
			rows.put(entry.getKey(), new EpisodeComparison(episode.season, numbering.number(episode), episode,
					tvdbByKey.get(entry.getKey())));
		}
		for (Map.Entry<Long, TvdbBasicEpisode> entry : tvdbByKey.entrySet()) {
			if (!rows.containsKey(entry.getKey())) {
				TvdbBasicEpisode episode = entry.getValue();
				rows.put(entry.getKey(), new EpisodeComparison(episode.airedSeason.intValue(),
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
				if (row.netflix != null && other.tvdb != null && row.netflixTitleMatchesTvdb == null
						&& TextComparison.looselyEquals(row.netflix.title, other.tvdb.frenchName)) {
					row.netflixTitleMatchesTvdb = other.number;
				}
				if (row.tvdb != null && other.netflix != null && row.tvdbTitleMatchesNetflix == null
						&& TextComparison.looselyEquals(row.tvdb.frenchName, other.netflix.title)) {
					row.tvdbTitleMatchesNetflix = other.number;
				}
			}
		}
	}

	// Clé de tri et de rapprochement : saison puis épisode
	private static long key(long season, long number) {
		return season * 100_000 + number;
	}
}
