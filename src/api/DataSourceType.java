package api;

import api.tmdb.TmdbSource;
import api.tvdb.TvdbSource;

/**
 * Sources de données proposées à l'utilisateur.
 */
public enum DataSourceType {
	TVDB("TVDB") {
		@Override
		public DataSource create(ApiConfig config) throws ApiConfig.ConfigException {
			return new TvdbSource(config.requireTvdbApiKey(), config.getTvdbPin());
		}
	},
	TMDB("TMDB") {
		@Override
		public DataSource create(ApiConfig config) throws ApiConfig.ConfigException {
			return new TmdbSource(config.requireTmdbApiKey());
		}
	};

	private final String label;

	DataSourceType(String label) {
		this.label = label;
	}

	/** Nom de la source, tel qu'affiché à l'utilisateur */
	public String getLabel() {
		return label;
	}

	/**
	 * Crée la source à partir de la configuration.
	 *
	 * @throws ApiConfig.ConfigException si la configuration de cette source est incomplète
	 */
	public abstract DataSource create(ApiConfig config) throws ApiConfig.ConfigException;

	@Override
	public String toString() {
		return label;
	}
}
