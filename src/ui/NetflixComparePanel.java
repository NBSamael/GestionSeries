package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import api.TvdbBasicEpisode;
import api.TvdbConfig;
import api.TvdbEndpoint;
import api.TvdbSerie;
import api.TvdbSite;
import data.EpisodeComparison;
import data.EpisodeNumbering;
import data.EpisodeNumbering.Reference;
import data.NetflixCsvReader;
import data.NetflixEpisode;
import data.TextComparison.Result;

/**
 * Onglet de comparaison entre un CSV exporté de Netflix et les épisodes TVDB,
 * pour préparer la saisie des traductions françaises sur thetvdb.com.
 */
public class NetflixComparePanel extends JPanel {

	/** serialUID */
	private static final long serialVersionUID = 1L;

	/*
	 * Nom des fichiers exportés : « <Série> - Saison N - episodes.csv », suivi
	 * d'un éventuel « (1) » ajouté par le navigateur quand le fichier existe déjà
	 */
	private static final Pattern CSV_FILE_NAME = Pattern.compile(
			"^(.*?)(?: - Saison \\d+)? - episodes(?:\\s*\\(\\d+\\))?\\.csv$", Pattern.CASE_INSENSITIVE);

	private final ComparisonTableModel tableModel = new ComparisonTableModel();
	private final JTable table = new JTable(tableModel);
	private final TableRowSorter<ComparisonTableModel> sorter = new TableRowSorter<>(tableModel);
	private final JComboBox<Filter> comboFilter = new JComboBox<>(Filter.values());
	private final JTextArea netflixSynopsis = createTextArea();
	private final JTextArea tvdbOverview = createTextArea();
	private final JLabel lblFile = new JLabel("Aucun fichier chargé");
	private final JTextField textShowName = new JTextField(20);
	private final JButton btnSearch = new JButton("Rechercher Série");
	private final JLabel statusBar = new JLabel();

	/* Dossier proposé à l'ouverture du CSV : le dernier utilisé, sinon les téléchargements */
	private File lastDirectory = new File(System.getProperty("user.home"), "Downloads");

	private List<NetflixEpisode> netflixEpisodes = new ArrayList<>();
	// Nom de la série déduit du nom du dernier CSV chargé
	private String csvShowName;

	private TvdbEndpoint tvdb;
	private SeriesSearchDialog searchDialog;
	// Série TVDB choisie (null si aucune) et ses épisodes des saisons du CSV
	private TvdbSerie serie;
	private List<TvdbBasicEpisode> tvdbEpisodes = new ArrayList<>();
	// Source dont les numéros sont conservés (ORIGINAL : aucune renumérotation)
	private Reference numberingReference = Reference.ORIGINAL;
	private final JButton btnOpenTvdb = new JButton("Traduire sur TVDB");
	private final JButton btnCopyTitle = new JButton("Copier le titre");
	private final JButton btnCopySynopsis = new JButton("Copier le résumé");
	private final JLabel lblCopied = new JLabel(" ");
	private final JButton btnRenumber = new JButton("Renuméroter…");
	private final JButton btnOriginalNumbering = new JButton("Numérotation d'origine");

	public NetflixComparePanel() {
		super(new BorderLayout(0, 5));
		setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel settingsPanel = new JPanel(new GridBagLayout());
		add(settingsPanel, BorderLayout.NORTH);

		// 1. CSV Netflix
		JPanel csvPanel = createGroupPanel("1. CSV Netflix");
		addGroup(settingsPanel, csvPanel, 0, 0.5);
		JButton btnOpenCsv = new JButton("Ouvrir CSV…");
		btnOpenCsv.setToolTipText("CSV exporté de Netflix par l'extension NetflixEpisodesExport");
		btnOpenCsv.addActionListener(e -> chooseCsv());
		csvPanel.add(btnOpenCsv, gbc(0, 0));
		GridBagConstraints fileConstraints = gbc(1, 0);
		fileConstraints.weightx = 1.0;
		csvPanel.add(lblFile, fileConstraints);

		// 2. Série TVDB
		JPanel showPanel = createGroupPanel("2. Série TVDB");
		addGroup(settingsPanel, showPanel, 1, 0.5);
		showPanel.add(new JLabel("Nom Série"), gbc(0, 0));
		GridBagConstraints textConstraints = gbc(1, 0);
		textConstraints.fill = GridBagConstraints.HORIZONTAL;
		textConstraints.weightx = 1.0;
		showPanel.add(textShowName, textConstraints);
		btnSearch.setEnabled(false);
		btnSearch.addActionListener(e -> searchSerie());
		showPanel.add(btnSearch, gbc(2, 0));
		// Entrée dans le champ de saisie : lance la recherche, si le bouton est actif
		textShowName.addActionListener(e -> {
			if (btnSearch.isEnabled()) {
				btnSearch.doClick();
			}
		});
		textShowName.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateBtnSearch();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateBtnSearch();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateBtnSearch();
			}
		});

		// 3. Ecarts
		JPanel filterPanel = createGroupPanel("3. Écarts");
		addGroup(settingsPanel, filterPanel, 2, 0.0);
		filterPanel.add(new JLabel("Afficher"), gbc(0, 0));
		comboFilter.addActionListener(e -> {
			sorter.setRowFilter(((Filter) comboFilter.getSelectedItem()).rowFilter);
			updateSelection();
		});
		filterPanel.add(comboFilter, gbc(1, 0));

		btnRenumber.setEnabled(false);
		btnRenumber.setToolTipText("<html>Recale la numérotation d'une source sur l'autre, d'après les titres qui correspondent<br>"
				+ "(affichage seulement : ni le CSV ni TVDB ne sont modifiés)</html>");
		btnRenumber.addActionListener(e -> renumber());
		filterPanel.add(btnRenumber, actionConstraints(0, 1));
		btnOriginalNumbering.setEnabled(false);
		btnOriginalNumbering.setToolTipText("Revient aux numéros d'épisode d'origine des deux sources");
		btnOriginalNumbering.addActionListener(e -> {
			numberingReference = Reference.ORIGINAL;
			refreshComparison();
		});
		filterPanel.add(btnOriginalNumbering, actionConstraints(0, 2));

		// Episodes face à face, et résumés complets de l'épisode sélectionné
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSorter(sorter);
		table.setDefaultRenderer(Result.class, new ResultRenderer());
		for (Column column : Column.values()) {
			table.getColumnModel().getColumn(column.ordinal()).setPreferredWidth(column.width);
		}
		// Les décalages de numérotation sont expliqués au survol
		table.getColumnModel().getColumn(Column.MATCH.ordinal()).setCellRenderer(new MatchRenderer());
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				updateSelection();
			}
		});

		JSplitPane synopsisPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				createTitledScrollPane(netflixSynopsis, "Résumé Netflix"),
				createTitledScrollPane(tvdbOverview, "Résumé TVDB (FR)"));
		synopsisPane.setResizeWeight(0.5);

		// Actions sur l'épisode sélectionné, au-dessus de ses résumés
		JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		actionsPanel.setBorder(new EmptyBorder(0, 0, 4, 0));
		actionsPanel.add(new JLabel("Épisode sélectionné :"));
		btnOpenTvdb.addActionListener(e -> openOnTvdb());
		actionsPanel.add(btnOpenTvdb);
		btnCopyTitle.setToolTipText("Copie le titre Netflix dans le presse-papiers");
		btnCopyTitle.addActionListener(e -> copy(selectedRow().netflix.title, "Titre"));
		actionsPanel.add(btnCopyTitle);
		btnCopySynopsis.setToolTipText("Copie le résumé Netflix dans le presse-papiers");
		btnCopySynopsis.addActionListener(e -> copy(selectedRow().netflix.synopsis, "Résumé"));
		actionsPanel.add(btnCopySynopsis);
		actionsPanel.add(lblCopied);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(actionsPanel, BorderLayout.NORTH);
		bottomPanel.add(synopsisPane, BorderLayout.CENTER);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), bottomPanel);
		splitPane.setResizeWeight(0.8);
		add(splitPane, BorderLayout.CENTER);

		// Barre d'état
		JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.separator()),
				new EmptyBorder(3, 2, 0, 2)));
		statusPanel.add(statusBar, BorderLayout.CENTER);
		add(statusPanel, BorderLayout.SOUTH);
		updateSelection();
		updateStatusBar();
	}

	private void chooseCsv() {
		JFileChooser chooser = new JFileChooser(lastDirectory);
		chooser.setDialogTitle("Ouvrir un CSV Netflix");
		chooser.setFileFilter(new FileNameExtensionFilter("Fichiers CSV", "csv"));
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = chooser.getSelectedFile();
		lastDirectory = file.getParentFile();
		try {
			netflixEpisodes = NetflixCsvReader.read(file);
		} catch (NetflixCsvReader.FormatException e) {
			JOptionPane.showMessageDialog(this, "Le fichier n'a pas le format attendu.\n\n" + e.getMessage(),
					"Lecture du CSV", JOptionPane.ERROR_MESSAGE);
			return;
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Impossible de lire le fichier.\n\nDétail technique : " + e,
					"Lecture du CSV", JOptionPane.ERROR_MESSAGE);
			return;
		}
		lblFile.setText(file.getName());
		lblFile.setToolTipText(file.getAbsolutePath());

		// Autre saison de la même série : la série TVDB choisie est conservée et ses épisodes rechargés ;
		// sinon il faut chercher la série correspondant au nouveau fichier
		String showName = showNameFromFile(file);
		tvdbEpisodes = new ArrayList<>();
		numberingReference = Reference.ORIGINAL;
		if (serie != null && showName.equals(csvShowName)) {
			if (!loadTvdbEpisodes(serie, SwingUtilities.getWindowAncestor(this))) {
				serie = null;
			}
		} else {
			serie = null;
			textShowName.setText(showName);
		}
		csvShowName = showName;
		updateBtnSearch();
		refreshComparison();
	}

	// Nom de la série d'après le nom du fichier exporté, ou le nom du fichier sans extension
	private static String showNameFromFile(File file) {
		Matcher matcher = CSV_FILE_NAME.matcher(file.getName());
		if (matcher.matches()) {
			return matcher.group(1).trim();
		}
		return file.getName().replaceFirst("(?i)(\\s*\\(\\d+\\))?\\.csv$", "").trim();
	}

	// La recherche n'est possible qu'une fois un CSV chargé, avec un nom de série saisi
	private void updateBtnSearch() {
		btnSearch.setEnabled(!netflixEpisodes.isEmpty() && !textShowName.getText().trim().isEmpty());
	}

	private void searchSerie() {
		TvdbConfig config = TvdbUi.loadConfig(this);
		if (config == null) {
			return;
		}
		Window window = SwingUtilities.getWindowAncestor(this);
		tvdb = new TvdbEndpoint(config.getApiKey(), config.getPin());
		String name = textShowName.getText().trim();
		List<TvdbSerie> series;

		TvdbUi.setBusy(window, true);
		try {
			tvdb.login();
			series = tvdb.searchByName(name);
		} catch (Exception e) {
			// Erreur réseau, refus du serveur ou réponse illisible : expliquée à l'utilisateur
			e.printStackTrace();
			TvdbUi.setBusy(window, false);
			TvdbUi.showError(this, "Recherche de série", e);
			return;
		} finally {
			// Rétabli avant l'ouverture de la fenêtre de recherche, qui est bloquante
			TvdbUi.setBusy(window, false);
		}
		if (series == null) {
			JOptionPane.showMessageDialog(this, "Aucune série trouvée pour « " + name + " ».", "Recherche de série",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		// Fenêtre modale : l'appel est bloquant jusqu'à la sélection d'une série ou l'annulation
		getSearchDialog().showSeries(series);
	}

	// Fenêtre de choix de la série, propre à cet onglet ; créée au premier besoin, une fois l'onglet dans la fenêtre
	private SeriesSearchDialog getSearchDialog() {
		if (searchDialog == null) {
			searchDialog = new SeriesSearchDialog((JFrame) SwingUtilities.getWindowAncestor(this));
			searchDialog.getSelectButton().addActionListener(e -> selectSerie());
		}
		return searchDialog;
	}

	private void selectSerie() {
		TvdbSerie selected = searchDialog.getSelectedSerie();
		if (selected == null) {
			return;
		}
		// En cas d'erreur, la fenêtre de recherche reste ouverte : l'utilisateur peut réessayer ou choisir une autre série
		if (loadTvdbEpisodes(selected, searchDialog)) {
			serie = selected;
			refreshComparison();
			searchDialog.setVisible(false);
		}
	}

	/**
	 * Charge depuis TVDB les épisodes de la série pour chaque saison du CSV.
	 *
	 * @return false si le chargement a échoué (erreur déjà expliquée à l'utilisateur)
	 */
	private boolean loadTvdbEpisodes(TvdbSerie selected, Component window) {
		List<TvdbBasicEpisode> episodes = new ArrayList<>();
		TvdbUi.setBusy(window, true);
		try {
			for (int season : csvSeasons()) {
				episodes.addAll(tvdb.getSeasonEpisodes(selected.id, season));
			}
		} catch (Exception e) {
			e.printStackTrace();
			TvdbUi.setBusy(window, false);
			TvdbUi.showError(window, "Chargement des épisodes", e);
			return false;
		} finally {
			TvdbUi.setBusy(window, false);
		}
		tvdbEpisodes = episodes;
		numberingReference = Reference.ORIGINAL;
		return true;
	}

	private TreeSet<Integer> csvSeasons() {
		TreeSet<Integer> seasons = new TreeSet<>();
		for (NetflixEpisode episode : netflixEpisodes) {
			seasons.add(episode.season);
		}
		return seasons;
	}

	private void refreshComparison() {
		EpisodeNumbering numbering = EpisodeNumbering.compute(netflixEpisodes, tvdbEpisodes, numberingReference);
		tableModel.setRows(EpisodeComparison.match(netflixEpisodes, tvdbEpisodes, numbering));
		table.clearSelection();
		btnRenumber.setEnabled(serie != null && !netflixEpisodes.isEmpty() && !tvdbEpisodes.isEmpty());
		btnOriginalNumbering.setEnabled(numberingReference != Reference.ORIGINAL);
		updateSelection();
		updateStatusBar();
	}

	// Demande quelle source garde ses numéros, puis renumérote l'autre en fonction
	private void renumber() {
		Object[] options = { "Garder les numéros Netflix", "Garder les numéros TVDB", "Annuler" };
		int choice = JOptionPane.showOptionDialog(this,
				"<html>Quelle source doit garder ses numéros d'épisode ?<br><br>"
						+ "L'autre source est renumérotée d'après les titres qui correspondent ; "
						+ "ses épisodes en trop sont placés en fin de saison.<br>"
						+ "Seul l'affichage change : ni le CSV ni TVDB ne sont modifiés.</html>",
				"Renuméroter", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		Reference reference;
		if (choice == 0) {
			reference = Reference.NETFLIX;
		} else if (choice == 1) {
			reference = Reference.TVDB;
		} else {
			return;
		}
		// Sans titre commun, rien ne permet de rapprocher les épisodes
		if (EpisodeNumbering.compute(netflixEpisodes, tvdbEpisodes, reference).getAnchorCount() == 0) {
			JOptionPane.showMessageDialog(this,
					"Aucun titre ne correspond entre Netflix et TVDB (titres français absents de TVDB ?) :\n"
							+ "impossible de recaler la numérotation.",
					"Renuméroter", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		numberingReference = reference;
		refreshComparison();
	}

	/** Ligne sélectionnée, ou null si aucune */
	private EpisodeComparison selectedRow() {
		int viewRow = table.getSelectedRow();
		return viewRow < 0 ? null : tableModel.getRow(table.convertRowIndexToModel(viewRow));
	}

	private void updateSelection() {
		EpisodeComparison row = selectedRow();
		setText(netflixSynopsis, row != null && row.netflix != null ? row.netflix.synopsis : null);
		setText(tvdbOverview, row != null && row.tvdb != null ? row.tvdb.frenchOverview : null);

		// Episode présent sur TVDB : page de traduction française ; absent : page d'ajout d'épisodes de la saison
		boolean tvdbPresent = row != null && row.tvdb != null;
		btnOpenTvdb.setText(row != null && !tvdbPresent ? "Ajouter sur TVDB" : "Traduire sur TVDB");
		btnOpenTvdb.setEnabled(row != null && serie != null && serie.slug != null);
		btnOpenTvdb.setToolTipText(btnOpenTvdb.isEnabled()
				? "<html>" + tvdbPageFor(row) + "<br>Copie aussi le titre Netflix s'il est à reporter, sinon le résumé</html>"
				: null);
		btnCopyTitle.setEnabled(row != null && row.netflix != null && !row.netflix.title.isEmpty());
		btnCopySynopsis.setEnabled(row != null && row.netflix != null && !row.netflix.synopsis.isEmpty());
		lblCopied.setText(" ");
	}

	private String tvdbPageFor(EpisodeComparison row) {
		return row.tvdb != null ? TvdbSite.frenchTranslationPage(serie.slug, row.tvdb.id)
				: TvdbSite.addEpisodesPage(serie.slug, row.season);
	}

	// Ouvre la page de saisie dans le navigateur par défaut ; la saisie et la validation restent manuelles
	private void openOnTvdb() {
		EpisodeComparison row = selectedRow();
		String page = tvdbPageFor(row);

		// Prépare la saisie la plus logique : le titre s'il est à reporter, sinon le résumé
		if (row.netflix != null) {
			if (row.titleResult.isToFix()) {
				copy(row.netflix.title, "Titre");
			} else if (row.overviewResult.isToFix()) {
				copy(row.netflix.synopsis, "Résumé");
			}
		}
		try {
			Desktop.getDesktop().browse(new URI(page));
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"Impossible d'ouvrir le navigateur. Adresse de la page :\n" + page
							+ "\n\nDétail technique : " + e,
					"Ouvrir sur TVDB", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void copy(String text, String what) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.trim()), null);
		lblCopied.setText(what + " copié dans le presse-papiers");
	}

	private static void setText(JTextArea area, String text) {
		area.setText(text != null ? text : "");
		area.setCaretPosition(0);
	}

	// Episodes Netflix lus, et série TVDB choisie avec le nombre de traductions françaises existantes
	private void updateStatusBar() {
		StringBuilder status = new StringBuilder();
		if (netflixEpisodes.isEmpty()) {
			status.append("Aucun épisode Netflix chargé");
		} else {
			TreeSet<Integer> seasons = csvSeasons();
			status.append(netflixEpisodes.size()).append(" épisode(s) Netflix, saison")
					.append(seasons.size() > 1 ? "s " : " ")
					.append(String.join(", ", seasons.stream().map(String::valueOf).toList()));
		}
		status.append("   —   Série TVDB : ");
		if (serie == null) {
			status.append("aucune");
		} else {
			int frenchNames = 0;
			int frenchOverviews = 0;
			for (TvdbBasicEpisode episode : tvdbEpisodes) {
				if (episode.frenchName != null) {
					frenchNames++;
				}
				if (episode.frenchOverview != null) {
					frenchOverviews++;
				}
			}
			status.append(serie.seriesName);
			if (serie.firstAired != null && serie.firstAired.length() >= 4) {
				status.append(" (").append(serie.firstAired.substring(0, 4)).append(")");
			}
			status.append(", ").append(tvdbEpisodes.size()).append(" épisode(s) dont ").append(frenchNames)
					.append(" titre(s) et ").append(frenchOverviews).append(" résumé(s) en français");

			// Bilan de la comparaison
			int toFix = 0;
			int punctuationOnly = 0;
			int shifted = 0;
			for (int row = 0; row < tableModel.getRowCount(); row++) {
				EpisodeComparison comparison = tableModel.getRow(row);
				if (comparison.isToFix()) {
					toFix++;
				} else if (comparison.hasPunctuationOnly()) {
					punctuationOnly++;
				}
				if (!comparison.describeMatches().isEmpty()) {
					shifted++;
				}
			}
			status.append("   —   ").append(toFix).append(" épisode(s) à reporter, ").append(punctuationOnly)
					.append(" en ponctuation seule");
			if (shifted > 0) {
				status.append(", ").append(shifted).append(" décalage(s) de numérotation probable(s)");
			}
			if (numberingReference == Reference.NETFLIX) {
				status.append("   —   Numéros TVDB recalés sur Netflix");
			} else if (numberingReference == Reference.TVDB) {
				status.append("   —   Numéros Netflix recalés sur TVDB");
			}
		}
		statusBar.setText(status.toString());
	}

	/* Utilitaires de mise en page */

	private static JPanel createGroupPanel(String title) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(new TitledBorder(title));
		return panel;
	}

	// Ajoute un cadre à la zone de paramétrage ; weightx répartit l'espace horizontal restant
	private static void addGroup(JPanel settingsPanel, JPanel group, int column, double weightx) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = column;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = weightx;
		c.insets = new Insets(0, column == 0 ? 0 : 5, 0, 0);
		settingsPanel.add(group, c);
	}

	private static GridBagConstraints gbc(int x, int y) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 4, 2, 4);
		return c;
	}

	// Bouton sur toute la largeur du cadre
	private static GridBagConstraints actionConstraints(int x, int y) {
		GridBagConstraints c = gbc(x, y);
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		return c;
	}

	private static JTextArea createTextArea() {
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setRows(4);
		return area;
	}

	private static JScrollPane createTitledScrollPane(JTextArea area, String title) {
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setBorder(new TitledBorder(title));
		return scrollPane;
	}

	/** Lignes affichées dans le tableau */
	private enum Filter {
		ALL("Tous les épisodes", r -> true),
		TO_FIX("Écarts à reporter sur TVDB", r -> r.isToFix() || !r.describeMatches().isEmpty()),
		WITH_PUNCTUATION("Écarts, ponctuation comprise",
				r -> r.isToFix() || r.hasPunctuationOnly() || !r.describeMatches().isEmpty());

		private final String label;
		private final RowFilter<ComparisonTableModel, Integer> rowFilter;

		Filter(String label, Predicate<EpisodeComparison> accepted) {
			this.label = label;
			this.rowFilter = new RowFilter<>() {
				@Override
				public boolean include(Entry<? extends ComparisonTableModel, ? extends Integer> entry) {
					return accepted.test(entry.getModel().getRow(entry.getIdentifier()));
				}
			};
		}

		@Override
		public String toString() {
			return label;
		}
	}

	/** Résultat de comparaison coloré selon son importance */
	private static class ResultRenderer extends DefaultTableCellRenderer {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected && value instanceof Result) {
				setForeground(foreground((Result) value, table.getForeground()));
			}
			return this;
		}

		private static Color foreground(Result result, Color defaultColor) {
			switch (result) {
			case EPISODE_MISSING:
			case MISSING:
				return Theme.errorForeground();
			case DIFFERENT:
				return Theme.warningForeground();
			case IDENTICAL:
				return Theme.successForeground();
			case PUNCTUATION:
			case NETFLIX_MISSING:
				return Theme.ignoredForeground();
			default:
				return defaultColor;
			}
		}
	}

	/** Correspondance avec un autre numéro, signalée comme un écart à vérifier */
	private static class MatchRenderer extends DefaultTableCellRenderer {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected) {
				setForeground(Theme.warningForeground());
			}
			boolean empty = value == null || value.toString().isEmpty();
			setToolTipText(empty ? null
					: "<html>Le titre correspond à celui d'un autre numéro : "
							+ "la numérotation est probablement décalée entre Netflix et TVDB<br>"
							+ "(par exemple un épisode présent d'un seul côté).</html>");
			return this;
		}
	}

	/** Colonnes du tableau de comparaison ; l'ordre de déclaration détermine leur position */
	private enum Column {
		SEASON("Saison", 50, Integer.class, r -> r.season),
		NUMBER("Épisode", 55, Integer.class, r -> r.number),
		TITLE_RESULT("Titre", 140, Result.class, r -> r.titleResult),
		OVERVIEW_RESULT("Résumé", 140, Result.class, r -> r.overviewResult),
		MATCH("Autre numéro", 170, String.class, EpisodeComparison::describeMatches),
		ORIGINAL_NUMBER("N° d'origine", 110, String.class, EpisodeComparison::describeOriginalNumbers),
		NETFLIX_TITLE("Titre Netflix", 230, String.class, r -> r.netflix != null ? r.netflix.title : null),
		TVDB_TITLE("Titre TVDB (FR)", 230, String.class, r -> r.tvdb != null ? r.tvdb.frenchName : null),
		NETFLIX_SYNOPSIS("Résumé Netflix", 300, String.class, r -> r.netflix != null ? r.netflix.synopsis : null),
		TVDB_OVERVIEW("Résumé TVDB (FR)", 300, String.class, r -> r.tvdb != null ? r.tvdb.frenchOverview : null);

		private final String name;
		private final int width;
		private final Class<?> type; // Integer : tri numérique
		private final Function<EpisodeComparison, Object> value;

		Column(String name, int width, Class<?> type, Function<EpisodeComparison, Object> value) {
			this.name = name;
			this.width = width;
			this.type = type;
			this.value = value;
		}
	}

	private static class ComparisonTableModel extends AbstractTableModel {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		private List<EpisodeComparison> rows = new ArrayList<>();

		void setRows(List<EpisodeComparison> rows) {
			this.rows = rows;
			fireTableDataChanged();
		}

		EpisodeComparison getRow(int row) {
			return rows.get(row);
		}

		@Override
		public int getRowCount() {
			return rows.size();
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
		public Class<?> getColumnClass(int col) {
			return Column.values()[col].type;
		}

		@Override
		public Object getValueAt(int row, int col) {
			return Column.values()[col].value.apply(rows.get(row));
		}
	}
}
