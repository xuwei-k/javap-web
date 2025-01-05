package javap_web

import org.scalatest.freespec.AnyFreeSpec
import java.io.File
import sbt.io.IO

class Test extends AnyFreeSpec {
  "test" in {
    IO.withTemporaryDirectory { dir =>
      val src = new File(dir, "A.java")
      IO.write(src, "class A {}")
      Main.javac(Array(src.getCanonicalPath, "-d", dir.getCanonicalPath))
      val result = Main.javap(dir.getCanonicalPath)
      assert(result.error() == null, s"${result.error()}")
      val expect =
        """|  Compiled from "A.java"
           |class A
           |  minor version: 0
           |  major version: 52
           |  flags: ACC_SUPER
           |Constant pool:""".stripMargin.linesIterator.toList
      expect.foreach { line =>
        assert(result.result().contains(line), line)
      }
    }
  }
}
