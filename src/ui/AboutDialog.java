package ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;

/**
 * Fenêtre « À propos » : présentation, licence et crédits des sources de
 * données (mentions exigées par TheTVDB et TMDB pour l'utilisation de leurs
 * API).
 */
class AboutDialog extends JDialog {

	/** serialUID */
	private static final long serialVersionUID = 1L;

	/* Logo officiel de TMDB (273 x 36 pixels), affiché réduit de moitié */
	private static final URL TMDB_LOGO = AboutDialog.class.getResource("tmdb-logo.png");

	private static final String CONTENT = "<html><body>"
			+ "<h2>GestionSeries</h2>"
			+ "<p>Renommage d'épisodes de séries TV et comparaison des épisodes Netflix avec la source de données.</p>"
			+ "<p>Auteurs : NBSamael, Francis Bellanger, Elise<br>"
			+ "Distribué sous licence <a href='https://polyformproject.org/licenses/noncommercial/1.0.0/'>"
			+ "PolyForm Noncommercial 1.0.0</a> (usage non commercial).</p>"
			+ "<h3>Sources de données</h3>"
			+ "<p><b>TheTVDB</b> : informations sur les séries et les épisodes fournies par "
			+ "<a href='https://thetvdb.com'>TheTVDB</a>, selon les "
			+ "<a href='https://thetvdb.com/api-information'>conditions de son API</a>.</p>"
			+ (TMDB_LOGO != null
					? "<p><a href='https://www.themoviedb.org'><img src='" + TMDB_LOGO
							+ "' width='136' height='18' border='0' alt='TMDB'></a></p>"
					: "")
			+ "<p><b>TMDB</b> : ce produit utilise l'API TMDB mais n'est ni approuvé ni certifié par TMDB.<br>"
			+ "<i>This product uses the TMDB API but is not endorsed or certified by TMDB.</i><br>"
			+ "Informations fournies par <a href='https://www.themoviedb.org'>The Movie Database (TMDB)</a>.</p>"
			+ "<p>La source utilisée se choisit dans Paramètres &gt; Préférences.</p>"
			+ "</body></html>";

	AboutDialog(JFrame owner) {
		super(owner, "À propos de GestionSeries", true);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		JPanel contentPane = new JPanel(new BorderLayout(0, 8));
		contentPane.setBorder(new EmptyBorder(8, 8, 8, 8));
		setContentPane(contentPane);

		// Texte HTML aux couleurs du thème, liens ouverts dans le navigateur
		JEditorPane text = new JEditorPane();
		text.setEditorKit(new SynchronousImagesKit());
		text.setText(CONTENT);
		text.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		text.setFont(UIManager.getFont("Label.font"));
		text.setEditable(false);
		text.setOpaque(false);
		text.addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
				try {
					Desktop.getDesktop().browse(e.getURL().toURI());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(text);
		scrollPane.setBorder(null);
		scrollPane.setPreferredSize(new Dimension(520, 360));
		contentPane.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		JButton btnClose = new JButton("Fermer");
		btnClose.addActionListener(e -> setVisible(false));
		buttonsPanel.add(btnClose);
		contentPane.add(buttonsPanel, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(btnClose);

		pack();
	}

	/**
	 * HTML dont les images sont chargées avant l'affichage : par défaut, Swing les
	 * charge en arrière-plan et affiche d'abord une icône de remplacement.
	 */
	private static class SynchronousImagesKit extends HTMLEditorKit {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		@Override
		public ViewFactory getViewFactory() {
			return new HTMLFactory() {
				@Override
				public View create(Element element) {
					View view = super.create(element);
					if (view instanceof ImageView) {
						((ImageView) view).setLoadsSynchronously(true);
					}
					return view;
				}
			};
		}
	}

	/** Ouvre la fenêtre, centrée sur la fenêtre principale ; bloquant jusqu'à sa fermeture */
	void open() {
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}
}
