package api;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Données de connexion à TVDB, lues dans un fichier de configuration
 * (gestionseries.properties) qui n'est pas versionné.
 *
 * Le fichier est cherché, dans l'ordre :
 * <ol>
 * <li>au chemin donné par la propriété système "gestionseries.config"
 * (-Dgestionseries.config=...) ;</li>
 * <li>dans le dossier courant (la racine du projet quand l'application est
 * lancée depuis VS Code) ;</li>
 * <li>dans le dossier personnel de l'utilisateur.</li>
 * </ol>
 */
public final class TvdbConfig {

	public static final String FILE_NAME = "gestionseries.properties";
	private static final String PATH_PROPERTY = "gestionseries.config";

	private static final String KEY_API_KEY = "tvdb.apikey";
	private static final String KEY_PIN = "tvdb.pin";

	/** Configuration absente ou incomplète ; le message explique comment la corriger */
	public static class ConfigException extends Exception {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		public ConfigException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private final String apiKey;
	private final String pin;

	private TvdbConfig(String apiKey, String pin) {
		this.apiKey = apiKey;
		this.pin = pin;
	}

	public String getApiKey() {
		return apiKey;
	}

	/** PIN d'abonné, uniquement nécessaire pour les clés "user-supported" ; null si absent */
	public String getPin() {
		return pin;
	}

	/** Lit la configuration ; à appeler à chaque utilisation pour prendre en compte une correction du fichier */
	public static TvdbConfig load() throws ConfigException {
		List<Path> candidates = getCandidatePaths();
		for (Path path : candidates) {
			if (Files.isRegularFile(path)) {
				return load(path);
			}
		}
		StringBuilder message = new StringBuilder("Fichier de configuration " + FILE_NAME + " introuvable.\n\n");
		message.append("Créez-le à partir du modèle gestionseries.example.properties, à l'un de ces emplacements :\n");
		for (Path path : candidates) {
			message.append("  - ").append(path.toAbsolutePath()).append("\n");
		}
		throw new ConfigException(message.toString(), null);
	}

	private static TvdbConfig load(Path path) throws ConfigException {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException e) {
			throw new ConfigException("Impossible de lire le fichier de configuration " + path.toAbsolutePath()
					+ " : " + e.getMessage(), e);
		}
		String apiKey = trimToNull(properties.getProperty(KEY_API_KEY));
		if (apiKey == null) {
			throw new ConfigException("La clé API TVDB (" + KEY_API_KEY + ") n'est pas renseignée dans "
					+ path.toAbsolutePath(), null);
		}
		return new TvdbConfig(apiKey, trimToNull(properties.getProperty(KEY_PIN)));
	}

	private static List<Path> getCandidatePaths() {
		List<Path> paths = new ArrayList<>();
		String explicitPath = System.getProperty(PATH_PROPERTY);
		if (explicitPath != null) {
			paths.add(Paths.get(explicitPath));
		}
		paths.add(Paths.get(FILE_NAME));
		paths.add(Paths.get(System.getProperty("user.home"), FILE_NAME));
		return paths;
	}

	private static String trimToNull(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
	}
}
