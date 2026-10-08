package ui;

import java.awt.Color;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.ColorFunctions;

/**
 * Couleurs propres à l'application, adaptées au thème courant (clair ou
 * sombre). Les valeurs sont recalculées à chaque appel : elles suivent donc le
 * thème actif.
 */
public final class Theme {

	private Theme() {
	}

	private static boolean isDark() {
		return FlatLaf.isLafDark();
	}

	/* Fonds de la colorisation des sélections de saison / d'épisode / de leur chevauchement */

	public static Color seasonBackground() {
		return isDark() ? new Color(0x31573A) : new Color(0xB6F2B6); // vert
	}

	public static Color episodeBackground() {
		return isDark() ? new Color(0x2F4B73) : new Color(0xB3D1FF); // bleu
	}

	public static Color overlapBackground() {
		return isDark() ? new Color(0x733434) : new Color(0xFFB3B3); // rouge
	}

	/** Texte des lignes décochées (ignorées par les traitements) */
	public static Color ignoredForeground() {
		Color color = UIManager.getColor("Label.disabledForeground");
		return color != null ? color : Color.GRAY;
	}

	/** Texte du statut "Erreur" */
	public static Color errorForeground() {
		return isDark() ? new Color(0xFF6B6B) : new Color(0xC80000);
	}

	/** Fond d'une ligne sur deux, légèrement décalé par rapport au fond du tableau */
	public static Color stripeBackground(Color tableBackground) {
		return isDark() ? ColorFunctions.lighten(tableBackground, 0.04f)
				: ColorFunctions.darken(tableBackground, 0.03f);
	}

	/** Trait de séparation (barre d'état) */
	public static Color separator() {
		Color color = UIManager.getColor("Separator.foreground");
		return color != null ? color : Color.LIGHT_GRAY;
	}

	/** Couleur au format HTML (#rrggbb) */
	public static String toHtml(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
}
