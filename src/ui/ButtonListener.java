package ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import api.TvdbBasicEpisode;
import api.TvdbConfig;
import api.TvdbEndpoint;
import api.TvdbErrors;
import api.TvdbSerie;
import api.TvdbSeriesEpisodes;
import data.FileItem;
import data.SeasonReading;
import data.ShowInformations;

public class ButtonListener implements java.awt.event.ActionListener, ChangeListener, DocumentListener {
	private List<TvdbSerie> series;
	private TvdbEndpoint tvdb;

	private TvdbSeriesEpisodes episodes;
	private String nomSerie = null;
	private JTable table;
	private boolean directoryScanned = false;

	Application app;

	public ButtonListener(Application app) {
		super();
		this.app = app;
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		this.app.itemList.refresh();
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		updateBtnRecherche();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		updateBtnRecherche();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		updateBtnRecherche();
	}

	// Le bouton de recherche n'est actif que si un dossier a été scanné et qu'un nom de série est saisi
	private void updateBtnRecherche() {
		app.btnRecherche.setEnabled(directoryScanned && !app.textShowName.getText().trim().isEmpty());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Si c'est le bouton de scan de dossier qui a été déclenché
		if (e.getSource().equals(app.btnScannerDossier)) {
			scanDirectory();
		}

		// Si c'est le bouton de recherche de séries qui a été déclenché
		if (e.getSource().equals(app.btnRecherche)) {
			rechercheSerie();
		}

		// Si c'est le bouton de recherche d'épisode qui a été déclenché
		if (e.getSource().equals(app.btnEpisode)) {
			rechercheEpisode();
		}

		// Si c'est le bouton de traitement des données qui a été déclenché
		if (e.getSource().equals(app.btnTraitementDesDonnes)) {
			completeData();
		}

		// Si c'est le bouton Déterminer nom qui a été déclenché
		if (e.getSource().equals(app.btnNewName)) {
			makeFilesName();
		}
		// Si c'est le bouton Renommer qui a été déclenché
		if (e.getSource().equals(app.btnRenommer)) {
			rename();
		}
		// Si c'est la checkbox d'inclusion des sous-dossiers qui a été modifiée
		if (e.getSource().equals(app.chckbxRecursive)) {
			app.itemList.getTableModel().setFolderColumnVisible(app.chckbxRecursive.isSelected());
			app.updateSeasonReadingOptions();
		}
		// Si c'est le mode de lecture de saison qui a été modifié
		if (e.getSource().equals(app.comboSeasonReading)) {
			boolean seasonRead = getSeasonReading().isRead();
			app.spinnerSeasonNum.setEnabled(!seasonRead);
			app.sliderSeasonNumPos.setEnabled(seasonRead);
			app.spinnerSeasonNumSize.setEnabled(seasonRead);
			// La colorisation de la saison change de colonne selon le mode
			app.itemList.refresh();
		}
	}

	private SeasonReading getSeasonReading() {
		return (SeasonReading) app.comboSeasonReading.getSelectedItem();
	}

	// Renseigne le mode de lecture de la saison et ses paramètres
	private void setSeasonInformations(ShowInformations showInfos) {
		showInfos.seasonReading = getSeasonReading();
		if (showInfos.seasonReading.isRead()) {
			showInfos.SeasonNumPos = app.sliderSeasonNumPos.getValue();
			showInfos.SeasonNumSize = (int) app.spinnerSeasonNumSize.getValue();
		} else {
			showInfos.SeasonNumber = (int) app.spinnerSeasonNum.getValue();
		}
	}

	private void rechercheSerie() {
		// Données de connexion lues à chaque recherche : une correction du fichier est prise en compte sans relancer
		TvdbConfig config;
		try {
			config = TvdbConfig.load();
		} catch (TvdbConfig.ConfigException e) {
			javax.swing.JOptionPane.showMessageDialog(app.frmGestionSeries, e.getMessage(),
					"Configuration TVDB", javax.swing.JOptionPane.ERROR_MESSAGE);
			return;
		}
		tvdb = new TvdbEndpoint(config.getApiKey(), config.getPin());
		series = null;

		setBusy(app.frmGestionSeries, true);
		try {
			tvdb.login();
			String NameSerie = app.textShowName.getText();
			series = tvdb.searchByName(NameSerie);
		} catch (Exception e1) {
			// Erreur réseau, refus du serveur ou réponse illisible : expliquée à l'utilisateur
			e1.printStackTrace();
			setBusy(app.frmGestionSeries, false);
			showTvdbError(app.frmGestionSeries, "Recherche de série", e1);
			return;
		} finally {
			// Rétabli avant l'ouverture de la fenêtre de recherche, qui est bloquante
			setBusy(app.frmGestionSeries, false);
		}
		if (series != null) {
			System.out.println(app.textShowName.getText());

			app.btnTraitementDesDonnes.setEnabled(true);

			// Fenêtre modale : l'appel est bloquant jusqu'à la sélection d'une série ou l'annulation
			app.searchDialog.showSeries(series);
		} else {
			javax.swing.JOptionPane.showMessageDialog(this.table, "Pas de série trouvée", "Problème Recherche",
					javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}

	private void rechercheEpisode() {

		TvdbSerie s = app.searchDialog.getSelectedSerie();
		if (s == null) {
			return;
		}
		setBusy(app.searchDialog, true);
		try {
			episodes = tvdb.getEpisodesList(s.id);
			nomSerie = s.seriesName;
			app.currentSerie = s;
			app.updateStatusBar();
			for (TvdbBasicEpisode tvdbBasicEpisode : episodes.tvdbBasicEpisodes.values()) {
				System.out.println("S" + tvdbBasicEpisode.airedSeason + "E"
						+ tvdbBasicEpisode.airedEpisodeNumber + " : " + tvdbBasicEpisode.episodeName);
			}
		} catch (Exception e1) {
			// La fenêtre de recherche reste ouverte : l'utilisateur peut réessayer ou choisir une autre série
			e1.printStackTrace();
			setBusy(app.searchDialog, false);
			showTvdbError(app.searchDialog, "Chargement des épisodes", e1);
			return;
		} finally {
			setBusy(app.searchDialog, false);
		}

		app.searchDialog.setVisible(false);
	}

	// Explique à l'utilisateur l'échec d'un appel à TVDB ; le détail technique reste disponible en dessous
	private void showTvdbError(Component parent, String title, Exception error) {
		String message = TvdbErrors.describe(error) + "\n\nDétail technique : " + error.getClass().getSimpleName()
				+ (error.getMessage() != null ? " - " + error.getMessage() : "");
		javax.swing.JOptionPane.showMessageDialog(parent, message, title, javax.swing.JOptionPane.ERROR_MESSAGE);
	}

	// Affiche le curseur d'attente sur la fenêtre pendant les appels à TVDB (qui bloquent l'interface)
	private void setBusy(Component window, boolean busy) {
		window.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
	}

	private void rename() {
		for (FileItem item : app.itemList) {
			if (!item.selected) {
				continue;
			}
			// Fichier recoché après "Déterminer nom" : pas de nouveau nom calculé
			if (item.treatedName == null) {
				System.out.println("Pas de nouveau nom pour : " + item.file.getAbsolutePath());
				continue;
			}
			File dest = new File(item.file.getParentFile() + "\\" + item.treatedName);
			System.out.println("Renommage : " + item.file.getAbsolutePath() + " ==> " + dest.getAbsolutePath());
			item.file.renameTo(dest);
		}
	}

	private void makeFilesName() {
		ShowInformations showInfos = new ShowInformations();
		showInfos.ShowName = nomSerie;
		setSeasonInformations(showInfos);
		showInfos.EpisodeNumPos = app.sliderEpisodeNumPos.getValue();
		showInfos.EpisodeNumSize = (int) app.spinnerEpisodeNumSize.getValue();
		app.itemList.findNewName(showInfos);
		app.btnRenommer.setEnabled(true);
	}

	// Non utilisée tant que le bouton "Coller" (btnPaste) est désactivé dans Application
	@SuppressWarnings("unused")
	private void pasteNames() {
		// System.out.println("Ligne : " + app.table.getSelectedRows()[0]);
		// StringSelection ss = (StringSelection)
		// Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		// System.out.println(ss.toString());
		String clipboardContent = null;
		try {
			clipboardContent = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
					.getData(DataFlavor.stringFlavor);
		} catch (HeadlessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedFlavorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (clipboardContent != null) {
			List<String> titles = Arrays.asList(clipboardContent.split("\n"));
			int row = app.table.getSelectedRows()[0];
			for (String title : titles) {
				title = title.replaceAll("[/\\\\*?!\"<>|]", ""); // Supprime les caractères interdits
				title = title.replaceAll(":", "-"); // remplace le : par un -
				title = title.replaceAll("  ", " "); // supprime les doubles espaces laissés par les modifications
				title = title.replaceAll(" $", ""); // supprime les espaces en fin de chaîne
				title = title.replaceAll("^ ", ""); // supprime les espaces en début de chaîne
				app.itemList.setEpisodeName((String) app.table.getValueAt(row++,
						app.table.convertColumnIndexToView(FileListTableModel.Columns.ORIGINAL_NAME.getPosition())),
						title);
			}
			app.btnNewName.setEnabled(true);
		}
	}

	private void completeData() {
		if (episodes == null) {
			javax.swing.JOptionPane.showMessageDialog(app.frmGestionSeries,
					"Aucune série sélectionnée : recherchez une série puis sélectionnez-la dans la liste.",
					"Traitement des données", javax.swing.JOptionPane.WARNING_MESSAGE);
			return;
		}
		ShowInformations showInfos = new ShowInformations();
		showInfos.ShowName = app.textShowName.getText();
		setSeasonInformations(showInfos);
		showInfos.EpisodeNumPos = app.sliderEpisodeNumPos.getValue();
		showInfos.EpisodeNumSize = (int) app.spinnerEpisodeNumSize.getValue();
		showInfos.offset = (int) app.spinnerOffset.getValue();
		showInfos.finalLength = (int) app.spinnerFinalLength.getValue();
		List<String> errors = app.itemList.completeData(showInfos, episodes);
		// En cas d'erreur, la colorisation reste active pour aider à corriger les positions
		app.colorization = !errors.isEmpty();
		app.itemList.refresh();
		app.btnNewName.setEnabled(true);
		// Les erreurs sont visibles dans le tableau (statut en rouge, raison en info-bulle)
	}

	private void scanDirectory() {
		JFileChooser directoryChooser = new JFileChooser();
		directoryChooser.setCurrentDirectory(new java.io.File("Z:\\DL\\Temp"));
		directoryChooser.setDialogTitle("Sélectionnez un dossier");
		directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		directoryChooser.setAcceptAllFileFilterUsed(false);
		//
		if (directoryChooser.showOpenDialog(app.frmGestionSeries) == JFileChooser.APPROVE_OPTION) {
			System.out.println("getSelectedFile() : " + directoryChooser.getSelectedFile());
			app.itemList.clear();

			File rootDirectory = directoryChooser.getSelectedFile();
			app.currentDirectory = rootDirectory;
			addFiles(rootDirectory, rootDirectory, app.chckbxRecursive.isSelected());
			app.updatePositionSlidersMaximum();
			app.updateStatusBar();

			directoryScanned = true;
			updateBtnRecherche();
			app.colorization = true;
			if (nomSerie == null) {
				app.btnTraitementDesDonnes.setEnabled(false);
			}
			app.btnNewName.setEnabled(false);
			app.btnRenommer.setEnabled(false);
		} else {
			System.out.println("No Selection ");
		}

	}

	// Ajoute les fichiers du dossier à la liste, et ceux de ses sous-dossiers si recursive est vrai
	private void addFiles(File rootDirectory, File directory, boolean recursive) {
		File[] files = directory.listFiles();
		if (files == null) {
			System.out.println("Dossier illisible : " + directory.getAbsolutePath());
			return;
		}
		List<File> subDirectories = new ArrayList<>();
		for (File file : files) {
			if (file.isDirectory()) {
				subDirectories.add(file);
			} else {
				app.itemList.add(new FileItem(file, rootDirectory));
			}
		}
		if (recursive) {
			for (File subDirectory : subDirectories) {
				addFiles(rootDirectory, subDirectory, true);
			}
		}
	}
}
