package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import api.TvdbSerie;

/**
 * Fenêtre de choix de la série parmi les résultats d'une recherche TVDB.
 */
public class SeriesSearchDialog extends JDialog {

	/** serialUID */
	private static final long serialVersionUID = 1L;

	private final SeriesTableModel tableModel = new SeriesTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextArea overview = new JTextArea();
	private final JButton btnSelect = new JButton("Sélectionner");

	public SeriesSearchDialog(JFrame owner) {
		super(owner, "Rechercher Séries", true);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		JPanel contentPane = new JPanel(new BorderLayout(0, 5));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		// Liste des séries trouvées
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		for (Column column : Column.values()) {
			table.getColumnModel().getColumn(column.ordinal()).setPreferredWidth(column.width);
		}
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				updateSelection();
			}
		});
		// Un double-clic sur une ligne équivaut à un clic sur "Sélectionner"
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0 && btnSelect.isEnabled()) {
					btnSelect.doClick();
				}
			}
		});

		// Résumé de la série sélectionnée
		overview.setEditable(false);
		overview.setLineWrap(true);
		overview.setWrapStyleWord(true);
		overview.setRows(5);
		JScrollPane overviewScrollPane = new JScrollPane(overview);
		overviewScrollPane.setBorder(new TitledBorder("Résumé"));

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table),
				overviewScrollPane);
		splitPane.setResizeWeight(0.75);
		contentPane.add(splitPane, BorderLayout.CENTER);

		// Boutons
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		JButton btnCancel = new JButton("Annuler");
		btnCancel.addActionListener(e -> setVisible(false));
		buttonsPanel.add(btnSelect);
		buttonsPanel.add(btnCancel);
		contentPane.add(buttonsPanel, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(btnSelect);

		setSize(900, 500);
	}

	/** Bouton de validation du choix ; son action est gérée par l'appelant */
	public JButton getSelectButton() {
		return btnSelect;
	}

	/**
	 * Affiche les séries trouvées (triées par nom puis par date de première
	 * diffusion) et ouvre la fenêtre, centrée sur la fenêtre principale. L'appel
	 * est bloquant jusqu'à la fermeture de la fenêtre.
	 */
	public void showSeries(List<TvdbSerie> series) {
		List<TvdbSerie> sorted = new ArrayList<>(series);
		sorted.sort(Comparator.comparing((TvdbSerie s) -> s.seriesName, Comparator.nullsLast(String::compareTo))
				.thenComparing(s -> s.firstAired, Comparator.nullsLast(String::compareTo)));
		tableModel.setSeries(sorted);
		table.clearSelection();
		updateSelection();
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}

	/** Série sélectionnée, ou null si aucune */
	public TvdbSerie getSelectedSerie() {
		int viewRow = table.getSelectedRow();
		if (viewRow < 0) {
			return null;
		}
		return tableModel.getSerie(table.convertRowIndexToModel(viewRow));
	}

	private void updateSelection() {
		TvdbSerie serie = getSelectedSerie();
		btnSelect.setEnabled(serie != null);
		overview.setText(serie != null && serie.overview != null ? serie.overview : "");
		overview.setCaretPosition(0);
	}

	/** Colonnes de la liste des séries ; l'ordre de déclaration détermine leur position */
	private enum Column {
		NAME("Nom", 400, s -> s.seriesName),
		FIRST_AIRED("Première diffusion", 110, s -> s.firstAired),
		NETWORK("Réseau", 150, s -> s.network),
		STATUS("Statut", 100, s -> s.status);

		private final String name;
		private final int width;
		private final Function<TvdbSerie, String> value;

		Column(String name, int width, Function<TvdbSerie, String> value) {
			this.name = name;
			this.width = width;
			this.value = value;
		}
	}

	private static class SeriesTableModel extends AbstractTableModel {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		private List<TvdbSerie> series = new ArrayList<>();

		void setSeries(List<TvdbSerie> series) {
			this.series = series;
			fireTableDataChanged();
		}

		TvdbSerie getSerie(int row) {
			return series.get(row);
		}

		@Override
		public int getRowCount() {
			return series.size();
		}

		@Override
		public int getColumnCount() {
			return Column.values().length;
		}

		@Override
		public String getColumnName(int col) {
			return Column.values()[col].name;
		}

		@Override
		public Object getValueAt(int row, int col) {
			return Column.values()[col].value.apply(series.get(row));
		}
	}
}
