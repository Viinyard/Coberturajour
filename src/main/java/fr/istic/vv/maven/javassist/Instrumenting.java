package fr.istic.vv.maven.javassist;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VinYarD
 * created : 10/02/2019, 15:09
 */


public class Instrumenting {
	
	/**
	 * a map of qualifiedName -> line number -> execution
	 */
	public static Map<String, Map<Integer, Boolean>> lines = new HashMap<String, Map<Integer, Boolean>>();
	
	public static void addInstrumentedClass(String qualifiedName) {
		registerClass(qualifiedName);
	}
	
	public static void addInstrumentedStatement(String qualifiedName, int position) {
		registerClass(qualifiedName);
		lines.get(qualifiedName).put(position, false);
	}
	
	public static void isPassedThrough(String outputDirectory, String qualifiedName, final int position) {
			File f = new File(outputDirectory + File.separatorChar + "jassist.csv");
			loadFromFile(f);
			
		registerClass(qualifiedName);
		lines.get(qualifiedName).put(position, true);
		export(outputDirectory);
	}
	
	private static void loadFromFile(File f) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			lines.clear();
			while ((line = br.readLine()) != null) {
				String[] data = line.split(";");
				lines.putIfAbsent(data[0], new HashMap<>());
				lines.get(data[0]).put(Integer.parseInt(data[1]), Boolean.valueOf(data[2]));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	// private method
	private static void registerClass(String qualifiedName) {
		if (lines.get(qualifiedName) == null) {
			lines.put(qualifiedName, new HashMap<Integer, Boolean>());
		}
	}
	
	public static void export(String path) {
		File f = new File(path + File.separatorChar + "jassist.csv");
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f, false));
			
			for (String classFile : lines.keySet()) {
				Map<Integer, Boolean> instrumenters = lines.get(classFile);
				for (int line : instrumenters.keySet()) {
					writer.append(classFile + ";" + line + ";" + instrumenters.get(line) + ";\n");
				}
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

