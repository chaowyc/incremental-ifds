package soot.jimple.interproc.ifds.test;

import org.junit.Test;
import soot.*;
import soot.jimple.interproc.ifds.IFDSTabulationProblem;
import soot.jimple.interproc.ifds.InterproceduralCFG;
import soot.jimple.interproc.ifds.problems.IFDSLocalInfoFlow;
import soot.jimple.interproc.ifds.problems.IFDSReachingDefinitions;
import soot.jimple.interproc.ifds.problems.UpdatableReachingDefinition;
import soot.jimple.interproc.ifds.solver.IFDSSolver;
import soot.jimple.interproc.ifds.template.JimpleBasedInterproceduralCFG;
import soot.jimple.interproc.incremental.UpdatableWrapper;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class SuperGraphTest {
    private void performDirectRun(final String className) {
        soot.G.reset();
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifds", new SceneTransformer() {
            protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
                // Force method load to avoid spurious changes
                for (SootClass sc : Scene.v().getApplicationClasses())
                    for (SootMethod sm : sc.getMethods())
                        sm.retrieveActiveBody();

                long timeBefore = System.nanoTime();
                System.out.println("Running IFDS on initial CFG...");
                InterproceduralCFG<UpdatableWrapper<Unit>, UpdatableWrapper<SootMethod>> icfg = new JimpleBasedInterproceduralCFG();
                IFDSTabulationProblem<UpdatableWrapper<Unit>, UpdatableReachingDefinition, UpdatableWrapper<SootMethod>,
                        InterproceduralCFG<UpdatableWrapper<Unit>, UpdatableWrapper<SootMethod>>> problem =
                        new IFDSReachingDefinitions(icfg);
                IFDSSolver<UpdatableWrapper<Unit>,UpdatableReachingDefinition,UpdatableWrapper<SootMethod>,
                        InterproceduralCFG<UpdatableWrapper<Unit>,UpdatableWrapper<SootMethod>>> solver =
                        new IFDSSolver<UpdatableWrapper<Unit>,UpdatableReachingDefinition,UpdatableWrapper<SootMethod>,
                                InterproceduralCFG<UpdatableWrapper<Unit>,UpdatableWrapper<SootMethod>>>(problem);
                solver.solve();

                Unit ret = Scene.v().getMainMethod().getActiveBody().getUnits().getLast();
                Set<UpdatableReachingDefinition> results = solver.ifdsResultsAt(icfg.wrapWeak(ret));

                System.out.println("Time elapsed: " + ((double) (System.nanoTime() - timeBefore)) / 1E9);
            }
        }));

        String cpSep = ":";
        String udir = System.getProperty("user.dir");
        String sootcp = udir + File.separator + "test/junit-4.10.jar";
//                + "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar" + cpSep
//                + "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";
        System.out.println("Soot classpath: " + sootcp);

        soot.Main.v().run(new String[] {
                "-W",
                "-main-class", className,
                "-process-path", udir + File.separator + "test",
                "-src-prec", "java",
                "-pp",
                "-cp", sootcp,
                "-no-bodies-for-excluded",
                "-exclude", "java.",
                className } );
    }
    @Test
    public void simpleTest() {
        System.out.println("Start simpleTest ...");
        performDirectRun("org.junit.runner.JUnitCore");
        System.out.println("simpleTest finished.");
    }
}
