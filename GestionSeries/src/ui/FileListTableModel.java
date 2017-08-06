package ui;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import data.FileItem;
import data.FileItemList;

public class FileListTableModel extends AbstractTableModel {

	String[] columnNames = { "Nom fichier original", "Saison", "Episode", "Nom épisode", "Nom fichier traité" };

	FileItemList data;

	JTable table;

	public FileListTableModel(FileItemList data) {
		super();
		this.data = data;
	}

	public void setTable(JTable table) {
		this.table = table;

		TableColumn column = null;
		for (int i = 0; i < 4; i++) {
			column = table.getColumnModel().getColumn(i);
			if ((i == 0) || (i == 3)) {
				column.setPreferredWidth(100);
			} else {
				column.setPreferredWidth(10);
			}
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
		FileItem item = data.get(row);
		if (col == 0) {
			return item.originalName;
		}
		if (col == 1) {
			return item.season;
		}
		if (col == 2) {
			return item.episode;
		}
		if (col == 3) {
			return item.episodeName;
		}
		if (col == 4) {
			return item.treatedName;
		}
		return null;
	}

	public void setValueAt(Object value, int row, int col) {
		FileItem item = data.get(row);
		if (row == 0) {
			item.originalName = (String) value;
		}
		if (row == 1) {
			item.season = (String) value;
		}
		if (row == 2) {
			item.episode = (String) value;
		}
		if (row == 3) {
			item.episodeName = (String) value;
		}
		if (row == 4) {
			item.treatedName = (String) value;
		}
		fireTableCellUpdated(row, col);
	}

	public void addRow(FileItem item) {
		data.add(item);
		this.fireTableRowsInserted(data.size() - 1, data.size() - 1);
	}
}
