package data;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileItem {

	public enum Status {
		OK("OK"), ERREUR("Erreur");

		private String name = "";

		Status(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public static Status getStatusByName(String statusSearched) {
			List<Status> statusList = Arrays.asList(Status.values());
			for (Status status : statusList) {
				if (status.name.equals(statusSearched)) {
					return status;
				}
			}
			return null;
		}
	}

	public boolean selected;
	public File file;
	public String originalName;
	public Status status;
	public String season;
	public String episode;
	public String episodeName;
	public String treatedName;

	public FileItem(File file) {
		this.file = file;
		this.originalName = file.getName();
		this.selected = true;
		this.status = Status.OK;
	}

}
