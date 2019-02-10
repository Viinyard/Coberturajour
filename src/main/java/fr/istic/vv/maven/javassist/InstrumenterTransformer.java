package fr.istic.vv.maven.javassist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.analysis.ControlFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VinYarD
 * created : 04/02/2019, 12:00
 */


public class InstrumenterTransformer extends ClassTransformer {
	@Override
	public void applyTransformations(ClassPool classPool, CtClass classToTransform) throws TransformationException {
		Instrumenting.addInstrumentedClass(classToTransform.getName());
		getLogger().info("INFO : " +classToTransform.getName());
		for(CtMethod ctMethod : classToTransform.getDeclaredMethods()) {
			try {
				ControlFlow cf = new ControlFlow(ctMethod);
				getLogger().info(ctMethod.getLongName());
				ControlFlow.Block[] blocks = cf.basicBlocks();
				List<Integer> listLine = new ArrayList<>();
				for(ControlFlow.Block b : blocks) {
					getLogger().info(b.toString());
					int line = ctMethod.insertAt(ctMethod.getMethodInfo().getLineNumber(b.position()), false, "{ fr.istic.vv.maven.javassist.Instrumenting.isPassedThrough(\""+ this.getBaseDirectory()+"\", \""+classToTransform.getName()+"\", "+ b.position()+"); }");
					listLine.add(line);
					getLogger().info("Line : " + line + ", " + (ctMethod.getMethodInfo().getLineNumber(b.position() )+ b.index()));
					Instrumenting.addInstrumentedStatement(classToTransform.getName(), line);
				}
				for(int i : listLine) {
					ctMethod.insertAt(i , true, "{ fr.istic.vv.maven.javassist.Instrumenting.isPassedThrough(\""+ this.getBaseDirectory()+"\", \""+classToTransform.getName()+"\", "+ i +"); }");
				}
			} catch (BadBytecode badBytecode) {
				badBytecode.printStackTrace();
			} catch (CannotCompileException e) {
				e.printStackTrace();
			}
		}
	}
}
