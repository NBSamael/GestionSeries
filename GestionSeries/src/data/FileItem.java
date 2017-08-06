package data;

import java.io.File;

public class FileItem {
	public File file;
	public String originalName;
	public String season;
	public String episode;
	public String episodeName;
	public String treatedName;

	public FileItem(File file) {
		this.file = file;
		this.originalName = file.getName();
	}

}
