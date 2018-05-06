package org.jboss.jenkins.local.repository;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;

import org.jenkinsci.remoting.RoleChecker;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;

public class JenkinsSlaveDownloadCallable extends JenkinsSlaveCallableBase
implements Callable<String, IOException>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String loggerName = "shared-maven-repository-slave-download";

	public JenkinsSlaveDownloadCallable(TaskListener listener, FilePath workspace, EnvVars env, Label label) {
		super(listener, workspace, env, label);
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
	}

	@Override
	public String call() throws IOException {
		initSlave(loggerName);
		try {
			info("Downloading files for label: " + label.toString());
			FilePath downloadDest = label.getDownloadFilePath(workspace, env);
			FilePath downloadZipFile = new FilePath(downloadDest.getParent(), "file.zip");

			if (downloadZipFile.exists()) {
				info(downloadZipFile.getRemote() + " already exists, delete.");
				boolean deleted = downloadZipFile.delete();
				if (!deleted) {
					info("Unable to delete file: " + downloadZipFile.getRemote());
				}
			}
			FilePath archivedFile = label.getArchiveFilePath(workspace, env);

			FilePath archivedZipFile = label.getLatestRepoFileArchive(workspace, env);

			if (archivedFile == null || !archivedFile.exists() || archivedZipFile == null
					|| !archivedZipFile.exists()) {
				return "Jenkins does not have any file with path: " + archivedFile.getRemote();
			}
			
			info("Downloading files from " + archivedZipFile.getRemote() + " to " + downloadDest.getRemote());
			final String downloadZipFileRemote = downloadZipFile.getRemote();
			monitorFileSizeChange(downloadZipFileRemote);
			
			archivedZipFile.copyTo(downloadZipFile);
			Label.deleteDir(new FilePath(label.getChannel(),downloadDest.getRemote()));
			downloadZipFile.unzip(downloadDest.getParent());
		} catch (InterruptedException e) {
			e.printStackTrace();
			return "Failed";
		}
		return "Done";
	}

}
