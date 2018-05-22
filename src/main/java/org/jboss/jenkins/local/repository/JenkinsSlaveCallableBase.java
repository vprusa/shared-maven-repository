package org.jboss.jenkins.local.repository;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jenkinsci.remoting.RoleChecker;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;

public class JenkinsSlaveCallableBase implements Callable<String, IOException>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	TaskListener listener;
	FilePath workspace;
	EnvVars env;
	String logDirPath;
	Logger logger;
	PrintStream ps;
	Label label;

	public JenkinsSlaveCallableBase(TaskListener listener, FilePath workspace, EnvVars env, Label label) {
		this.listener = listener;
		this.workspace = workspace;
		this.env = env;
		this.label = label;
		this.logDirPath = workspace.getRemote();
		// this.logDirPath = "/local/git/";
	}

	public void info(String msg) {
		if (logger != null) {
			logger.info(msg);
		}
		if (ps != null) {
			ps.println(msg);
		}
		if (ps == null && logger == null) {
			System.out.print(msg);
		}
	}

	public void initLogging(String loggerName) {
		String path = this.logDirPath + "/" + loggerName + ".log";
		if (listener == null) {
			logger = Logger.getLogger(loggerName);
			FileHandler fh;
			try {
				// This block configure the logger with handler and formatter
				fh = new FileHandler(path);
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);
				info("Logging as java.util.logging.Logger into: " + path);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			ps = listener.getLogger();
			info("Loggin as java.util.logging.Logger into: " + path);
		}
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
	}

	@Override
	public String call() throws IOException {
		initLogging("shared-maven-repository-slave-base");
		return "DoneBase";
	}

}
