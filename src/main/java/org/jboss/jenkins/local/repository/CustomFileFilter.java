package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

public class CustomFileFilter implements FileFilter, Serializable {
	public boolean accept(File file) {
		return file.isFile();
	}
}