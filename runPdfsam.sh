#!/bin/sh

java -ea -Xmx35g -cp build/libs/incremental-ifds.jar guava-13.0.jar:lib/soot-3.0.1.jar:lib/junit-4.10.jar soot.jimple.interproc.ifds.test.SingleJUnitTestRunner soot.jimple.interproc.ifds.test.IFDSTestPDFsam#newVersionSH_Propagate > output_newVersionSH_Propagate.log 2> error_newVersionSH_Propagate.log

java -ea -Xmx35g -cp build/libs/incremental-ifds.jar soot.jimple.interproc.ifds.test.SingleJUnitTestRunner soot.jimple.interproc.ifds.test.IFDSTestPDFsam#newVersionSH_Propagate > output_newVersionSH_Propagate.log 2> error_newVersionSH_Propagate.log

java -ea -Xmx35g -cp build/libs/incremental-ifds.jar:lib/guava-13.0.jar:lib/soot-3.0.1.jar:lib/junit-4.10.jar soot.jimple.interproc.ifds.test.SingleJUnitTestRunner soot.jimple.interproc.ifds.test.IFDSTestPDFsam#newVersionSH_Propagate

java -ea -Xmx35g -cp build/libs/incremental-ifds.jar:lib/guava-13.0.jar:lib/soot-4.1.0.jar:lib/junit-4.10.jar soot.jimple.interproc.ifds.test.SingleJUnitTestRunner soot.jimple.interproc.ifds.test.IFDSTestPDFsam#newVersionSH_Propagate