package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import com.formdev.flatlaf.FlatDarkLaf;

import api.TvdbSerie;
import data.FileItem;
import data.FileItemList;
import data.SeasonReading;

public class Application {

	/* Taille de la fenêtre principale à l'ouverture, et hauteur minimale */
	private static final int DEFAULT_WIDTH = 1080;
	private static final int DEFAULT_HEIGHT = 600;
	private static final int MINIMUM_HEIGHT = 400;

	/* Bornes des spinners de numérotation */
	private static final int MIN_POSITION_SLIDER_MAX = 30; // maximum des sliders de position tant qu'aucun nom long n'est listé
	private static final int POSITION_SLIDER_WIDTH = 180;
	private static final int MAX_NUMBER_SIZE = 4;
	private static final int MAX_SEASON_NUMBER = 999;

	public JFrame frmGestionSeries;
	public JTable table;
	public FileItemList itemList;
	public JTextField textShowName;
	public JButton btnScannerDossier;
	public JSlider sliderSeasonNumPos;
	public JSpinner spinnerSeasonNumSize;
	public JSlider sliderEpisodeNumPos;
	public JSpinner spinnerEpisodeNumSize;
	public JButton btnTraitementDesDonnes;
	public JButton btnPaste;
	public JButton btnRenommer;
	public JComboBox<SeasonReading> comboSeasonReading;
	public JCheckBox chckbxRecursive;
	public JSpinner spinnerSeasonNum;
	public JButton btnNewName;
	public JSpinner spinnerOffset;
	public JSpinner spinnerFinalLength;
	public boolean colorization;

	public SeriesSearchDialog searchDialog;

	/* Barre d'état et informations qu'elle affiche */
	private JLabel statusBar;
	public File currentDirectory;
	public TvdbSerie currentSerie;
	public JButton btnRecherche;
	public JLabel lblSerie;
	public JButton btnEpisode;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		// Thème sombre FlatLaf ; en cas d'échec, thème natif du système
		if (!FlatDarkLaf.setup()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Application window = new Application();
					window.frmGestionSeries.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * L'option "Nom de dossier" de la lecture de saison n'est proposée que si les
	 * sous-dossiers sont inclus. Si elle était sélectionnée quand elle disparaît,
	 * la lecture repasse sur "Nom d'épisode".
	 */
	public void updateSeasonReadingOptions() {
		boolean folderOptionPresent = false;
		for (int i = 0; i < comboSeasonReading.getItemCount(); i++) {
			if (comboSeasonReading.getItemAt(i) == SeasonReading.FOLDER_NAME) {
				folderOptionPresent = true;
			}
		}
		if (chckbxRecursive.isSelected() && !folderOptionPresent) {
			comboSeasonReading.addItem(SeasonReading.FOLDER_NAME);
		} else if (!chckbxRecursive.isSelected() && folderOptionPresent) {
			if (comboSeasonReading.getSelectedItem() == SeasonReading.FOLDER_NAME) {
				comboSeasonReading.setSelectedItem(SeasonReading.FILE_NAME);
			}
			comboSeasonReading.removeItem(SeasonReading.FOLDER_NAME);
		}
	}

	/**
	 * Met à jour la barre d'état : dossier scanné, nombre de fichiers (dont
	 * ignorés et en erreur) et série sélectionnée.
	 */
	public void updateStatusBar() {
		StringBuilder status = new StringBuilder();
		if (currentDirectory == null) {
			status.append("Aucun dossier scanné");
		} else {
			int ignored = 0;
			int errors = 0;
			for (FileItem item : itemList) {
				if (!item.selected) {
					ignored++;
				} else if (item.status == FileItem.Status.ERREUR) {
					errors++;
				}
			}
			status.append("Dossier : ").append(currentDirectory.getAbsolutePath());
			status.append("   —   ").append(itemList.size()).append(" fichier(s)");
			status.append(", ").append(ignored).append(" ignoré(s)");
			status.append(", ").append(errors).append(" en erreur");
		}
		status.append("   —   Série : ");
		if (currentSerie == null) {
			status.append("aucune");
		} else {
			status.append(currentSerie.seriesName);
			if (currentSerie.firstAired != null && currentSerie.firstAired.length() >= 4) {
				status.append(" (").append(currentSerie.firstAired.substring(0, 4)).append(")");
			}
		}
		statusBar.setText(status.toString());
	}

	/**
	 * Create the application.
	 */
	public Application() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		colorization = false;

		ButtonListener buttonListener = new ButtonListener(this);

		frmGestionSeries = new JFrame();
		frmGestionSeries.setResizable(true);
		frmGestionSeries.setTitle("Gestion Séries");
		frmGestionSeries.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Deux fonctionnalités indépendantes, chacune dans son onglet
		JTabbedPane tabbedPane = new JTabbedPane();
		frmGestionSeries.setContentPane(tabbedPane);

		// Onglet Renommage
		JPanel contentPane = new JPanel(new BorderLayout(0, 5));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Renommage", contentPane);

		// Onglet Comparaison Netflix
		tabbedPane.addTab("Comparaison Netflix", new NetflixComparePanel());

		// Fenêtre de choix de la série, ouverte à chaque recherche
		searchDialog = new SeriesSearchDialog(frmGestionSeries);
		btnEpisode = searchDialog.getSelectButton();
		btnEpisode.addActionListener(buttonListener);

		// Zone de paramétrage : un cadre par étape, dans l'ordre d'utilisation
		JPanel settingsPanel = new JPanel(new GridBagLayout());
		contentPane.add(settingsPanel, BorderLayout.NORTH);

		// 1. Fichiers
		JPanel filesPanel = createGroupPanel("1. Fichiers");
		addGroup(settingsPanel, filesPanel, 0, 0.0);

		btnScannerDossier = new JButton("Scanner Dossier");
		btnScannerDossier.addActionListener(buttonListener);
		filesPanel.add(btnScannerDossier, gbc(0, 0));

		chckbxRecursive = new JCheckBox("Inclure les sous-dossiers");
		chckbxRecursive.setSelected(false);
		chckbxRecursive.addActionListener(buttonListener);
		filesPanel.add(chckbxRecursive, gbc(0, 1));

		// 2. Série
		JPanel showPanel = createGroupPanel("2. Série");
		addGroup(settingsPanel, showPanel, 1, 0.3);

		showPanel.add(new JLabel("Nom Série"), gbc(0, 0));

		textShowName = new JTextField();
		textShowName.setColumns(15);
		textShowName.getDocument().addDocumentListener(buttonListener);
		GridBagConstraints textConstraints = gbc(1, 0);
		textConstraints.fill = GridBagConstraints.HORIZONTAL;
		textConstraints.weightx = 1.0;
		showPanel.add(textShowName, textConstraints);

		btnRecherche = new JButton("Rechercher Série");
		btnRecherche.setEnabled(false);
		btnRecherche.addActionListener(buttonListener);

		// Entrée dans le champ de saisie : lance la recherche, si le bouton est actif
		textShowName.addActionListener(e -> {
			if (btnRecherche.isEnabled()) {
				btnRecherche.doClick();
			}
		});
		GridBagConstraints searchConstraints = gbc(1, 1);
		searchConstraints.anchor = GridBagConstraints.EAST;
		showPanel.add(btnRecherche, searchConstraints);

		// 3. Numérotation
		JPanel numberingPanel = createGroupPanel("3. Numérotation");
		addGroup(settingsPanel, numberingPanel, 2, 0.7);

		comboSeasonReading = new JComboBox<>(SeasonReading.values());
		comboSeasonReading.setSelectedItem(SeasonReading.FILE_NAME);
		comboSeasonReading.addActionListener(buttonListener);
		addLabeled(numberingPanel, "Lecture Saison", comboSeasonReading, 0, 0,
				"<html>Où lire le numéro de saison :<br>"
						+ "<b>Non</b> : numéro saisi dans « Numéro Saison », identique pour tous les fichiers<br>"
						+ "<b>Nom d'épisode</b> : dans le nom du fichier<br>"
						+ "<b>Nom de dossier</b> : dans la colonne Dossier (avec les sous-dossiers inclus)</html>");
		updateSeasonReadingOptions();

		spinnerSeasonNum = createSpinner(0, 0, MAX_SEASON_NUMBER);
		spinnerSeasonNum.setEnabled(false);
		spinnerSeasonNum.addChangeListener(buttonListener);
		addLabeled(numberingPanel, "Numéro Saison", spinnerSeasonNum, 2, 0,
				"Numéro de saison appliqué à tous les fichiers (0 pour les épisodes spéciaux)");

		sliderSeasonNumPos = createPositionSlider(3);
		sliderSeasonNumPos.addChangeListener(buttonListener);
		addLabeled(numberingPanel, "Position Numéro Saison", withValueLabel(sliderSeasonNumPos), 0, 1,
				"Position du premier chiffre du numéro de saison, en comptant à partir de 0");

		spinnerSeasonNumSize = createSpinner(2, 1, MAX_NUMBER_SIZE);
		spinnerSeasonNumSize.addChangeListener(buttonListener);
		addLabeled(numberingPanel, "Taille Numéro Saison", spinnerSeasonNumSize, 2, 1,
				"Nombre de chiffres du numéro de saison");

		sliderEpisodeNumPos = createPositionSlider(5);
		sliderEpisodeNumPos.addChangeListener(buttonListener);
		addLabeled(numberingPanel, "Position Numéro Episode", withValueLabel(sliderEpisodeNumPos), 0, 2,
				"Position du premier chiffre du numéro d'épisode dans le nom du fichier, en comptant à partir de 0");

		spinnerEpisodeNumSize = createSpinner(2, 1, MAX_NUMBER_SIZE);
		spinnerEpisodeNumSize.addChangeListener(buttonListener);
		addLabeled(numberingPanel, "Taille Numéro Episode", spinnerEpisodeNumSize, 2, 2,
				"Nombre de chiffres du numéro d'épisode");

		// L'offset peut être négatif : pas de borne
		spinnerOffset = createSpinner(0, null, null);
		addLabeled(numberingPanel, "Offset", spinnerOffset, 0, 3,
				"Valeur ajoutée au numéro d'épisode lu (négative pour le diminuer), "
						+ "par exemple quand la numérotation des fichiers est décalée par rapport à TVDB");

		spinnerFinalLength = createSpinner(2, 1, MAX_NUMBER_SIZE);
		addLabeled(numberingPanel, "Longueur finale", spinnerFinalLength, 2, 3,
				"Nombre de chiffres du numéro d'épisode dans le nouveau nom (complété par des zéros à gauche)");

		// Légende des couleurs de sélection dans le tableau
		JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		legendPanel.add(new JLabel("Sélections :"));
		legendPanel.add(createLegendLabel("Saison", Theme.seasonBackground()));
		legendPanel.add(createLegendLabel("Episode", Theme.episodeBackground()));
		legendPanel.add(createLegendLabel("Chevauchement", Theme.overlapBackground()));
		GridBagConstraints legendConstraints = gbc(0, 4);
		legendConstraints.gridwidth = 4;
		numberingPanel.add(legendPanel, legendConstraints);

		// 4. Actions
		JPanel actionsPanel = createGroupPanel("4. Actions");
		addGroup(settingsPanel, actionsPanel, 3, 0.0);

		btnTraitementDesDonnes = new JButton("Traitement des données");
		btnTraitementDesDonnes.setEnabled(false);
		btnTraitementDesDonnes.addActionListener(buttonListener);
		actionsPanel.add(btnTraitementDesDonnes, actionConstraints(0));

		btnNewName = new JButton("Déterminer nom");
		btnNewName.setEnabled(false);
		btnNewName.addActionListener(buttonListener);
		actionsPanel.add(btnNewName, actionConstraints(1));

		btnRenommer = new JButton("Renommer");
		btnRenommer.setEnabled(false);
		btnRenommer.addActionListener(buttonListener);
		actionsPanel.add(btnRenommer, actionConstraints(2));

//		btnPaste = new JButton("Coller");
//		btnPaste.setEnabled(false);
//		btnPaste.addActionListener(buttonListener);
//		actionsPanel.add(btnPaste, actionConstraints(3));

		// Tableau des fichiers
		itemList = new FileItemList(this);

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		table = new JTable(itemList.getTableModel());
		itemList.getTableModel().setTable(table);
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		table.setCellSelectionEnabled(true);
		table.setAutoCreateRowSorter(true);
		scrollPane.setViewportView(table);

		// Barre d'état, mise à jour à chaque modification du tableau
		statusBar = new JLabel();
		JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.separator()),
				new EmptyBorder(3, 2, 0, 2)));
		statusPanel.add(statusBar, BorderLayout.CENTER);
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		itemList.getTableModel().addTableModelListener(e -> updateStatusBar());
		updateStatusBar();

		// Taille de la fenêtre : la largeur minimale est celle qui permet d'afficher
		// les cadres de paramétrage en entier ; le tableau prend tout l'espace restant
		frmGestionSeries.pack();
		Dimension minimumSize = new Dimension(frmGestionSeries.getWidth(), MINIMUM_HEIGHT);
		frmGestionSeries.setMinimumSize(minimumSize);
		frmGestionSeries.setSize(Math.max(DEFAULT_WIDTH, minimumSize.width), DEFAULT_HEIGHT);
		frmGestionSeries.setLocationRelativeTo(null);
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

	// Les boutons d'action ont tous la même largeur
	private static GridBagConstraints actionConstraints(int row) {
		GridBagConstraints c = gbc(0, row);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		return c;
	}

	// Ajoute un libellé et son composant sur une même ligne, à partir de la colonne indiquée ;
	// l'info-bulle s'affiche au survol du libellé comme du composant
	private static void addLabeled(JPanel panel, String label, JComponent component, int column, int row,
			String tooltip) {
		JLabel jLabel = new JLabel(label);
		jLabel.setToolTipText(tooltip);
		component.setToolTipText(tooltip);
		if (component instanceof JSpinner) {
			// Le champ de saisie du spinner recouvre le spinner : il doit porter l'info-bulle lui aussi
			((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField().setToolTipText(tooltip);
		} else if (component instanceof JPanel) {
			// Composant composé (slider + valeur) : chaque élément porte l'info-bulle
			for (Component child : component.getComponents()) {
				((JComponent) child).setToolTipText(tooltip);
			}
		}
		panel.add(jLabel, gbc(column, row));
		GridBagConstraints c = gbc(column + 1, row);
		c.insets = new Insets(2, 4, 2, 16);
		panel.add(component, c);
	}

	// Etiquette de légende : le texte sur la couleur de fond qu'il représente
	private static JLabel createLegendLabel(String text, Color background) {
		JLabel label = new JLabel(text);
		label.setOpaque(true);
		label.setBackground(background);
		label.setBorder(new EmptyBorder(1, 6, 1, 6));
		return label;
	}

	private static JSlider createPositionSlider(int initialValue) {
		JSlider slider = new JSlider(0, MIN_POSITION_SLIDER_MAX, initialValue);
		slider.setPreferredSize(new Dimension(POSITION_SLIDER_WIDTH, slider.getPreferredSize().height));
		return slider;
	}

	// Associe au slider un libellé affichant sa valeur courante
	private static JPanel withValueLabel(JSlider slider) {
		JLabel valueLabel = new JLabel("000");
		valueLabel.setPreferredSize(valueLabel.getPreferredSize());
		valueLabel.setHorizontalAlignment(JLabel.RIGHT);
		valueLabel.setText(Integer.toString(slider.getValue()));
		slider.addChangeListener(e -> valueLabel.setText(Integer.toString(slider.getValue())));

		JPanel panel = new JPanel(new BorderLayout(4, 0));
		panel.add(slider, BorderLayout.CENTER);
		panel.add(valueLabel, BorderLayout.EAST);
		return panel;
	}

	/**
	 * Ajuste le maximum des sliders de position à la longueur du plus long nom de
	 * fichier ou de dossier listé, pour qu'ils couvrent tous les noms sans perdre
	 * en précision.
	 */
	public void updatePositionSlidersMaximum() {
		int maximum = MIN_POSITION_SLIDER_MAX;
		for (FileItem item : itemList) {
			maximum = Math.max(maximum, item.originalName.length());
			maximum = Math.max(maximum, item.folder.length());
		}
		sliderSeasonNumPos.setMaximum(maximum);
		sliderEpisodeNumPos.setMaximum(maximum);
	}

	// Spinner d'entiers ; min et max à null pour ne pas borner
	private static JSpinner createSpinner(int initialValue, Integer min, Integer max) {
		JSpinner spinner = new JSpinner(
				new SpinnerNumberModel(Integer.valueOf(initialValue), min, max, Integer.valueOf(1)));
		((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
		return spinner;
	}
}
