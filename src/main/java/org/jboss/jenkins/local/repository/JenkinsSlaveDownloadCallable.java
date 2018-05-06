package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;

import org.jenkinsci.remoting.RoleChecker;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;

public class JenkinsSlaveDownloadCallable implements Callable<String, IOException>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	FilePath archivedZipFile;
	FilePath downloadZipFile;
	FilePath downloadDest;
	TaskListener listener;

	public JenkinsSlaveDownloadCallable(FilePath archivedZipFile, FilePath downloadZipFile, FilePath downloadDest,
			TaskListener listener) {
		this.archivedZipFile = archivedZipFile;
		this.downloadZipFile = downloadZipFile;
		this.downloadDest = downloadDest;
		this.listener = listener;
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
		// TODO Auto-generated method stub
	}

	@Override
	public String call() throws IOException {
		try {
			listener.getLogger().println(
					"Executing remote call on slave with address: " + InetAddress.getLocalHost().getHostName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			listener.getLogger().println(
					"Downloading files from " + archivedZipFile.getRemote() + " to " + downloadDest.getRemote());
			archivedZipFile.copyTo(downloadZipFile);
			Label.deleteDir(new File(downloadDest.getRemote()));
			downloadZipFile.unzip(downloadDest.getParent());
		} catch (InterruptedException e) {
			e.printStackTrace();
			return "Failed";
		}
		return "Done";
	}

}
