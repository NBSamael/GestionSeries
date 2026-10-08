package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import api.TvdbBasicEpisode;
import api.TvdbSeriesEpisodes;
import ui.Application;
import ui.FileListTableModel;

public class FileItemList extends ArrayList<FileItem> {

	/** serialUID */
	private static final long serialVersionUID = 1L;

	FileListTableModel tableModel;

	public FileItemList(Application app) {
		super();
		tableModel = new FileListTableModel(this, app);
	}

	public FileListTableModel getTableModel() {
		return tableModel;
	}

	@Override
	public boolean add(FileItem e) {
		boolean valueReturned = super.add(e);
		if (valueReturned)
			tableModel.fireTableRowsInserted(size() - 1, size() - 1);
		return valueReturned;
	}

	public void setEpisodeName(String originalName, String episodeName) {
		for (int row = 0; row < size(); row++) {
			if (get(row).originalName.equals(originalName)) {
				get(row).episodeName = episodeName;
				tableModel.fireTableCellUpdated(row, FileListTableModel.Columns.EPISODE_NAME.getPosition());
				return;
			}
		}
	}

	/**
	 * Extrait la partie du nom de fichier correspondant à un numéro (saison ou
	 * épisode), en vérifiant qu'elle existe et qu'elle ne contient que des
	 * chiffres.
	 *
	 * @throws IllegalArgumentException si la sélection est invalide
	 */
	private static String extractNumber(String name, int position, int size, String label, String source) {
		if (position < 0 || size <= 0 || position + size > name.length()) {
			throw new IllegalArgumentException("la sélection du numéro " + label + " (position " + position
					+ ", taille " + size + ") dépasse du nom du " + source + " (" + name.length() + " caractères)");
		}
		String number = name.substring(position, position + size);
		if (!number.matches("\\d+")) {
			throw new IllegalArgumentException(
					"la sélection du numéro " + label + " (\"" + number + "\") n'est pas un nombre");
		}
		return number;
	}

	/** Longueur du numéro d'épisode quand aucune longueur finale valide n'est indiquée */
	private static final int DEFAULT_EPISODE_LENGTH = 2;

	/**
	 * Met en forme le numéro d'épisode sur la longueur finale demandée : des zéros
	 * sont ajoutés à gauche si le numéro est plus court, et les zéros inutiles à
	 * gauche disparaissent s'il était plus long (ex. "0012" lu avec une longueur
	 * finale de 2 donne "12"). Les chiffres significatifs ne sont jamais retirés.
	 */
	private static String formatEpisodeNumber(int episodeNumber, int finalLength) {
		int length = finalLength > 0 ? finalLength : DEFAULT_EPISODE_LENGTH;
		return String.format("%0" + length + "d", episodeNumber);
	}

	/**
	 * Complète les fichiers cochés avec les informations de la série.
	 *
	 * @return la liste des erreurs rencontrées (vide si tout s'est bien passé) ;
	 *         les fichiers en erreur passent au statut ERREUR
	 */
	public List<String> completeData(ShowInformations showInfos, TvdbSeriesEpisodes episodes) {
		List<String> errors = new ArrayList<>();
		for (FileItem fileItem : this) {
			if (!fileItem.selected) {
				continue;
			}
			String originalName = fileItem.originalName;

			String season;
			String episode;
			try {
				switch (showInfos.seasonReading) {
				case FILE_NAME:
					season = extractNumber(originalName, showInfos.SeasonNumPos, showInfos.SeasonNumSize,
							"de saison", "fichier");
					break;
				case FOLDER_NAME:
					season = extractNumber(fileItem.folder, showInfos.SeasonNumPos, showInfos.SeasonNumSize,
							"de saison", "dossier");
					break;
				case NONE:
				default:
					season = Integer.toString(showInfos.SeasonNumber);
					System.out.println(showInfos.SeasonNumber);
					break;
				}
				episode = extractNumber(originalName, showInfos.EpisodeNumPos, showInfos.EpisodeNumSize,
						"d'épisode", "fichier");
			} catch (IllegalArgumentException e) {
				fileItem.status = FileItem.Status.ERREUR;
				fileItem.errorMessage = e.getMessage();
				fileItem.season = null;
				fileItem.episode = null;
				fileItem.episodeName = null;
				fileItem.treatedName = null;
				errors.add(originalName + " : " + e.getMessage());
				continue;
			}
			fileItem.status = FileItem.Status.OK;
			fileItem.errorMessage = null;

			// Les numéros sont comparés à ceux de TVDB en tant que nombres,
			// indépendamment de leur mise en forme
			int seasonNumber = Integer.parseInt(season);
			int episodeNumber = Integer.parseInt(episode) + showInfos.offset;

			if (season.length() < 2) {
				season = "0" + season;
			}
			episode = formatEpisodeNumber(episodeNumber, showInfos.finalLength);

			fileItem.season = season;
			fileItem.episode = episode;
			System.out.println("Saison : " + season + " Episode : " + episode);

			for (TvdbBasicEpisode tvdbBasicEpisode : episodes.tvdbBasicEpisodes.values()) {
				// Episodes sans numéro dans TVDB (épisodes spéciaux mal renseignés, etc.)
				if (tvdbBasicEpisode.airedSeason == null || tvdbBasicEpisode.airedEpisodeNumber == null) {
					continue;
				}
				System.out.println("  Saison : " + tvdbBasicEpisode.airedSeason + " Episode : "
						+ tvdbBasicEpisode.airedEpisodeNumber);
				if (tvdbBasicEpisode.airedSeason == seasonNumber
						&& tvdbBasicEpisode.airedEpisodeNumber == episodeNumber) {
					System.out.println("Nom épisode : " + tvdbBasicEpisode.episodeName);
					fileItem.episodeName = tvdbBasicEpisode.episodeName;
				}
			}
		}

		tableModel.fireTableDataChanged();
		return errors;
	}

	public void findNewName(ShowInformations showInfos) {
		for (FileItem fileItem : this) {
			// Les fichiers décochés ou en erreur lors du traitement des données sont ignorés
			if (!fileItem.selected || fileItem.status == FileItem.Status.ERREUR) {
				continue;
			}

			String originalName = fileItem.originalName;

			List<String> namePieces = Arrays.asList(originalName.split("\\."));
			String extension = namePieces.get(namePieces.size() - 1);

			StringBuilder treatedName = new StringBuilder();
			treatedName.append(showInfos.ShowName).append(" - S");
			treatedName.append(fileItem.season).append("E");
			treatedName.append(fileItem.episode);
			treatedName.append(" - ");
			treatedName.append(fileItem.episodeName);
			treatedName.append(".").append(extension);
			fileItem.treatedName = treatedName.toString();
			fileItem.treatedName = fileItem.treatedName.replaceAll("[/\\\\*?!\"<>|]", ""); // Supprime les caractères
																							// interdits
			fileItem.treatedName = fileItem.treatedName.replaceAll(":", "-"); // remplace le : par un -
			fileItem.treatedName = fileItem.treatedName.replaceAll("  ", " "); // supprime les doubles espaces laissés
																				// par les modifications
			fileItem.treatedName = fileItem.treatedName.replaceAll(" $", ""); // supprime les espaces en fin de chaîne
			fileItem.treatedName = fileItem.treatedName.replaceAll("^ ", ""); // supprime les espaces en début de chaîne
		}
		tableModel.fireTableDataChanged();
	}

	@Override
	public void clear() {
		super.clear();
		tableModel.fireTableDataChanged();
	}

	public void refresh() {
		tableModel.fireTableDataChanged();
	}
}
