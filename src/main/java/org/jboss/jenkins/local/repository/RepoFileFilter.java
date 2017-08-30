package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class RepoFileFilter implements FileFilter, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<String, File> highestVersions = new HashMap<>();

	@Override
	public boolean accept(File pathname) {
		Path pPath = pathname.toPath().getParent();
		if (pPath != null) {
			Path parentPath = pPath.getParent();
			if (parentPath != null && highestVersions.containsKey(parentPath.toString())) {
				return highestVersions.get(parentPath.toString()).equals(pathname.getParentFile());
			}
		}
		return true;
	}
	
	public void preparePath(File repositoryFolder) {
		List<File> allFiles = new ArrayList<>();
		listFiles(repositoryFolder.toPath(), allFiles);
		for (File file : allFiles) {
			if (file.isDirectory()) {
				// check if starts with digit - ie version 2.6.2.v20161117-2150
				if (Character.isDigit(file.getName().charAt(0))) {
					// check if p2 is in path
					String[] pathParts = file.getAbsolutePath()
							.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
					boolean found = false;
					for (String part : pathParts) {
						if (part.equals("p2")) {
							found = true;
							break;
						}
					}
					if (found && !wasSearched(file)) {
						File latestFile = getLatestFile(file);
						highestVersions.put(latestFile.getParentFile().toPath().toString(), latestFile);
					}
				}
			}

		}
	}

	private boolean wasSearched(File file) {
		Path filePath = file.toPath().getParent();
		if (filePath != null) {
			for (String s : highestVersions.keySet()) {
				if (s.startsWith(filePath.toString())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void listFiles(Path path, List<File> allFiles) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					listFiles(entry, allFiles);
				}
				allFiles.add(entry.toFile());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private File getLatestFile(File pathname) {
		File highestVersionFile = pathname;
		File[] otherVersions = pathname.getParentFile().listFiles();
		if (otherVersions != null && otherVersions.length > 1) {
			for (File f : otherVersions) {
				if (f.equals(pathname)) {
					continue;
				}

				DefaultArtifactVersion otherVersion = getMainVersion(f.getName());
				DefaultArtifactVersion highVersion = getMainVersion(highestVersionFile.getName());
				int compResult = otherVersion.compareTo(highVersion);

				if (compResult > 0) {
					// found new high version
					highestVersionFile = f;
				} else if (compResult == 0) {
					// check part after 'v'
					long otherSecVersion = getSecondaryVersion(f.getName());
					long highSecVersion = getSecondaryVersion(highestVersionFile.getName());
					if (highSecVersion - otherSecVersion < 0) {
						highestVersionFile = f;
					} else if (highSecVersion - otherSecVersion == 0) {
						// check part after '-'
						long otherTercVersion = getTerciaryVersion(f.getName());
						long highTercVersion = getTerciaryVersion(highestVersionFile.getName());
						if (highTercVersion - otherTercVersion < 0) {
							highestVersionFile = f;
						}
					}
				}

			}
		}
		return highestVersionFile;
	}
	
	private DefaultArtifactVersion getMainVersion(String name) {
		String[] splitName = name.split("v");
		return new DefaultArtifactVersion(splitName[0]);
	}

	private long getSecondaryVersion(String name) {
		String[] splitName = name.split("v");
		if (splitName.length == 2) {
			String[] anotherSplit = splitName[1].split("-");
			return Long.parseLong(anotherSplit[0]);
		}
		return 0;
	}

	private long getTerciaryVersion(String name) {
		String[] splitName = name.split("-");
		if (splitName.length == 2) {
			return Long.parseLong(splitName[1]);
		}
		return 0;
	}

}