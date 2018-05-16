package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
	// TaskListener listener;
	String logDirPath;
	static String loggerName = "shared-maven-repository-slave-download";

	public JenkinsSlaveDownloadCallable(FilePath archivedZipFile, FilePath downloadZipFile, FilePath downloadDest,
			/* TaskListener listener */FilePath workspace) {
		this.archivedZipFile = archivedZipFile;
		this.downloadZipFile = downloadZipFile;
		this.downloadDest = downloadDest;
		// this.listener = listener;
		this.logDirPath = workspace.getRemote();
		// this.logDirPath = "/local/git/";
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
		// TODO Auto-generated method stub
	}

	@Override
	public String call() throws IOException {
		Logger logger = Logger.getLogger(loggerName);
		FileHandler fh;

		try {

			// This block configure the logger with handler and formatter
			fh = new FileHandler(this.logDirPath + "/" + loggerName + ".log");
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			// the following statement is used to log any messages

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * try {
		 * listener.getLogger().println("Executing remote call on slave with address: "
		 * + InetAddress.getLocalHost().getHostName()); } catch (Exception e) {
		 * e.printStackTrace(); }
		 */
		try {
			// listener.getLogger().println("Downloading files from " +
			// archivedZipFile.getRemote() + " to " + downloadDest.getRemote());
			logger.info("Executing remote call on slave with address: " + InetAddress.getLocalHost().getHostName());
			logger.info("Downloading files from " + archivedZipFile.getRemote() + " to " + downloadDest.getRemote());
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
