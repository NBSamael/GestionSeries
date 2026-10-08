package data;

/**
 * Episode lu dans un CSV exporté de Netflix (extension NetflixEpisodesExport).
 */
public class NetflixEpisode {
	public int season;
	public int number;
	public String title;
	public String synopsis;

	public NetflixEpisode(int season, int number, String title, String synopsis) {
		this.season = season;
		this.number = number;
		this.title = title;
		this.synopsis = synopsis;
	}
}
