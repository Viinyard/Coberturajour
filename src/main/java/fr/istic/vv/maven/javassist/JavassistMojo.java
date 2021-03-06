package fr.istic.vv.maven.javassist;

import static java.lang.Thread.currentThread;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Mojo(name = "javassist", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JavassistMojo extends AbstractMojo implements ILogger {

	private static final Class<ClassTransformer> TRANSFORMER_TYPE = ClassTransformer.class;

	@Component
	private BuildContext buildContext;

	@Parameter(defaultValue = "${project}", property = "project", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "packageName", required = true)
	private String packageName;

	@Parameter(property = "outputDirectory", required = true, defaultValue = "${project.build.outputDirectory}/Jacoco")
	private String outputDirectory;

	public void execute() throws MojoExecutionException {
		final ClassLoader originalContextClassLoader = currentThread()
				.getContextClassLoader();
		try {
			final List<String> classpathElements = getCompileClasspathElements();
			loadClassPath(originalContextClassLoader,
					generateClassPathUrls(classpathElements));
			transform(classpathElements);
			Instrumenting.export(outputDirectory);
		} catch (DependencyResolutionRequiredException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			currentThread().setContextClassLoader(originalContextClassLoader);
		}
	}

	public final void transform(final List<String> classPaths)
			throws MojoExecutionException {
		int errors = 0;
		if (classPaths.isEmpty())
			return;

		ClassTransformer transformer = instantiateTransformerClass();
		final ClassPool classPool = new ClassPool(ClassPool.getDefault());
		classPool.appendClassPath(new LoaderClassPath(Thread.currentThread()
				.getContextClassLoader()));

		final Iterator<String> classPathIterator = classPaths.iterator();
		while (classPathIterator.hasNext()) {
			final String classPath = classPathIterator.next();
			getLog().debug("Processing " + classPath);
			final ClassFileIterator classNames = createClassNameIterator(classPath);
			while (classNames.hasNext()) {
				final String className = classNames.next();
				if (!transformer.filterClassName(className)) {
					getLog().debug("Skipping " + className);
					continue;
				}

				try {
					final CtClass candidateClass = classPool.get(className);
					if (transformer.filterCtClass(candidateClass)) {
						transformer.applyTransformations(classPool,
								candidateClass);
						writeFile(candidateClass, outputDirectory);
					}
				} catch (final TransformationException e) {
					errors++;
					addMessage(classNames.getLastFile(), 1, 1, e.getMessage(),
							null);
					continue;
				} catch (final NotFoundException e) {
					errors++;
					addMessage(classNames.getLastFile(), 1, 1, String.format(
							"Class %s could not be resolved due "
									+ "to dependencies not found on current "
									+ "classpath.", className), e);
					continue;
				} catch (final Exception e) {
					errors++;
					addMessage(classNames.getLastFile(), 1, 1, String.format(
							"Class %s could not be transformed.", className), e);
					continue;
				}
			}
		}
		if (errors > 0)
			throw new MojoExecutionException(errors
					+ " errors found during transformation.");
	}

	public void writeFile(CtClass candidateClass, String targetDirectory)
			throws Exception {
		candidateClass.getClassFile().compact();
		candidateClass.rebuildClassFile();

		String classname = candidateClass.getName();
		String filename = targetDirectory + File.separatorChar
				+ classname.replace('.', File.separatorChar) + ".class";
		int pos = filename.lastIndexOf(File.separatorChar);
		if (pos > 0) {
			String dir = filename.substring(0, pos);
			if (!dir.equals(".")) {
				File outputDir = new File(dir);
				outputDir.mkdirs();
				buildContext.refresh(outputDir);
			}
		}
		try (DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
						buildContext.newFileOutputStream(new File(filename))))) {
			candidateClass.toBytecode(out);
		}
	}

	private ClassFileIterator createClassNameIterator(final String classPath) {
		if (new File(classPath).isDirectory()) {
			return new ClassNameDirectoryIterator(classPath, buildContext);
		} else {
			return new ClassNameJarIterator(classPath, buildContext);
		}
	}

	private List<String> getCompileClasspathElements()
			throws DependencyResolutionRequiredException {
		List<?> ret = project.getCompileClasspathElements();
		//ret.remove(project.getBuild().getOutputDirectory());
		return Lists.newArrayList(Iterables.filter(ret, String.class));
	}

	protected ClassTransformer instantiateTransformerClass() {
		ClassTransformer transformerInstance = new InstrumenterTransformer();

		transformerInstance.setLogger(this);
		transformerInstance.setBaseDirectory(outputDirectory);
		transformerInstance.setFilterPackageName(packageName);

		return transformerInstance;
	}

	private List<URL> generateClassPathUrls(Iterable<String> classpathElements) {
		final List<URL> classPath = new ArrayList<URL>();
		for (final String runtimeResource : classpathElements) {
			URL url = resolveUrl(runtimeResource);
			if (url != null) {
				classPath.add(url);
			}
		}

		return classPath;
	}

	private void loadClassPath(final ClassLoader contextClassLoader,
			final List<URL> urls) {
		if (urls.size() <= 0)
			return;

		final URLClassLoader pluginClassLoader = URLClassLoader.newInstance(
				urls.toArray(new URL[urls.size()]), contextClassLoader);
		currentThread().setContextClassLoader(pluginClassLoader);
	}

	private URL resolveUrl(final String resource) {
		try {
			return new File(resource).toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void addMessage(File file, int line, int pos, String message,
			Throwable e) {
		buildContext.addMessage(file, line, pos, message,
				BuildContext.SEVERITY_ERROR, e);
	}

	@Override
	public void debug(String message) {
		getLog().debug(message);
	}

	@Override
	public void debug(String message, Throwable throwable) {
		getLog().debug(message, throwable);
	}

	@Override
	public void info(String message) {
		getLog().info(message);
	}

	@Override
	public void info(String message, Throwable throwable) {
		getLog().info(message, throwable);
	}

	@Override
	public void warn(String message) {
		getLog().warn(message);
	}

	@Override
	public void warn(String message, Throwable throwable) {
		getLog().warn(message, throwable);
	}

	@Override
	public void error(String message) {
		getLog().error(message);
	}

	@Override
	public void error(String message, Throwable throwable) {
		getLog().error(message, throwable);
	}
}
