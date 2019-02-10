package fr.istic.vv.maven.javassist;

import javassist.ClassPool;
import javassist.CtClass;

public abstract class ClassTransformer {
	private String filterPackageName;

	private ILogger logger;
	
	private String baseDirectory;
	
	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}
	
	public String getBaseDirectory() {
		return this.baseDirectory;
	}

	public final ILogger getLogger() {
		return logger;
	}

	public final void setLogger(ILogger logger) {
		this.logger = logger;
	}

	public final String getFilterPackageName() {
		return filterPackageName;
	}

	public final void setFilterPackageName(String filterPackageName) {
		this.filterPackageName = filterPackageName;
	}

	public abstract void applyTransformations(ClassPool classPool,
			CtClass classToTransform) throws TransformationException;

	public boolean filterCtClass(final CtClass candidateClass) {
		return true;
	}

	public boolean filterClassName(String className) {
		if (filterPackageName != null && filterPackageName.length() > 0)
			return className.startsWith(filterPackageName);

		return false;
	}
}
