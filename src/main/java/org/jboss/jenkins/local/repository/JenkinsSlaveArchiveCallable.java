package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jenkinsci.remoting.RoleChecker;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;

public class JenkinsSlaveArchiveCallable extends JenkinsSlaveCallableBase
		implements Callable<String, IOException>, Serializable {

	Label label;
	FilePath workspace;
	EnvVars env;
	String logDirPath;
	static String loggerName = "shared-maven-repository-slave-archive";

	public JenkinsSlaveArchiveCallable(Label label, FilePath workspace, EnvVars env, TaskListener listener) {
		super(listener, workspace);
		this.label = label;
		this.env = env;
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
	}

	@Override
	public String call() throws IOException {
		FilePath archivedLocalFile;

		try {
			label.clearFilePathsCache();

			Logger logger = Logger.getLogger(loggerName);
			FileHandler fh;

			try {

				// This block configure the logger with handler and formatter
				fh = new FileHandler(this.logDirPath + "/" + loggerName + ".log");
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);

				// the following statement is used to log any messages
				// logger.info("My first log");

			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			logger.info(label.toString());
			if (label.getArchiveFilePath(workspace, env).isDirectory()) {
				// is dir so create new zip with unique name in this dir
				archivedLocalFile = new FilePath(label.getDownloadFilePath(workspace, env).getParent(),
						"archived-" + UUID.randomUUID().toString() + ".zip");
			} else {
				// is file so create new file next to old one
				archivedLocalFile = new FilePath(label.getDownloadFilePath(workspace, env).getParent(),
						label.getArchiveFilePath(workspace, env).getBaseName() + "-archived-"
								+ UUID.randomUUID().toString() + ".zip");
			}
			logger.info("archivedLocalFile.getRemote()");
			logger.info(archivedLocalFile.getRemote());
			if (!archivedLocalFile.exists()) {
				// listener.getLogger().println("Creating empty zip " +
				// archivedLocalFile.toURI());
				try {
					try {
						archivedLocalFile.getParent().mkdirs();
					} catch (Exception e) {
						logger.info(e.toString());
						e.printStackTrace();
					}
					archivedLocalFile.touch(new Date().getTime());
				} catch (Exception e) {
					// https://github.com/openshift/jenkins-client-plugin/issues/121
					logger.info(e.toString());
					e.printStackTrace();
					try {
						new File(archivedLocalFile.getRemote()).createNewFile();
					} catch (Exception ee) {
						logger.info(ee.toString());
						ee.printStackTrace();
					}
				}
			}

			FileFilter filter;
			if (label.isM2Repo()) {
				RepoFileFilter repoFilter = new RepoFileFilter();
				try {
					repoFilter.preparePath(new File(label.getDownloadFilePath(workspace, env).toURI()));
				} catch (InterruptedException e) {
					e.printStackTrace();
					logger.info(e.toString());
				}
				filter = repoFilter;
			} else {
				filter = new CustomFileFilter();
			}
			OutputStream repoOutputStream;

			repoOutputStream = archivedLocalFile.write();
			try {
				// listener.getLogger().println("Fill archive " + archivedLocalFile.toURI());
				label.getDownloadFilePath(workspace, env).archive(TrueZipArchiver.FACTORY, repoOutputStream, filter);
				// listener.getLogger().println("Done!");
			} catch (IOException e) {
				repoOutputStream.close();
				logger.info(e.toString());
				e.printStackTrace();
				return "Failed";
			}
			repoOutputStream.close();
			MasterMavenRepository.getInstance().uploadRepository(archivedLocalFile, workspace, /* listener, */ label,
					env, logger);
		} catch (InterruptedException e) {
			e.printStackTrace();
			// logger.info(e.toString());
			return "Failed";
		}

		return "Done";
	}

}
