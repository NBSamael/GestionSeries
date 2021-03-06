package ui;

import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import data.FileItem;
import data.FileItemList;

public class FileListTableModel extends AbstractTableModel {

	public enum Columns {
		ORIGINAL_NAME("Nom fichier original", 0), SEASON("Saison", 1), EPISODE("Episode",
				2), EPISODE_NAME("Nom �pisode", 3), TREATED_NAME("Nom fichier trait�", 4);

		private String name = "";
		private int position;

		Columns(String name, int position) {
			this.name = name;
			this.position = position;
		}

		public int getPosition() {
			return position;
		}

		public String toString() {
			return name;
		}
	}

	String[] columnNames = { "Sel.", "Nom fichier original", "Saison", "Episode", "Statut", "Nom �pisode",
			"Nom fichier trait�" };

	FileItemList data;

	Application app;

	JTable table;

	public FileListTableModel(FileItemList data, Application app) {
		super();
		this.data = data;
		this.app = app;
	}

	public void setTable(JTable table) {
		this.table = table;

		TableColumn column = null;
		for (int col = 0; col < getColumnCount(); col++) {
			column = table.getColumnModel().getColumn(col);
			if (col == 0) {
				column.setPreferredWidth(35);
				column.setCellEditor(new DefaultCellEditor(new JCheckBox()));
			}
			if (col == 1) {
				column.setPreferredWidth(240);
			}
			if (col == 2) {
				column.setPreferredWidth(70);
			}
			if (col == 3) {
				column.setPreferredWidth(70);
			}
			if (col == 4) {
				column.setPreferredWidth(70);
			}
			if (col == 5) {
				column.setPreferredWidth(210);
			}
			if (col == 6) {
				column.setPreferredWidth(350);
			}
		}
	}

	public Class getColumnClass(int c) {
		if (c == 0) {
			return Boolean.class;
		} else {
			return String.class;
		}
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (row < data.size()) {
			FileItem item = data.get(row);
			if (col == 0) {
				return item.selected;
			}
			if (col == 1) {
				return getColorizedName(item);
			}
			if (col == 2) {
				return item.season;
			}
			if (col == 3) {
				return item.episode;
			}
			if (col == 4) {
				return item.status;
			}
			if (col == 5) {
				return item.episodeName;
			}
			if (col == 6) {
				return item.treatedName;
			}
		}
		return null;
	}

	public boolean isCellEditable(int row, int col) {
		// Note that the data/cell address is constant,
		// no matter where the cell appears onscreen.
		if (col == 0) {
			return true;
		} else {
			return false;
		}
	}

	public void setValueAt(Object value, int row, int col) {
		FileItem item = data.get(row);
		if (col == 0) {
			item.selected = (boolean) value;
		}
		if (col == 1) {
			item.originalName = (String) value;
		}
		if (col == 2) {
			item.season = (String) value;
		}
		if (col == 3) {
			item.episode = (String) value;
		}
		if (col == 4) {
			item.status = FileItem.Status.getStatusByName((String) value);
		}
		if (col == 5) {
			item.episodeName = (String) value;
		}
		if (col == 6) {
			item.treatedName = (String) value;
		}
		fireTableCellUpdated(row, col);
	}

	public void addRow(FileItem item) {
		data.add(item);
		this.fireTableRowsInserted(data.size() - 1, data.size() - 1);
	}

	public String getColorizedName(FileItem item) {
		if (!app.colorization) {
			return item.originalName;
		}
		StringBuilder name = new StringBuilder("<html>");
		Set<Colorization> colorizations = new TreeSet<Colorization>();

		if (app.chckbxSeasonReading.isSelected()) {
			Colorization cSeason = new Colorization();
			cSeason.start = (int) app.spinnerSeasonNumPos.getValue();
			cSeason.lentgh = (int) app.spinnerSeasonNumSize.getValue();
			cSeason.color = "green";
			colorizations.add(cSeason);
		}

		Colorization cEpisode = new Colorization();
		cEpisode.start = (int) app.spinnerEpisodeNumPos.getValue();
		cEpisode.lentgh = (int) app.spinnerEpisodeNumSize.getValue();
		cEpisode.color = "red";
		colorizations.add(cEpisode);

		int lastPosition = 0;
		for (Colorization colorization : colorizations) {
			name.append(item.originalName.substring(lastPosition, colorization.start));
			name.append("<font color='" + colorization.color + "'>");
			name.append(item.originalName.substring(colorization.start, colorization.start + colorization.lentgh));
			name.append("</font>");
			lastPosition = colorization.start + colorization.lentgh;
		}
		name.append(item.originalName.substring(lastPosition));
		name.append("</html>");
		String name2 = name.toString().replaceAll(" ", "&nbsp;");
		return name2;
	}

	class Colorization implements Comparable<Colorization> {
		public int start;
		public int lentgh;
		public String color;

		@Override
		public int compareTo(Colorization o) {
			return this.start - o.start;
		}
	}
}
