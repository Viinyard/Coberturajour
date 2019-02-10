package fr.istic.vv.maven.javassist;

import java.io.File;
import java.util.Iterator;

public interface ClassFileIterator extends Iterator<String> {
	public File getLastFile();
}
