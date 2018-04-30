package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;

import org.acegisecurity.AccessDeniedException;
import org.jfree.util.Log;

import hudson.EnvVars;
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
	//private static File repositoriesDir;

	private MasterMavenRepository() {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			File config = new File(jenkins.getRootDir(), "shared-maven-repository/config.json");
			if(!config.exists()) {
				try {
					Label.loadStringFromFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(!config.exists()) {
					throw new RuntimeException("Unable to create shared-maven-repository repository folder with config.json file");
				}
			}
			/*repositoriesDir = new File(jenkins.getRootDir(), "shared-maven-repository");
			if (!repositoriesDir.exists()) {
				boolean created = repositoriesDir.mkdir();
				if (!created) {
					throw new RuntimeException("Unable to delete master repository folder");
				}
			}*/
			
		}
	}

	public synchronized static MasterMavenRepository getInstance() {
		if (instance == null) {
			instance = new MasterMavenRepository();
		}
		return instance;
	}

	public void uploadRepository(FilePath repositoryTar, FilePath workspace, TaskListener listener, Label label, EnvVars env)
			throws IOException, InterruptedException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			listener.getLogger().println("Upload " + repositoryTar.absolutize().toURI() + " to Jenkins master");

			String path = label.getArchiveFilePath(workspace, env).getRemote();

			FilePath masterRepo = new FilePath(new File(new File(path), repositoryTar.getName()));
			repositoryTar.copyTo(masterRepo);
			listener.getLogger().println("Repository uploaded to " + masterRepo.absolutize().toURI());
			repositoryTar.delete();
			deleteOldRepositories(listener, workspace, label, env);
		}
	}

	private void deleteOldRepositories(TaskListener listener, FilePath workspace, Label label,EnvVars env) throws IOException, InterruptedException {
		listener.getLogger().println("Delete old repositories from master");
		File[] repositories = new File(label.getArchiveFilePath(workspace, env).getRemote()).listFiles();
		if (repositories != null) {
			for (File repo : repositories) {
				String repoName = repo.getName();
				if (!repoName.equals(label.getLatestRepoFileArchive(workspace, env).getName())) {
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
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static FilePath getLatestRepo(Label label, FilePath workspace, EnvVars env) throws IOException, InterruptedException {
		//if (repositoriesDir == null)
		//	getInstance();
		
		Log.info("a");
		FilePath archiveFile = label.getArchiveFilePath(workspace, env);
		Log.info("a1");
		
		FilePath archiveDir;
		if(archiveFile.isDirectory()) {
			archiveDir = archiveFile;
		}else {
			archiveDir = archiveFile.getParent();
		}
		Log.info("a2");
		
		
		File archiveDirFile = new File(archiveDir.getRemote());
		if (archiveDirFile == null || !archiveDirFile.exists()) {
			return null;
		}
		Log.info("a3");
		Log.info(archiveDirFile.toString());
		File lastModifiedFile = lastFileModified(archiveDirFile);
		if (lastModifiedFile == null) {
			return null;
		}
		Log.info("a4");
		
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
