package ui;

import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import data.FileItemList;

public class Application {

	public JFrame frmGestionSeries;
	public JTable table;
	public FileItemList itemList;
	public JTextField textShowName;
	public JButton btnScannerDossier;
	public JSpinner spinnerSeasonNumPos;
	public JSpinner spinnerSeasonNumSize;
	public JSpinner spinnerEpisodeNumPos;
	public JSpinner spinnerEpisodeNumSize;
	public JButton btnTraitementDesDonnes;
	public JButton btnPaste;
	public JButton btnRenommer;
	public JCheckBox chckbxSeasonReading;
	public JSpinner spinnerSeasonNum;
	public JButton btnNewName;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Throwable e) {
			e.printStackTrace();
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
	 * Create the application.
	 */
	public Application() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		ButtonListener buttonListener = new ButtonListener(this);

		frmGestionSeries = new JFrame();
		frmGestionSeries.setResizable(false);
		frmGestionSeries.setTitle("Gestion S\u00E9ries");
		frmGestionSeries.setBounds(100, 100, 1080, 600);
		frmGestionSeries.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGestionSeries.getContentPane().setLayout(null);

		btnScannerDossier = new JButton("Scanner Dossier");
		btnScannerDossier.addActionListener(buttonListener);
		btnScannerDossier.setBounds(270, 120, 150, 25);
		frmGestionSeries.getContentPane().add(btnScannerDossier);

		itemList = new FileItemList();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 154, 1054, 393);
		frmGestionSeries.getContentPane().add(scrollPane);

		table = new JTable(itemList.getTableModel());
		itemList.getTableModel().setTable(table);
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		table.setCellSelectionEnabled(true);
		table.setAutoCreateRowSorter(true);
		scrollPane.setViewportView(table);

		spinnerSeasonNumPos = new JSpinner();
		spinnerSeasonNumPos.setBounds(410, 37, 43, 20);
		frmGestionSeries.getContentPane().add(spinnerSeasonNumPos);
		spinnerSeasonNumPos.setValue(3);

		JLabel lblSeasonNumPos = new JLabel("Position Num\u00E9ro Saison");
		lblSeasonNumPos.setBounds(268, 40, 132, 14);
		frmGestionSeries.getContentPane().add(lblSeasonNumPos);

		JLabel lblSeasonNumSize = new JLabel("Taille Num\u00E9ro Saison");
		lblSeasonNumSize.setBounds(268, 68, 132, 14);
		frmGestionSeries.getContentPane().add(lblSeasonNumSize);

		spinnerSeasonNumSize = new JSpinner();
		spinnerSeasonNumSize.setBounds(410, 65, 43, 20);
		frmGestionSeries.getContentPane().add(spinnerSeasonNumSize);
		spinnerSeasonNumSize.setValue(1);

		JLabel lblEpisodeNumPos = new JLabel("Position Num\u00E9ro Episode");
		lblEpisodeNumPos.setBounds(494, 40, 132, 14);
		frmGestionSeries.getContentPane().add(lblEpisodeNumPos);

		spinnerEpisodeNumPos = new JSpinner();
		spinnerEpisodeNumPos.setBounds(636, 37, 43, 20);
		frmGestionSeries.getContentPane().add(spinnerEpisodeNumPos);
		spinnerEpisodeNumPos.setValue(5);

		JLabel lblEpisodeNumSire = new JLabel("Taille Num\u00E9ro Episode");
		lblEpisodeNumSire.setBounds(494, 68, 132, 14);
		frmGestionSeries.getContentPane().add(lblEpisodeNumSire);

		spinnerEpisodeNumSize = new JSpinner();
		spinnerEpisodeNumSize.setBounds(636, 65, 43, 20);
		frmGestionSeries.getContentPane().add(spinnerEpisodeNumSize);
		spinnerEpisodeNumSize.setValue(2);

		btnTraitementDesDonnes = new JButton("Traitement des donn\u00E9es");
		btnTraitementDesDonnes.setEnabled(false);
		btnTraitementDesDonnes.addActionListener(buttonListener);
		btnTraitementDesDonnes.setBounds(430, 120, 150, 25);
		frmGestionSeries.getContentPane().add(btnTraitementDesDonnes);

		textShowName = new JTextField();
		textShowName.setBounds(768, 37, 149, 20);
		frmGestionSeries.getContentPane().add(textShowName);
		textShowName.setColumns(10);

		JLabel lblShowName = new JLabel("Nom S\u00E9rie");
		lblShowName.setBounds(705, 40, 53, 14);
		frmGestionSeries.getContentPane().add(lblShowName);

		btnPaste = new JButton("Coller");
		btnPaste.setEnabled(false);
		btnPaste.addActionListener(buttonListener);
		btnPaste.setBounds(590, 120, 150, 25);
		frmGestionSeries.getContentPane().add(btnPaste);

		btnRenommer = new JButton("Renommer");
		btnRenommer.setEnabled(false);
		btnRenommer.addActionListener(buttonListener);
		btnRenommer.setBounds(910, 120, 150, 25);
		frmGestionSeries.getContentPane().add(btnRenommer);

		chckbxSeasonReading = new JCheckBox("Lecture Saison");
		chckbxSeasonReading.setSelected(true);
		chckbxSeasonReading.addActionListener(buttonListener);
		chckbxSeasonReading.setBounds(146, 36, 97, 23);
		frmGestionSeries.getContentPane().add(chckbxSeasonReading);

		spinnerSeasonNum = new JSpinner();
		spinnerSeasonNum.setEnabled(false);
		spinnerSeasonNum.setBounds(214, 65, 29, 20);
		frmGestionSeries.getContentPane().add(spinnerSeasonNum);

		JLabel lblNumroSaison = new JLabel("Num\u00E9ro Saison");
		lblNumroSaison.setBounds(131, 68, 71, 14);
		frmGestionSeries.getContentPane().add(lblNumroSaison);

		btnNewName = new JButton("D\u00E9terminer nom");
		btnNewName.setEnabled(false);
		btnNewName.addActionListener(buttonListener);
		btnNewName.setBounds(750, 120, 150, 25);
		frmGestionSeries.getContentPane().add(btnNewName);
	}
}
