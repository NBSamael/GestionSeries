package data;

public class ShowInformations {
	public String ShowName;
	public SeasonReading seasonReading = SeasonReading.FILE_NAME;
	public int SeasonNumber = -1;
	public int SeasonNumPos;
	public int SeasonNumSize;
	public int EpisodeNumPos;
	public int EpisodeNumSize;
	public int offset;
	public int finalLength;

	public ShowInformations() {
		ShowName = null;
		seasonReading = SeasonReading.FILE_NAME;
		SeasonNumber = -1;
		SeasonNumPos = -1;
		SeasonNumSize = -1;
		EpisodeNumPos = -1;
		EpisodeNumSize = -1;
		offset = 0;
		finalLength = -1;
	}

}
