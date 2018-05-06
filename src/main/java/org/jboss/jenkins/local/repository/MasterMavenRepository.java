package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.IOException;
import java.util.List;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.remoting.Channel;
import hudson.remoting.LocalChannel;
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
			EnvVars env, JenkinsSlaveCallableBase logger) throws IOException, InterruptedException {
		try {
			String path = label.getArchiveFilePath(workspace, env).getRemote();

			FilePath archivedZipLocation = new FilePath(new FilePath(workspace.getChannel()/*label.getChannel()*/, path), repositoryTar.getName());

			// create dirs if not exists
			if(!archivedZipLocation.getParent().exists()) {
				archivedZipLocation.getParent().mkdirs();
			}
			logger.info("Uploading to " + archivedZipLocation.absolutize().toURI());
			logger.monitorFileSizeChange(archivedZipLocation.getRemote());
			repositoryTar.copyTo(archivedZipLocation);
			logger.info("Repository uploaded.");
			repositoryTar.delete();
			deleteOldRepositories(logger, workspace, label, env);
		} catch (Exception e) {
			logger.info(e.getMessage());
			e.printStackTrace();
			// logger.info("e.getMessage: " + e.getMessage());
		}
	}

	private void deleteOldRepositories(JenkinsSlaveCallableBase logger, FilePath workspace, Label label, EnvVars env)
			throws IOException, InterruptedException {
		logger.info("Delete old repositories from master");
		List<FilePath> repositories = label.getArchiveFilePath(workspace, env).list();
		if (repositories != null) {
			for (FilePath repo : repositories) {
				String repoName = repo.getName();
				if (!repoName.equals(label.getLatestRepoFileArchive(workspace, env).getName())) {
					boolean deleted = repo.delete();

					if (!deleted) {
						logger.info("Unable to delete old repository " + repoName);
					} else {
						logger.info("Deleted old repository from master " + repoName);
					}

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

		FilePath archiveFile = label.getArchiveFilePath(workspace, env);

		if (!archiveFile.exists() || !(new File(archiveFile.getRemote())).exists() || archiveFile.getChannel() == null
				|| archiveFile.getChannel() instanceof LocalChannel) {
			// TODO check if there are any exception for master node named master

			//FilePath master = new FilePath(Channel.current(), archiveFile.getRemote());
			FilePath master = new FilePath(label.getChannel(), archiveFile.getRemote());
			if (master.exists()) {
				archiveFile = master;
			} else {
				return null;
			}
		}

		FilePath archiveDir;
		if (archiveFile.isDirectory()) {
			archiveDir = archiveFile;
		} else {
			archiveDir = archiveFile.getParent();
		}

		FilePath lastModifiedFile = lastFileModified(archiveDir);

		return lastModifiedFile;
	}

	private static FilePath lastFileModified(FilePath dir) throws IOException, InterruptedException {
		//dir = new FilePath(Channel.current(), dir.getRemote());
		List<FilePath> files = dir.list(new CustomFileFilter());
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
