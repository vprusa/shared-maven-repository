package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;

import org.acegisecurity.AccessDeniedException;

import hudson.FilePath;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.search.Search;
import hudson.search.SearchIndex;
import hudson.security.ACL;
import hudson.security.Permission;
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

	public void uploadRepository(FilePath repositoryTar, FilePath workspace, TaskListener listener, Label label)
			throws IOException, InterruptedException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			listener.getLogger().println("Upload " + repositoryTar.absolutize().toURI() + " to Jenkins master");

			String path;

			if (label.getArchivePath() == null) {
				path = repositoriesDir + "/" + label.getId();
			} else {
				// TODO handle more
				path = label.getArchivePath();
			}

			FilePath masterRepo = new FilePath(new File(new File(path), repositoryTar.getName()));
			repositoryTar.copyTo(masterRepo);
			listener.getLogger().println("Repository uploaded to " + masterRepo.absolutize().toURI());
			deleteOldRepositories(listener, workspace, label);
		}
	}

	private void deleteOldRepositories(TaskListener listener, FilePath workspace, Label label) throws IOException, InterruptedException {
		listener.getLogger().println("Delete old repositories from master");
		File[] repositories = new File(repositoriesDir, label.getId()).listFiles();
		if (repositories != null) {
			for (File repo : repositories) {
				String repoName = repo.getName();
				if (!repoName.equals(label.getLatestRepoFileArchive(workspace).getName())) {
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

	/**
	 * 
	 * archiveOrDownload == true getArchivePath else
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static FilePath getLatestRepo(Label label, FilePath workspace, boolean archiveOrDownload) throws IOException, InterruptedException {
		if (repositoriesDir == null)
			getInstance();

		String defaultPath = repositoriesDir + "/" + label.getId();

		String path = archiveOrDownload ? (label.getArchivePath() == null ? defaultPath : label.getArchivePath())
				: (label.getDownloadPath() == null ? defaultPath : label.getDownloadPath());
		
		String jenkinsRootPath = Jenkins.getInstance().getRootPath().getRemote(); // jenkinsRoot
		String jobWorkspacePath = workspace.getRemote(); // workspace
		//String slaveName = workspace.getSlgetRemote(); // workspace
		
		path = path.replace("{workspace}", jobWorkspacePath).replace("{jenkinsRoot}", jenkinsRootPath);

		File repositoriesLabelDir = new File(path);
		if (repositoriesLabelDir == null || !repositoriesLabelDir.exists()) {
			return null;
		}
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
