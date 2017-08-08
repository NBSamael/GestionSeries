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
import java.util.List;

import javax.swing.JFileChooser;

import data.FileItem;
import data.ShowInformations;

public class ButtonListener implements java.awt.event.ActionListener {

	Application app;

	public ButtonListener(Application app) {
		super();
		this.app = app;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Si c'est le bouton de scan de dossier qui a été déclenché

		if (e.getSource().equals(app.btnScannerDossier)) {
			JFileChooser directoryChooser = new JFileChooser();
			directoryChooser.setCurrentDirectory(new java.io.File("Z:\\DL\\Temp"));
			directoryChooser.setDialogTitle("Sélectionnez un dossier");
			directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			directoryChooser.setAcceptAllFileFilterUsed(false);
			//
			if (directoryChooser.showOpenDialog(app.frmGestionSeries) == JFileChooser.APPROVE_OPTION) {
				System.out.println("getSelectedFile() : " + directoryChooser.getSelectedFile());
				app.itemList.clear();
				scanDirectory(directoryChooser.getSelectedFile());
				app.btnTraitementDesDonnes.setEnabled(true);
				app.btnPaste.setEnabled(false);
				app.btnNewName.setEnabled(false);
				app.btnRenommer.setEnabled(false);
			} else {
				System.out.println("No Selection ");
			}
		}

		// Si c'est le bouton de traitement des données qui a été déclenché
		if (e.getSource().equals(app.btnTraitementDesDonnes)) {
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
			app.itemList.completeData(showInfos);
			app.btnPaste.setEnabled(true);
		}

		// Si c'est le bouton Coller qui a été déclenché
		if (e.getSource().equals(app.btnPaste)) {
			System.out.println("Ligne : " + app.table.getSelectedRows()[0]);
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
					app.itemList.setEpisodeName((String) app.table.getValueAt(row++, 1), title);
				}
				app.btnNewName.setEnabled(true);
			}
		}

		// Si c'est le bouton Déterminer nom qui a été déclenché
		if (e.getSource().equals(app.btnNewName)) {
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
			app.itemList.findNewName(showInfos);
			app.btnRenommer.setEnabled(true);
		}
		// Si c'est le bouton Renommer qui a été déclenché
		if (e.getSource().equals(app.btnRenommer)) {
			for (FileItem item : app.itemList) {
				File dest = new File(item.file.getParentFile() + "\\" + item.treatedName);
				System.out.println("Renommage : " + item.file.getAbsolutePath() + " ==> " + dest.getAbsolutePath());
				item.file.renameTo(dest);
			}
		}
		// Si c'est la checkbox de lecture de saison qui a été modifiée
		if (e.getSource().equals(app.chckbxSeasonReading)) {
			app.spinnerSeasonNum.setEnabled(!app.chckbxSeasonReading.isSelected());
			app.spinnerSeasonNumPos.setEnabled(app.chckbxSeasonReading.isSelected());
			app.spinnerSeasonNumSize.setEnabled(app.chckbxSeasonReading.isSelected());
		}
	}

	private void scanDirectory(File directory) {
		ArrayList<File> filesList = new ArrayList<File>(Arrays.asList(directory.listFiles()));
		for (File file : filesList) {
			app.itemList.add(new FileItem(file));
		}
	}
}
