package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import api.DataSourceType;

/**
 * Fenêtre des préférences de l'application. Chaque groupe de réglages a son
 * cadre ; les valeurs sont lues dans les préférences à l'ouverture et n'y sont
 * enregistrées qu'à la validation.
 */
class SettingsDialog extends JDialog {

	/** serialUID */
	private static final long serialVersionUID = 1L;

	private final JComboBox<DataSourceType> comboDataSource = new JComboBox<>(DataSourceType.values());
	private final JTextField textScanDirectory = new JTextField(30);
	private boolean validated;

	SettingsDialog(JFrame owner) {
		super(owner, "Préférences", true);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		JPanel contentPane = new JPanel(new BorderLayout(0, 8));
		contentPane.setBorder(new EmptyBorder(8, 8, 8, 8));
		setContentPane(contentPane);

		JPanel settingsPanel = new JPanel(new GridBagLayout());
		contentPane.add(settingsPanel, BorderLayout.CENTER);

		// Source de données
		JPanel sourcePanel = createGroupPanel("Source de données");
		sourcePanel.add(new JLabel("Séries et épisodes lus sur"), gbc(0, 0));
		GridBagConstraints comboConstraints = gbc(1, 0);
		comboConstraints.weightx = 1.0; // contenu du cadre aligné à gauche
		sourcePanel.add(comboDataSource, comboConstraints);
		addNote(sourcePanel, "<html>Changer de source annule la série choisie dans chaque onglet.<br>"
				+ "La clé API de la source doit être renseignée dans gestionseries.properties.</html>", 1, 2);
		addGroup(settingsPanel, sourcePanel, 0);

		// Fichiers
		JPanel filesPanel = createGroupPanel("Fichiers");
		filesPanel.add(new JLabel("Dossier proposé par « Scanner Dossier »"), gbc(0, 0));
		GridBagConstraints textConstraints = gbc(0, 1);
		textConstraints.fill = GridBagConstraints.HORIZONTAL;
		textConstraints.weightx = 1.0;
		filesPanel.add(textScanDirectory, textConstraints);
		JButton btnBrowse = new JButton("Parcourir…");
		btnBrowse.addActionListener(e -> chooseScanDirectory());
		filesPanel.add(btnBrowse, gbc(1, 1));
		addNote(filesPanel, "Laisser vide pour partir du dossier par défaut du système.", 2, 2);
		addGroup(settingsPanel, filesPanel, 1);

		// Boutons
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		JButton btnOk = new JButton("OK");
		btnOk.addActionListener(e -> validateAndClose());
		JButton btnCancel = new JButton("Annuler");
		btnCancel.addActionListener(e -> setVisible(false));
		buttonsPanel.add(btnOk);
		buttonsPanel.add(btnCancel);
		contentPane.add(buttonsPanel, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(btnOk);

		pack();
		setResizable(false);
	}

	/**
	 * Ouvre la fenêtre avec les réglages enregistrés ; bloquant jusqu'à sa
	 * fermeture.
	 *
	 * @return vrai si l'utilisateur a validé : les nouveaux réglages sont alors
	 *         enregistrés et à relire dans Settings
	 */
	boolean edit() {
		comboDataSource.setSelectedItem(Settings.getDataSource());
		File scanDirectory = Settings.getScanDirectory();
		textScanDirectory.setText(scanDirectory != null ? scanDirectory.getPath() : "");
		validated = false;
		setLocationRelativeTo(getOwner());
		setVisible(true);
		return validated;
	}

	// Enregistre les réglages s'ils sont valides ; sinon explique le problème et laisse la fenêtre ouverte
	private void validateAndClose() {
		String path = textScanDirectory.getText().trim();
		File scanDirectory = path.isEmpty() ? null : new File(path);
		if (scanDirectory != null && !scanDirectory.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Le dossier « " + path + " » n'existe pas.", "Préférences",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		Settings.setDataSource((DataSourceType) comboDataSource.getSelectedItem());
		Settings.setScanDirectory(scanDirectory);
		validated = true;
		setVisible(false);
	}

	private void chooseScanDirectory() {
		JFileChooser chooser = new JFileChooser();
		String path = textScanDirectory.getText().trim();
		if (!path.isEmpty()) {
			chooser.setCurrentDirectory(new File(path));
		}
		chooser.setDialogTitle("Dossier proposé par « Scanner Dossier »");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			textScanDirectory.setText(chooser.getSelectedFile().getPath());
		}
	}

	/* Utilitaires de mise en page */

	private static JPanel createGroupPanel(String title) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(new TitledBorder(title));
		return panel;
	}

	// Ajoute un cadre sous les précédents, sur toute la largeur
	private static void addGroup(JPanel settingsPanel, JPanel group, int row) {
		GridBagConstraints c = gbc(0, row);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.insets = new Insets(row == 0 ? 0 : 6, 0, 0, 0);
		settingsPanel.add(group, c);
	}

	// Explication discrète sous les réglages d'un cadre
	private static void addNote(JPanel panel, String text, int row, int width) {
		JLabel note = new JLabel(text);
		note.setForeground(Theme.ignoredForeground());
		GridBagConstraints c = gbc(0, row);
		c.gridwidth = width;
		panel.add(note, c);
	}

	private static GridBagConstraints gbc(int x, int y) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 4, 2, 4);
		return c;
	}
}
