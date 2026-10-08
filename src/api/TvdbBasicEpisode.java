package api;

public class TvdbBasicEpisode {
	public Long absoluteNumber;
	public Long airedEpisodeNumber;
	public Long airedSeason;
	public Long dvdEpisodeNumber;
	public Long dvdSeason;
	// Titre et résumé en français, repliés sur la langue d'origine quand la traduction n'existe pas
	public String episodeName;
	public String firstAired;
	public Long id;
	public String lastUpdated;
	public String overview;
	// Titre et résumé en français seuls : null quand la traduction n'existe pas
	public String frenchName;
	public String frenchOverview;
}
