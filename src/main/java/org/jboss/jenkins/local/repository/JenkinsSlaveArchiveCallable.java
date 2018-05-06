package org.jboss.jenkins.local.repository;

import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.jenkinsci.remoting.RoleChecker;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;

public class JenkinsSlaveArchiveCallable extends JenkinsSlaveCallableBase
		implements Callable<String, IOException>, Serializable {

	String logDirPath;
	String loggerName = "shared-maven-repository-slave-archive";

	public JenkinsSlaveArchiveCallable(FilePath workspace, EnvVars env, TaskListener listener, Label label) {
		super(listener, workspace, env, label);
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
	}

	@Override
	public String call() throws IOException {
		initSlave(loggerName);
		FilePath archivedLocalFile;

		try {
			label.clearFilePathsCache();

			info(label.toString());
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
			if (!archivedLocalFile.exists()) {
				//info("archivedLocalFile");
				//info(archivedLocalFile.toString());
				//info(archivedLocalFile.getChannel().toString());
				// info("Creating empty zip " +
				// archivedLocalFile.toURI());
				// try {
				// try {
				archivedLocalFile.getParent().mkdirs();
				// } catch (Exception e) {
				// info(e.toString());
				// e.printStackTrace();
				// }
				archivedLocalFile.touch(new Date().getTime());
				// } catch (Exception e) {
				// https://github.com/openshift/jenkins-client-plugin/issues/121
				// info(e.toString());
				// e.printStackTrace();
				// try {
				// new File(archivedLocalFile.getRemote()).createNewFile();
				// } catch (Exception ee) {
				// info(ee.toString());
				// ee.printStackTrace();
				// }
				// }
			}
			info("Using file filter matching " + (label.isM2Repo() ? "latest .repository files" : "all files"));
			FileFilter filter;
			if (label.isM2Repo()) {
				RepoFileFilter repoFilter = new RepoFileFilter();
				try {
					// repoFilter.preparePath(new File(label.getDownloadFilePath(workspace,
					// env).toURI()));
					repoFilter.preparePath(label.getDownloadFilePath(workspace, env));
				} catch (InterruptedException e) {
					e.printStackTrace();
					info(e.toString());
				}
				filter = repoFilter;
			} else {
				filter = new CustomFileFilter();
			}
			OutputStream repoOutputStream;

			// start asynchronous file size change logging
			final String archivedLocalFileRemote = archivedLocalFile.getRemote();
			monitorFileSizeChange(archivedLocalFileRemote);

			repoOutputStream = archivedLocalFile.write();

			info("Writing into: " + archivedLocalFile.getRemote() + " (" + archivedLocalFile.getChannel().toString() + ")");

			try {
				int archivedCount = label.getDownloadFilePath(workspace, env).archive(TrueZipArchiver.FACTORY,
						repoOutputStream, filter);
				info("Archived " + archivedCount + " files");
			} catch (IOException e) {
				repoOutputStream.close();
				e.printStackTrace();
				info(e.toString());
				return "Failed";
			}
			repoOutputStream.close();
			MasterMavenRepository.getInstance().uploadRepository(archivedLocalFile, workspace, label, env, this);
		} catch (InterruptedException e) {
			e.printStackTrace();
			info(e.toString());
			return "Failed";
		}

		return "Done";
	}

}
