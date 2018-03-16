package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class MasterMavenRepository {

	private static MasterMavenRepository instance;
	private static File repositoriesDir;

	private MasterMavenRepository() {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			repositoriesDir = new File(jenkins.getRootDir(), "shared-maven-repository");
			if (!repositoriesDir.exists()) {
				boolean created = repositoriesDir.mkdir();
				if (!created) {
					throw new RuntimeException("Unable to delete master repository folder");
				}
			}
		}
	}

	public synchronized static MasterMavenRepository getInstance() {
		if (instance == null) {
			instance = new MasterMavenRepository();
		}
		return instance;
	}

	public void uploadRepository(FilePath repositoryTar, TaskListener listener, Label label)
			throws IOException, InterruptedException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			listener.getLogger().println("Upload " + repositoryTar.absolutize().toURI() + " to Jenkins master");
			FilePath masterRepo = new FilePath(
					new File(new File(repositoriesDir, label.getId()), repositoryTar.getName()));
			repositoryTar.copyTo(masterRepo);
			listener.getLogger().println("Repository uploaded to " + masterRepo.absolutize().toURI());
			deleteOldRepositories(listener, label);
		}
	}

	private void deleteOldRepositories(TaskListener listener, Label label) throws IOException, InterruptedException {
		listener.getLogger().println("Delete old repositories from master");
		File[] repositories = new File(repositoriesDir, label.getId()).listFiles();
		if (repositories != null) {
			for (File repo : repositories) {
				String repoName = repo.getName();
				if (!repoName.equals(label.getLatestRepoFile().getName())) {
					boolean deleted = repo.delete();
					if (!deleted) {
						listener.getLogger().println("Unable to delete old repository " + repoName);
					} else {
						listener.getLogger().println("Deleted old repository from master " + repoName);
					}
				}
			}
		}
	}

	public static FilePath getLatestRepo(Label label) {
		if(repositoriesDir == null)
			getInstance();
		File repositoriesLabelDir = new File(repositoriesDir, label.getId());
		File lastModifiedFile = lastFileModified(repositoriesLabelDir);
		if (lastModifiedFile == null) {
			return null;
		}
		return new FilePath(lastModifiedFile);
	}

	private static File lastFileModified(File dir) {
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.isFile();
			}
		});
		long lastMod = Long.MIN_VALUE;
		File choice = null;
		if (files != null) {
			for (File file : files) {
				if (file.lastModified() > lastMod) {
					choice = file;
					lastMod = file.lastModified();
				}
			}
		}
		return choice;
	}

}
