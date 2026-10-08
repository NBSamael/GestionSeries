package data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture des CSV exportés de Netflix par l'extension NetflixEpisodesExport :
 * UTF-8 avec BOM, séparateur ";", champs entre guillemets s'ils contiennent un
 * séparateur, un guillemet (doublé) ou un saut de ligne.
 */
public final class NetflixCsvReader {

	private static final char SEPARATOR = ';';
	private static final char QUOTE = '"';
	private static final char BOM = '﻿';

	/** Colonnes attendues, retrouvées par leur en-tête quelle que soit leur position */
	private enum Column {
		SEASON("Saison"), NUMBER("Épisode"), TITLE("Titre"), SYNOPSIS("Résumé");

		private final String header;

		Column(String header) {
			this.header = header;
		}
	}

	/** Fichier qui n'a pas le format attendu ; le message est destiné à l'utilisateur */
	public static class FormatException extends IOException {

		/** serialUID */
		private static final long serialVersionUID = 1L;

		public FormatException(String message) {
			super(message);
		}
	}

	private NetflixCsvReader() {
	}

	public static List<NetflixEpisode> read(File file) throws IOException {
		String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		if (!content.isEmpty() && content.charAt(0) == BOM) {
			content = content.substring(1);
		}
		List<List<String>> records = parseRecords(content);
		if (records.isEmpty()) {
			throw new FormatException("Le fichier est vide.");
		}

		Map<Column, Integer> positions = findColumns(records.get(0));
		List<NetflixEpisode> episodes = new ArrayList<>();
		for (int i = 1; i < records.size(); i++) {
			List<String> record = records.get(i);
			if (isBlank(record)) {
				continue;
			}
			// Numéro de l'enregistrement dans le fichier, en-tête compris (≠ numéro de ligne si un résumé est sur plusieurs lignes)
			int recordNumber = i + 1;
			episodes.add(new NetflixEpisode(
					parseNumber(field(record, positions, Column.SEASON), Column.SEASON, recordNumber),
					parseNumber(field(record, positions, Column.NUMBER), Column.NUMBER, recordNumber),
					field(record, positions, Column.TITLE),
					field(record, positions, Column.SYNOPSIS)));
		}
		return episodes;
	}

	// Découpe le contenu en enregistrements ; les sauts de ligne entre guillemets font partie du champ
	private static List<List<String>> parseRecords(String content) throws FormatException {
		List<List<String>> records = new ArrayList<>();
		List<String> record = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		int length = content.length();

		for (int i = 0; i < length; i++) {
			char c = content.charAt(i);
			if (quoted) {
				if (c == QUOTE) {
					if (i + 1 < length && content.charAt(i + 1) == QUOTE) {
						field.append(QUOTE); // guillemet doublé
						i++;
					} else {
						quoted = false;
					}
				} else {
					field.append(c);
				}
			} else if (c == QUOTE) {
				quoted = true;
			} else if (c == SEPARATOR) {
				record.add(field.toString());
				field.setLength(0);
			} else if (c == '\r' || c == '\n') {
				if (c == '\r' && i + 1 < length && content.charAt(i + 1) == '\n') {
					i++;
				}
				record.add(field.toString());
				field.setLength(0);
				records.add(record);
				record = new ArrayList<>();
			} else {
				field.append(c);
			}
		}
		if (quoted) {
			throw new FormatException("Guillemet non refermé : le fichier semble tronqué.");
		}
		// Dernier enregistrement sans fin de ligne
		if (field.length() > 0 || !record.isEmpty()) {
			record.add(field.toString());
			records.add(record);
		}
		return records;
	}

	private static Map<Column, Integer> findColumns(List<String> headers) throws FormatException {
		Map<Column, Integer> positions = new EnumMap<>(Column.class);
		for (Column column : Column.values()) {
			for (int i = 0; i < headers.size(); i++) {
				if (headers.get(i).trim().equalsIgnoreCase(column.header)) {
					positions.put(column, i);
				}
			}
			if (!positions.containsKey(column)) {
				throw new FormatException("Colonne « " + column.header + " » absente de l'en-tête.\n"
						+ "En-tête attendu : Saison;Épisode;Titre;Résumé");
			}
		}
		return positions;
	}

	private static String field(List<String> record, Map<Column, Integer> positions, Column column) {
		int position = positions.get(column);
		return position < record.size() ? record.get(position).trim() : "";
	}

	private static int parseNumber(String value, Column column, int recordNumber) throws FormatException {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new FormatException("Enregistrement " + recordNumber + " : « " + value
					+ " » n'est pas un numéro valide dans la colonne " + column.header + ".");
		}
	}

	private static boolean isBlank(List<String> record) {
		for (String value : record) {
			if (!value.trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
