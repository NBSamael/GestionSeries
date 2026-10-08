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
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
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

import api.Episode;
import api.DataSource;
import api.DataSourceType;
import api.Series;
import data.EpisodeComparison;
import data.EpisodeNumbering;
import data.EpisodeNumbering.Reference;
import data.NetflixCsvReader;
import data.NetflixEpisode;
import data.TextComparison.Result;

/**
 * Onglet de comparaison entre un CSV exporté de Netflix et les épisodes de la source de données,
 * pour préparer la saisie des traductions françaises sur le site de la source.
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
	private final JTextArea sourceOverview = createTextArea();
	private final JLabel lblFile = new JLabel("Aucun fichier chargé");
	private final JTextField textShowName = new JTextField(20);
	private final JButton btnSearch = new JButton("Rechercher Série");
	private final JLabel statusBar = new JLabel();

	/* Dossier proposé à l'ouverture du CSV : le dernier utilisé, sinon les téléchargements */
	private File lastDirectory = new File(System.getProperty("user.home"), "Downloads");

	private List<NetflixEpisode> netflixEpisodes = new ArrayList<>();
	// Nom de la série déduit du nom du dernier CSV chargé
	private String csvShowName;

	private DataSource source;
	private SeriesSearchDialog searchDialog;
	// Série choisie dans la source (null si aucune) et ses épisodes des saisons du CSV
	private Series serie;
	private List<Episode> sourceEpisodes = new ArrayList<>();
	// Source dont les numéros sont conservés (ORIGINAL : aucune renumérotation)
	private Reference numberingReference = Reference.ORIGINAL;
	// Source de données choisie dans les préférences ; son nom apparaît dans les libellés
	private DataSourceType dataSource;
	private final TitledBorder showPanelBorder = new TitledBorder("");
	private final TitledBorder sourceOverviewBorder = new TitledBorder("");
	private final JButton btnOpenSource = new JButton();
	private final JButton btnCopyTitle = new JButton("Copier le titre");
	private final JButton btnCopySynopsis = new JButton("Copier le résumé");
	private final JLabel lblCopied = new JLabel(" ");
	private final JButton btnRenumber = new JButton("Renuméroter…");
	private final JButton btnOriginalNumbering = new JButton("Numérotation d'origine");

	public NetflixComparePanel(DataSourceType dataSource) {
		super(new BorderLayout(0, 5));
		setBorder(new EmptyBorder(5, 5, 5, 5));
		this.dataSource = dataSource;

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

		// 2. Série
		JPanel showPanel = new JPanel(new GridBagLayout());
		showPanel.setBorder(showPanelBorder);
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
		// Libellés des filtres, avec le nom de la source
		comboFilter.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof Filter) {
					setText(((Filter) value).label(sourceName()));
				}
				return this;
			}
		});
		filterPanel.add(comboFilter, gbc(1, 0));

		btnRenumber.setEnabled(false);
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
				createTitledScrollPane(sourceOverview, sourceOverviewBorder));
		synopsisPane.setResizeWeight(0.5);

		// Actions sur l'épisode sélectionné, au-dessus de ses résumés
		JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		actionsPanel.setBorder(new EmptyBorder(0, 0, 4, 0));
		actionsPanel.add(new JLabel("Épisode sélectionné :"));
		btnOpenSource.addActionListener(e -> openOnSource());
		actionsPanel.add(btnOpenSource);
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
		updateLabels();
		updateSelection();
		updateStatusBar();
	}

	/**
	 * Change de source de données : la série choisie et ses épisodes sont oubliés
	 * (il faut chercher la série dans la nouvelle source), le CSV est conservé.
	 */
	public void setDataSource(DataSourceType dataSource) {
		this.dataSource = dataSource;
		source = null;
		serie = null;
		sourceEpisodes = new ArrayList<>();
		numberingReference = Reference.ORIGINAL;
		updateLabels();
		refreshComparison();
	}

	private String sourceName() {
		return dataSource.getLabel();
	}

	// Libellés fixes contenant le nom de la source ; les autres sont calculés à l'affichage
	private void updateLabels() {
		showPanelBorder.setTitle("2. Série " + sourceName());
		sourceOverviewBorder.setTitle("Résumé " + sourceName() + " (FR)");
		btnRenumber.setToolTipText(
				"<html>Recale la numérotation d'une source sur l'autre, d'après les titres qui correspondent<br>"
						+ "(affichage seulement : ni le CSV ni " + sourceName() + " ne sont modifiés)</html>");
		for (Column column : Column.values()) {
			table.getColumnModel().getColumn(table.convertColumnIndexToView(column.ordinal()))
					.setHeaderValue(column.name(sourceName()));
		}
		tableModel.sourceName = sourceName();
		table.getTableHeader().repaint();
		comboFilter.repaint();
		repaint();
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

		// Autre saison de la même série : la série choisie est conservée et ses épisodes rechargés ;
		// sinon il faut chercher la série correspondant au nouveau fichier
		String showName = showNameFromFile(file);
		sourceEpisodes = new ArrayList<>();
		numberingReference = Reference.ORIGINAL;
		if (serie != null && showName.equals(csvShowName)) {
			if (!loadSourceEpisodes(serie, SwingUtilities.getWindowAncestor(this))) {
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
		source = SourceUi.createSource(this, dataSource);
		if (source == null) {
			return;
		}
		Window window = SwingUtilities.getWindowAncestor(this);
		String name = textShowName.getText().trim();
		List<Series> series;

		SourceUi.setBusy(window, true);
		try {
			source.connect();
			series = source.searchByName(name);
		} catch (Exception e) {
			// Erreur réseau, refus du serveur ou réponse illisible : expliquée à l'utilisateur
			e.printStackTrace();
			SourceUi.setBusy(window, false);
			SourceUi.showError(this, "Recherche de série", e, source);
			return;
		} finally {
			// Rétabli avant l'ouverture de la fenêtre de recherche, qui est bloquante
			SourceUi.setBusy(window, false);
		}
		if (series.isEmpty()) {
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
		Series selected = searchDialog.getSelectedSerie();
		if (selected == null) {
			return;
		}
		// En cas d'erreur, la fenêtre de recherche reste ouverte : l'utilisateur peut réessayer ou choisir une autre série
		if (loadSourceEpisodes(selected, searchDialog)) {
			serie = selected;
			refreshComparison();
			searchDialog.setVisible(false);
		}
	}

	/**
	 * Charge depuis la source les épisodes de la série pour chaque saison du CSV.
	 *
	 * @return false si le chargement a échoué (erreur déjà expliquée à l'utilisateur)
	 */
	private boolean loadSourceEpisodes(Series selected, Component window) {
		List<Episode> episodes = new ArrayList<>();
		SourceUi.setBusy(window, true);
		try {
			for (int season : csvSeasons()) {
				episodes.addAll(source.getSeasonEpisodes(selected, season));
			}
		} catch (Exception e) {
			e.printStackTrace();
			SourceUi.setBusy(window, false);
			SourceUi.showError(window, "Chargement des épisodes", e, source);
			return false;
		} finally {
			SourceUi.setBusy(window, false);
		}
		sourceEpisodes = episodes;
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
		EpisodeNumbering numbering = EpisodeNumbering.compute(netflixEpisodes, sourceEpisodes, numberingReference);
		tableModel.setRows(EpisodeComparison.match(netflixEpisodes, sourceEpisodes, numbering));
		table.clearSelection();
		btnRenumber.setEnabled(serie != null && !netflixEpisodes.isEmpty() && !sourceEpisodes.isEmpty());
		btnOriginalNumbering.setEnabled(numberingReference != Reference.ORIGINAL);
		updateSelection();
		updateStatusBar();
	}

	// Demande quelle source garde ses numéros, puis renumérote l'autre en fonction
	private void renumber() {
		Object[] options = { "Garder les numéros Netflix", "Garder les numéros " + sourceName(), "Annuler" };
		int choice = JOptionPane.showOptionDialog(this,
				"<html>Quelle source doit garder ses numéros d'épisode ?<br><br>"
						+ "L'autre source est renumérotée d'après les titres qui correspondent ; "
						+ "ses épisodes en trop sont placés en fin de saison.<br>"
						+ "Seul l'affichage change : ni le CSV ni " + sourceName() + " ne sont modifiés.</html>",
				"Renuméroter", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		Reference reference;
		if (choice == 0) {
			reference = Reference.NETFLIX;
		} else if (choice == 1) {
			reference = Reference.SOURCE;
		} else {
			return;
		}
		// Sans titre commun, rien ne permet de rapprocher les épisodes
		if (EpisodeNumbering.compute(netflixEpisodes, sourceEpisodes, reference).getAnchorCount() == 0) {
			JOptionPane.showMessageDialog(this,
					"Aucun titre ne correspond entre Netflix et " + sourceName() + " (titres français absents de "
							+ sourceName() + " ?) :\n"
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
		setText(sourceOverview, row != null && row.source != null ? row.source.frenchOverview : null);

		// Episode présent dans la source : page de traduction française ; absent : page d'ajout d'épisodes de la saison
		boolean sourcePresent = row != null && row.source != null;
		btnOpenSource.setText(row != null && !sourcePresent ? "Ajouter sur " + sourceName() : "Traduire sur " + sourceName());
		btnOpenSource.setEnabled(row != null && serie != null && sourcePageFor(row) != null);
		btnOpenSource.setToolTipText(btnOpenSource.isEnabled()
				? "<html>" + sourcePageFor(row) + "<br>Copie aussi le titre Netflix s'il est à reporter, sinon le résumé</html>"
				: null);
		btnCopyTitle.setEnabled(row != null && row.netflix != null && !row.netflix.title.isEmpty());
		btnCopySynopsis.setEnabled(row != null && row.netflix != null && !row.netflix.synopsis.isEmpty());
		lblCopied.setText(" ");
	}

	private String sourcePageFor(EpisodeComparison row) {
		return row.source != null ? source.frenchTranslationPage(serie, row.source)
				: source.addEpisodesPage(serie, row.season);
	}

	// Ouvre la page de saisie dans le navigateur par défaut ; la saisie et la validation restent manuelles
	private void openOnSource() {
		EpisodeComparison row = selectedRow();
		String page = sourcePageFor(row);

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
					"Ouvrir sur " + sourceName(), JOptionPane.ERROR_MESSAGE);
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

	// Episodes Netflix lus, et série choisie avec le nombre de traductions françaises existantes
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
		status.append("   —   Série " + sourceName() + " : ");
		if (serie == null) {
			status.append("aucune");
		} else {
			int frenchNames = 0;
			int frenchOverviews = 0;
			for (Episode episode : sourceEpisodes) {
				if (episode.frenchName != null) {
					frenchNames++;
				}
				if (episode.frenchOverview != null) {
					frenchOverviews++;
				}
			}
			status.append(serie.name);
			if (serie.firstAired != null && serie.firstAired.length() >= 4) {
				status.append(" (").append(serie.firstAired.substring(0, 4)).append(")");
			}
			status.append(", ").append(sourceEpisodes.size()).append(" épisode(s) dont ").append(frenchNames)
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
				if (comparison.hasMatches()) {
					shifted++;
				}
			}
			status.append("   —   ").append(toFix).append(" épisode(s) à reporter, ").append(punctuationOnly)
					.append(" en ponctuation seule");
			if (shifted > 0) {
				status.append(", ").append(shifted).append(" décalage(s) de numérotation probable(s)");
			}
			if (numberingReference == Reference.NETFLIX) {
				status.append("   —   Numéros " + sourceName() + " recalés sur Netflix");
			} else if (numberingReference == Reference.SOURCE) {
				status.append("   —   Numéros Netflix recalés sur " + sourceName());
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
		return createTitledScrollPane(area, new TitledBorder(title));
	}

	// Le titre de la bordure peut être modifié ensuite (libellé dépendant de la source)
	private static JScrollPane createTitledScrollPane(JTextArea area, TitledBorder border) {
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setBorder(border);
		return scrollPane;
	}
	/** Lignes affichées dans le tableau */
	private enum Filter {
		ALL("Tous les épisodes", r -> true),
		TO_FIX("Écarts à reporter sur %s", r -> r.isToFix() || r.hasMatches()),
		WITH_PUNCTUATION("Écarts, ponctuation comprise", r -> r.isToFix() || r.hasPunctuationOnly() || r.hasMatches());

		// Libellé ; %s y est remplacé par le nom de la source
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

		String label(String sourceName) {
			return String.format(label, sourceName);
		}
	}

	/** Résultat de comparaison coloré selon son importance */
	private class ResultRenderer extends DefaultTableCellRenderer {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Result) {
				setText(((Result) value).label(sourceName()));
				if (!isSelected) {
					setForeground(foreground((Result) value, table.getForeground()));
				}
			}
			return this;
		}

		private Color foreground(Result result, Color defaultColor) {
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
	private class MatchRenderer extends DefaultTableCellRenderer {

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
							+ "la numérotation est probablement décalée entre Netflix et " + sourceName() + "<br>"
							+ "(par exemple un épisode présent d'un seul côté).</html>");
			return this;
		}
	}

	/**
	 * Colonnes du tableau de comparaison ; l'ordre de déclaration détermine leur
	 * position. Dans les noms, %s est remplacé par le nom de la source ; les
	 * valeurs reçoivent ce nom en second paramètre.
	 */
	private enum Column {
		SEASON("Saison", 50, Integer.class, (r, s) -> r.season),
		NUMBER("Épisode", 55, Integer.class, (r, s) -> r.number),
		TITLE_RESULT("Titre", 140, Result.class, (r, s) -> r.titleResult),
		OVERVIEW_RESULT("Résumé", 140, Result.class, (r, s) -> r.overviewResult),
		MATCH("Autre numéro", 170, String.class, EpisodeComparison::describeMatches),
		ORIGINAL_NUMBER("N° d'origine", 110, String.class, EpisodeComparison::describeOriginalNumbers),
		NETFLIX_TITLE("Titre Netflix", 230, String.class, (r, s) -> r.netflix != null ? r.netflix.title : null),
		SOURCE_TITLE("Titre %s (FR)", 230, String.class, (r, s) -> r.source != null ? r.source.frenchName : null),
		NETFLIX_SYNOPSIS("Résumé Netflix", 300, String.class,
				(r, s) -> r.netflix != null ? r.netflix.synopsis : null),
		SOURCE_OVERVIEW("Résumé %s (FR)", 300, String.class,
				(r, s) -> r.source != null ? r.source.frenchOverview : null);

		private final String name;
		private final int width;
		private final Class<?> type; // Integer : tri numérique
		private final BiFunction<EpisodeComparison, String, Object> value;

		Column(String name, int width, Class<?> type, BiFunction<EpisodeComparison, String, Object> value) {
			this.name = name;
			this.width = width;
			this.type = type;
			this.value = value;
		}

		String name(String sourceName) {
			return String.format(name, sourceName);
		}
	}

	private static class ComparisonTableModel extends AbstractTableModel {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		private List<EpisodeComparison> rows = new ArrayList<>();
		// Nom de la source, repris dans les noms de colonnes et certaines valeurs
		private String sourceName = "";

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
			return Column.values()[col].name(sourceName);
		}

		@Override
		public Class<?> getColumnClass(int col) {
			return Column.values()[col].type;
		}

		@Override
		public Object getValueAt(int row, int col) {
			return Column.values()[col].value.apply(rows.get(row), sourceName);
		}
	}
}
