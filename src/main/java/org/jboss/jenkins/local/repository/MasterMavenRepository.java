package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.jfree.util.Log;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import jenkins.model.Jenkins;

public class MasterMavenRepository {

	private static MasterMavenRepository instance;
	// private static File repositoriesDir;

	private MasterMavenRepository() {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			FilePath config = new FilePath(new FilePath(jenkins.getRootDir()), "shared-maven-repository/config.json");
			try {
				if (!config.exists()) {
					Label.loadStringFromFile();
					if (!config.exists()) {
						throw new RuntimeException(
								"Unable to create shared-maven-repository repository folder with config.json file");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/*
			 * repositoriesDir = new File(jenkins.getRootDir(), "shared-maven-repository");
			 * if (!repositoriesDir.exists()) { boolean created = repositoriesDir.mkdir();
			 * if (!created) { throw new
			 * RuntimeException("Unable to delete master repository folder"); } }
			 */

		}
	}

	public synchronized static MasterMavenRepository getInstance() {
		if (instance == null) {
			instance = new MasterMavenRepository();
		}
		return instance;
	}

	public void uploadRepository(FilePath repositoryTar, FilePath workspace, /* TaskListener listener, */ Label label,
			EnvVars env) throws IOException, InterruptedException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			// listener.getLogger().println("Upload " + repositoryTar.absolutize().toURI() +
			// " to Jenkins master");

			String path = label.getArchiveFilePath(workspace, env).getRemote();

			FilePath masterRepo = new FilePath(new FilePath(Channel.current(), path), repositoryTar.getName());
			repositoryTar.copyTo(masterRepo);
			// listener.getLogger().println("Repository uploaded to " +
			// masterRepo.absolutize().toURI());
			repositoryTar.delete();
			deleteOldRepositories(/* listener, */ workspace, label, env);
		}
	}

	private void deleteOldRepositories(/* TaskListener listener, */FilePath workspace, Label label, EnvVars env)
			throws IOException, InterruptedException {
		// listener.getLogger().println("Delete old repositories from master");
		List<FilePath> repositories = label.getArchiveFilePath(workspace, env).list();
		if (repositories != null) {
			for (FilePath repo : repositories) {
				String repoName = repo.getName();
				if (!repoName.equals(label.getLatestRepoFileArchive(workspace, env).getName())) {
					boolean deleted = repo.delete();
					/*
					 * if (!deleted) {
					 * listener.getLogger().println("Unable to delete old repository " + repoName);
					 * } else { listener.getLogger().println("Deleted old repository from master " +
					 * repoName); }
					 */
				}
			}
		}
	}

	/**
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static FilePath getLatestRepo(Label label, FilePath workspace, EnvVars env)
			throws IOException, InterruptedException {
		// if (repositoriesDir == null)
		// getInstance();

		FilePath archiveFile = label.getArchiveFilePath(workspace, env);

		FilePath archiveDir;
		if (archiveFile.isDirectory()) {
			archiveDir = archiveFile;
		} else {
			archiveDir = archiveFile.getParent();
		}

		// FilePath archiveDirFile = new FilePath(archiveDir.getRemote());
		// if (archiveDirFile == null || !archiveDirFile.exists()) {
		// return null;
		// }

		FilePath lastModifiedFile = lastFileModified(archiveDir);
		if (lastModifiedFile == null) {
			return null;
		}

		return lastModifiedFile;
	}

	private static FilePath lastFileModified(FilePath dir) throws IOException, InterruptedException {
		List<FilePath> files = dir.list(new FileFilter() {
			public boolean accept(File file) {
				return file.isFile();
			}
		});
		long lastMod = Long.MIN_VALUE;
		FilePath choice = null;
		if (files != null) {
			for (FilePath file : files) {
				if (file.lastModified() > lastMod) {
					choice = file;
					lastMod = file.lastModified();
				}
			}
		}
		return choice;
	}

}
