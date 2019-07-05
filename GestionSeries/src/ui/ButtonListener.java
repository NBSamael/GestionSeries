package ui;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.http.ParseException;

import api.TvdbBasicEpisode;
import api.TvdbEndpoint;
import api.TvdbSerie;
import api.TvdbSeriesEpisodes;
import data.FileItem;
import data.ShowInformations;

public class ButtonListener implements java.awt.event.ActionListener, ChangeListener {
	private static final String API_KEY = "D4858E1F5CC604F8";
	private static final String USER_KEY = "ECF8D275D64FAADF";
	private static final String USERNAME = "mr_flibble";

	private List<TvdbSerie> series;
	private JComboBox lsite;
	private TvdbEndpoint tvdb;
	private JList liste;

	private TvdbSeriesEpisodes episodes;
	private String NomSerie;
	private JTable table;

	Application app;

	public ButtonListener(Application app) {
		super();
		this.app = app;
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		// TODO : v�rifier que les intervales ne se chevauchent pas
		this.app.itemList.refresh();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Si c'est le bouton de scan de dossier qui a �t� d�clench�
		if (e.getSource().equals(app.btnScannerDossier)) {
			scanDirectory();
		}

		// Si c'est le bouton de recherche de s�ries qui a �t� d�clench�
		if (e.getSource().equals(app.btnRecherche)) {
			rechercheSerie();
		}

		// Si c'est le bouton de recherche d'�pisode qui a �t� d�clench�
		if (e.getSource().equals(app.btnEpisode)) {
			rechercheEpisode();
		}

		// Si c'est le bouton de traitement des donn�es qui a �t� d�clench�
		if (e.getSource().equals(app.btnTraitementDesDonnes)) {
			completeData();
		}

		// Si c'est le bouton Coller qui a �t� d�clench�
		if (e.getSource().equals(app.btnPaste)) {
			pasteNames();
		}

		// Si c'est le bouton D�terminer nom qui a �t� d�clench�
		if (e.getSource().equals(app.btnNewName)) {
			makeFilesName();
		}
		// Si c'est le bouton Renommer qui a �t� d�clench�
		if (e.getSource().equals(app.btnRenommer)) {
			rename();
		}
		// Si c'est la checkbox de lecture de saison qui a �t� modifi�e
		if (e.getSource().equals(app.chckbxSeasonReading)) {
			app.spinnerSeasonNum.setEnabled(!app.chckbxSeasonReading.isSelected());
			app.spinnerSeasonNumPos.setEnabled(app.chckbxSeasonReading.isSelected());
			app.spinnerSeasonNumSize.setEnabled(app.chckbxSeasonReading.isSelected());
		}
	}

	private void rechercheSerie() {
		ArrayList<String> listeSerie = new ArrayList();

		ArrayList<ArrayList<String>> testliste = new ArrayList<ArrayList<String>>();

		this.app.framePopUp.setVisible(true);

		tvdb = new TvdbEndpoint(API_KEY, USERNAME, USER_KEY);

		try {
			tvdb.login();

			String NameSerie = app.textShowName.getText().replaceAll(" ", "_");

			series = tvdb.searchByName(NameSerie);
			for (TvdbSerie s : series) {

				listeSerie.add(s.seriesName + "\t         " + s.firstAired);
			}
		} catch (ParseException | IOException | org.json.simple.parser.ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Collections.sort(listeSerie);
		Object[] elements = listeSerie.toArray();

		// TEST TABLE

//		table = new JTable(data, columnNames);
//
//		JScrollPane scrollPane = new JScrollPane();
//		scrollPane.setBounds(10, 10, 900, 393);
//		this.app.framePopUp.getContentPane().add(scrollPane);
//
//		scrollPane.setViewportView(table);
//
//		JTable table2 = new JTable(data2, columnNames2);
//
//		JScrollPane scrollPane2 = new JScrollPane();
//		scrollPane2.setBounds(910, 10, 100, 393);
//		this.app.framePopUp.getContentPane().add(scrollPane2);
//
//		scrollPane2.setViewportView(table2);

		liste = new JList(elements);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 10, 1054, 393);
		this.app.framePopUp.getContentPane().add(scrollPane);

		scrollPane.setViewportView(liste);

		System.out.println(app.textShowName.getText());

		app.btnTraitementDesDonnes.setEnabled(true);
	}

	private void rechercheEpisode() {

		// parse la chaine pour r�cuperer uniquement le nom de la s�rie
		String[] string = liste.getSelectedValue().toString().split("\t");
		NomSerie = string[0];

		for (TvdbSerie s : series) {
			if (NomSerie.equals(s.seriesName)) {

				try {
					episodes = tvdb.getEpisodesList(s.id);
					for (TvdbBasicEpisode tvdbBasicEpisode : episodes.tvdbBasicEpisodes.values()) {
						System.out.println("S" + tvdbBasicEpisode.airedSeason + "E"
								+ tvdbBasicEpisode.airedEpisodeNumber + " : " + tvdbBasicEpisode.episodeName);
					}
				} catch (ParseException | IOException | org.json.simple.parser.ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		}

		app.framePopUp.setVisible(false);
	}

	private void rename() {
		for (FileItem item : app.itemList) {
			File dest = new File(item.file.getParentFile() + "\\" + item.treatedName);
			System.out.println("Renommage : " + item.file.getAbsolutePath() + " ==> " + dest.getAbsolutePath());
			item.file.renameTo(dest);
		}
	}

	private void makeFilesName() {
		ShowInformations showInfos = new ShowInformations();
		showInfos.ShowName = NomSerie;
		if (app.chckbxSeasonReading.isSelected()) {
			showInfos.SeasonNumPos = (int) app.spinnerSeasonNumPos.getValue();
			showInfos.SeasonNumSize = (int) app.spinnerSeasonNumSize.getValue();
		} else {
			showInfos.SeasonNumber = (int) app.spinnerSeasonNum.getValue();
		}
		showInfos.EpisodeNumPos = (int) app.spinnerEpisodeNumPos.getValue();
		showInfos.EpisodeNumSize = (int) app.spinnerEpisodeNumSize.getValue();
		app.itemList.findNewName(showInfos);
		app.btnRenommer.setEnabled(true);
	}

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
				title = title.replaceAll("[/\\\\*?!\"<>|]", ""); // Supprime les caract�res interdits
				title = title.replaceAll(":", "-"); // remplace le : par un -
				title = title.replaceAll("  ", " "); // supprime les doubles espaces laiss�s par les modifications
				title = title.replaceAll(" $", ""); // supprime les espaces en fin de cha�ne
				title = title.replaceAll("^ ", ""); // supprime les espaces en d�but de cha�ne
				app.itemList.setEpisodeName((String) app.table.getValueAt(row++, 1), title);
			}
			app.btnNewName.setEnabled(true);
		}
	}

	private void completeData() {
		ShowInformations showInfos = new ShowInformations();
		showInfos.ShowName = app.textShowName.getText();
		if (app.chckbxSeasonReading.isSelected()) {
			showInfos.SeasonNumPos = (int) app.spinnerSeasonNumPos.getValue();
			showInfos.SeasonNumSize = (int) app.spinnerSeasonNumSize.getValue();
		} else {
			showInfos.SeasonNumber = (int) app.spinnerSeasonNum.getValue();
		}
		showInfos.EpisodeNumPos = (int) app.spinnerEpisodeNumPos.getValue();
		showInfos.EpisodeNumSize = (int) app.spinnerEpisodeNumSize.getValue();
		showInfos.offset = (int) app.spinnerOffset.getValue();
		showInfos.finalLength = (int) app.spinnerFinalLength.getValue();
		app.colorization = false;
		app.itemList.completeData(showInfos, episodes);
		app.btnNewName.setEnabled(true);
	}

	private void scanDirectory() {
		JFileChooser directoryChooser = new JFileChooser();
		directoryChooser.setCurrentDirectory(new java.io.File("Z:\\DL\\Temp"));
		directoryChooser.setDialogTitle("S�lectionnez un dossier");
		directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		directoryChooser.setAcceptAllFileFilterUsed(false);
		//
		if (directoryChooser.showOpenDialog(app.frmGestionSeries) == JFileChooser.APPROVE_OPTION) {
			System.out.println("getSelectedFile() : " + directoryChooser.getSelectedFile());
			app.itemList.clear();

			ArrayList<File> filesList = new ArrayList<File>(
					Arrays.asList(directoryChooser.getSelectedFile().listFiles()));
			for (File file : filesList) {
				app.itemList.add(new FileItem(file));
			}

			app.btnRecherche.setEnabled(true);
			app.colorization = true;
			app.btnTraitementDesDonnes.setEnabled(false);
			app.btnPaste.setEnabled(false);
			app.btnNewName.setEnabled(false);
			app.btnRenommer.setEnabled(false);
		} else {
			System.out.println("No Selection ");
		}

	}
}
