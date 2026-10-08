package data;

import java.util.Locale;

/**
 * Comparaison d'un texte Netflix (titre ou résumé) avec sa traduction
 * française sur TVDB.
 */
public final class TextComparison {

	/** Résultat de la comparaison ; l'ordre de déclaration va du plus au moins urgent (ordre de tri) */
	public enum Result {
		EPISODE_MISSING("Épisode absent de TVDB"),
		MISSING("Absent en français"),
		DIFFERENT("Différent"),
		PUNCTUATION("Ponctuation"),
		IDENTICAL("Identique"),
		NETFLIX_MISSING("Absent de Netflix");

		private final String label;

		Result(String label) {
			this.label = label;
		}

		/** Ecart à reporter sur TVDB, hors ponctuation */
		public boolean isToFix() {
			return this == EPISODE_MISSING || this == MISSING || this == DIFFERENT;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private TextComparison() {
	}

	/**
	 * @param netflix texte Netflix, null si l'épisode est absent du CSV
	 * @param tvdbFrench traduction française sur TVDB, null si elle n'existe pas
	 * @param tvdbEpisodePresent faux si l'épisode est absent de TVDB
	 */
	public static Result compare(String netflix, String tvdbFrench, boolean tvdbEpisodePresent) {
		if (isBlank(netflix)) {
			return Result.NETFLIX_MISSING;
		}
		if (!tvdbEpisodePresent) {
			return Result.EPISODE_MISSING;
		}
		if (isBlank(tvdbFrench)) {
			return Result.MISSING;
		}
		if (clean(netflix).equals(clean(tvdbFrench))) {
			return Result.IDENTICAL;
		}
		if (loose(netflix).equals(loose(tvdbFrench))) {
			return Result.PUNCTUATION;
		}
		return Result.DIFFERENT;
	}

	/** Vrai si les deux textes ne diffèrent que par la ponctuation, les espaces ou la casse */
	public static boolean looselyEquals(String a, String b) {
		return !isBlank(a) && !isBlank(b) && loose(a).equals(loose(b));
	}

	/**
	 * Ecarts invisibles ou sans importance : apostrophes typographiques, espaces
	 * insécables, espaces multiples ou en début / fin de texte.
	 */
	private static String clean(String text) {
		return text.replaceAll("[‘’ʼ]", "'")
				.replaceAll("[\\s  ]+", " ")
				.trim();
	}

	// Lettres (accents compris) et chiffres seuls, en minuscules
	private static String loose(String text) {
		return text.toLowerCase(Locale.FRENCH).replaceAll("[^\\p{L}\\p{N}]", "");
	}

	private static boolean isBlank(String text) {
		return text == null || text.isBlank();
	}
}
