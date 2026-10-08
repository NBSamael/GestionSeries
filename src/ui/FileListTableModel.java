package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Objects;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import data.FileItem;
import data.FileItemList;
import data.SeasonReading;

public class FileListTableModel extends AbstractTableModel {

	/** serialUID */
	private static final long serialVersionUID = 1L;

	// Les couleurs utilisées par le tableau sont définies dans la classe Theme

	/**
	 * Colonnes du tableau. L'ordre de déclaration détermine la position de la
	 * colonne : pour réordonner les colonnes, il suffit de réordonner les
	 * constantes.
	 */
	public enum Columns {
		SELECTED("Sel.", 35, Boolean.class, true),
		FOLDER("Dossier", 130),
		ORIGINAL_NAME("Nom fichier original", 220),
		SEASON("Saison", 70),
		EPISODE("Episode", 70),
		STATUS("Statut", 70),
		EPISODE_NAME("Nom épisode", 180),
		TREATED_NAME("Nom fichier traité", 280);

		private final String name;
		private final int width;
		private final Class<?> type;
		private final boolean editable;

		Columns(String name, int width) {
			this(name, width, String.class, false);
		}

		Columns(String name, int width, Class<?> type, boolean editable) {
			this.name = name;
			this.width = width;
			this.type = type;
			this.editable = editable;
		}

		public int getPosition() {
			return ordinal();
		}

		public static Columns atPosition(int position) {
			return values()[position];
		}

		@Override
		public String toString() {
			return name;
		}
	}

	FileItemList data;

	Application app;

	JTable table;

	private TableColumn folderColumn;
	private boolean folderColumnVisible = true;

	public FileListTableModel(FileItemList data, Application app) {
		super();
		this.data = data;
		this.app = app;
	}

	public void setTable(JTable table) {
		this.table = table;

		for (Columns c : Columns.values()) {
			TableColumn column = table.getColumnModel().getColumn(c.getPosition());
			column.setPreferredWidth(c.width);
			if (c == Columns.SELECTED) {
				column.setCellEditor(new DefaultCellEditor(new JCheckBox()));
			}
		}

		// Toutes les colonnes texte passent par ce rendu (String hérite d'Object)
		table.setDefaultRenderer(Object.class, new FileCellRenderer());
		table.setDefaultRenderer(Boolean.class, new StripedRenderer(table.getDefaultRenderer(Boolean.class)));

		folderColumn = table.getColumnModel().getColumn(Columns.FOLDER.getPosition());
		setFolderColumnVisible(app.chckbxRecursive.isSelected());
	}

	/**
	 * Affiche ou masque la colonne Dossier. Masquée, elle est retirée de la vue
	 * mais reste présente dans le modèle ; affichée, elle est remise à la position
	 * définie par l'enum Columns.
	 */
	public void setFolderColumnVisible(boolean visible) {
		if (visible == folderColumnVisible) {
			return;
		}
		if (visible) {
			table.addColumn(folderColumn);
			int lastIndex = table.getColumnCount() - 1;
			table.moveColumn(lastIndex, Math.min(Columns.FOLDER.getPosition(), lastIndex));
		} else {
			table.removeColumn(folderColumn);
		}
		folderColumnVisible = visible;
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return Columns.atPosition(col).type;
	}

	@Override
	public int getColumnCount() {
		return Columns.values().length;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public String getColumnName(int col) {
		return Columns.atPosition(col).toString();
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (row >= data.size()) {
			return null;
		}
		FileItem item = data.get(row);
		switch (Columns.atPosition(col)) {
		case SELECTED:
			return item.selected;
		case ORIGINAL_NAME:
			return getColorizedName(item);
		case SEASON:
			return item.season;
		case EPISODE:
			return item.episode;
		case STATUS:
			return item.status;
		case EPISODE_NAME:
			return item.episodeName;
		case TREATED_NAME:
			return item.treatedName;
		case FOLDER:
			return getColorizedFolder(item);
		default:
			return null;
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		// Note that the data/cell address is constant,
		// no matter where the cell appears onscreen.
		return Columns.atPosition(col).editable;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		FileItem item = data.get(row);
		switch (Columns.atPosition(col)) {
		case SELECTED:
			item.selected = (boolean) value;
			break;
		case ORIGINAL_NAME:
			item.originalName = (String) value;
			break;
		case SEASON:
			item.season = (String) value;
			break;
		case EPISODE:
			item.episode = (String) value;
			break;
		case STATUS:
			item.status = FileItem.Status.getStatusByName((String) value);
			break;
		case EPISODE_NAME:
			item.episodeName = (String) value;
			break;
		case TREATED_NAME:
			item.treatedName = (String) value;
			break;
		case FOLDER:
			item.folder = (String) value;
			break;
		}
		if (Columns.atPosition(col) == Columns.SELECTED) {
			// Toute la ligne doit être redessinée pour mettre à jour sa couleur
			fireTableRowsUpdated(row, row);
		} else {
			fireTableCellUpdated(row, col);
		}
	}

	public void addRow(FileItem item) {
		data.add(item);
		this.fireTableRowsInserted(data.size() - 1, data.size() - 1);
	}

	private SeasonReading getSeasonReading() {
		return (SeasonReading) app.comboSeasonReading.getSelectedItem();
	}

	private Range getSeasonRange() {
		return new Range(app.sliderSeasonNumPos.getValue(), (int) app.spinnerSeasonNumSize.getValue());
	}

	public String getColorizedName(FileItem item) {
		if (!app.colorization) {
			return item.originalName;
		}
		Range season = getSeasonReading() == SeasonReading.FILE_NAME ? getSeasonRange() : null;
		Range episode = new Range(app.sliderEpisodeNumPos.getValue(),
				(int) app.spinnerEpisodeNumSize.getValue());
		return colorize(item.originalName, season, episode);
	}

	public String getColorizedFolder(FileItem item) {
		if (!app.colorization || getSeasonReading() != SeasonReading.FOLDER_NAME) {
			return item.folder;
		}
		return colorize(item.folder, getSeasonRange(), null);
	}

	/**
	 * Colore le fond des caractères du texte appartenant aux sélections de saison
	 * et/ou d'épisode (null si la sélection ne s'applique pas à ce texte).
	 */
	private String colorize(String text, Range season, Range episode) {
		// Chaque caractère reçoit la couleur de la (ou des) sélection(s) à laquelle il appartient,
		// puis les caractères consécutifs de même couleur sont regroupés dans un seul span
		StringBuilder name = new StringBuilder("<html>");
		String currentColor = null;
		StringBuilder segment = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			boolean inSeason = season != null && season.contains(i);
			boolean inEpisode = episode != null && episode.contains(i);
			Color background = inSeason && inEpisode ? Theme.overlapBackground()
					: inSeason ? Theme.seasonBackground() : inEpisode ? Theme.episodeBackground() : null;
			String color = background != null ? Theme.toHtml(background) : null;

			if (i > 0 && !Objects.equals(color, currentColor)) {
				appendSegment(name, segment.toString(), currentColor);
				segment.setLength(0);
			}
			currentColor = color;
			segment.append(text.charAt(i));
		}
		appendSegment(name, segment.toString(), currentColor);
		name.append("</html>");
		return name.toString();
	}

	private void appendSegment(StringBuilder html, String text, String color) {
		if (text.isEmpty()) {
			return;
		}
		if (color == null) {
			html.append(toHtmlText(text));
		} else {
			html.append("<span style='background-color:").append(color).append("'>");
			html.append(toHtmlText(text));
			html.append("</span>");
		}
	}

	// Les espaces sont convertis uniquement dans le texte, pour ne pas casser les balises HTML
	private String toHtmlText(String text) {
		return text.replaceAll(" ", "&nbsp;");
	}

	// Echappe les caractères spéciaux HTML d'un texte (pour les info-bulles)
	private static String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	// Fichier affiché à la ligne indiquée (index de vue), ou null
	private FileItem getItemAtViewRow(JTable table, int row) {
		// La table est triable : l'index de ligne affiché doit être converti en index du modèle
		int modelRow = table.convertRowIndexToModel(row);
		return modelRow < data.size() ? data.get(modelRow) : null;
	}

	// Une ligne sur deux a un fond légèrement teinté (sauf si elle est sélectionnée)
	private static void applyStripe(Component component, JTable table, int row, boolean isSelected) {
		if (!isSelected) {
			component.setBackground(
					row % 2 == 0 ? table.getBackground() : Theme.stripeBackground(table.getBackground()));
		}
	}

	/**
	 * Rendu des cellules texte : alternance de couleur des lignes, texte gris pour
	 * les lignes décochées, statut "Erreur" en rouge, et info-bulle avec le chemin
	 * complet du fichier (et la raison de l'erreur le cas échéant).
	 */
	private class FileCellRenderer extends DefaultTableCellRenderer {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);
			applyStripe(component, table, row, isSelected);

			FileItem item = getItemAtViewRow(table, row);
			if (item == null) {
				setToolTipText(null);
				return component;
			}
			boolean error = item.status == FileItem.Status.ERREUR;
			boolean statusColumn = Columns.atPosition(table.convertColumnIndexToModel(column)) == Columns.STATUS;

			if (!item.selected) {
				component.setForeground(Theme.ignoredForeground());
			} else if (error && statusColumn) {
				component.setForeground(Theme.errorForeground());
				component.setFont(component.getFont().deriveFont(Font.BOLD));
			} else {
				component.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
			}

			StringBuilder tooltip = new StringBuilder("<html>");
			tooltip.append(escapeHtml(item.file.getAbsolutePath()));
			if (error && item.errorMessage != null) {
				tooltip.append("<br><b>Erreur :</b> ").append(escapeHtml(item.errorMessage));
			}
			tooltip.append("</html>");
			setToolTipText(tooltip.toString());
			return component;
		}
	}

	/**
	 * Rendu de la colonne de cases à cocher : reprend le rendu standard en y
	 * appliquant l'alternance de couleur des lignes.
	 */
	private static class StripedRenderer implements TableCellRenderer {

		private final TableCellRenderer delegate;

		StripedRenderer(TableCellRenderer delegate) {
			this.delegate = delegate;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			Component component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);
			applyStripe(component, table, row, isSelected);
			return component;
		}
	}

	private static class Range {
		private final int start;
		private final int length;

		Range(int start, int length) {
			this.start = start;
			this.length = length;
		}

		boolean contains(int position) {
			return position >= start && position < start + length;
		}
	}
}
