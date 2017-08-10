package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ui.Application;
import ui.FileListTableModel;

public class FileItemList extends ArrayList<FileItem> {

	FileListTableModel tableModel;

	public FileItemList(Application app) {
		super();
		tableModel = new FileListTableModel(this, app);
	}

	public FileListTableModel getTableModel() {
		return tableModel;
	}

	@Override
	public boolean add(FileItem e) {
		boolean valueReturned = super.add(e);
		if (valueReturned)
			tableModel.fireTableRowsInserted(size() - 1, size() - 1);
		return valueReturned;
	}

	public void setEpisodeName(String originalName, String episodeName) {
		for (int row = 0; row < size(); row++) {
			if (get(row).originalName.equals(originalName)) {
				get(row).episodeName = episodeName;
				tableModel.fireTableCellUpdated(row, 5);
				return;
			}
		}
	}

	public void completeData(ShowInformations showInfos) {
		for (FileItem fileItem : this) {
			String originalName = fileItem.originalName;

			String season;
			if (showInfos.SeasonNumber == -1) {
				season = originalName.substring(showInfos.SeasonNumPos,
						showInfos.SeasonNumPos + showInfos.SeasonNumSize);
			} else {
				season = new Integer(showInfos.SeasonNumber).toString();
				System.out.println(showInfos.SeasonNumber);
			}
			if (season.length() < 2) {
				season = "0" + season;
			}
			String episode = originalName.substring(showInfos.EpisodeNumPos,
					showInfos.EpisodeNumPos + showInfos.EpisodeNumSize);
			if (episode.length() < 2) {
				episode = "0" + episode;
			}

			fileItem.season = season;
			fileItem.episode = episode;
		}
		tableModel.fireTableDataChanged();
	}

	public void findNewName(ShowInformations showInfos) {
		for (FileItem fileItem : this) {

			String originalName = fileItem.originalName;

			List<String> namePieces = Arrays.asList(originalName.split("\\."));
			String extension = namePieces.get(namePieces.size() - 1);

			StringBuilder treatedName = new StringBuilder();
			treatedName.append(showInfos.ShowName).append(" - S");
			treatedName.append(fileItem.season).append("E");
			treatedName.append(fileItem.episode);
			treatedName.append(" - ");
			treatedName.append(fileItem.episodeName);
			treatedName.append(".").append(extension);
			fileItem.treatedName = treatedName.toString();
		}
		tableModel.fireTableDataChanged();
	}

	@Override
	public void clear() {
		super.clear();
		tableModel.fireTableDataChanged();
	}

	public void refresh() {
		tableModel.fireTableDataChanged();
	}
}
