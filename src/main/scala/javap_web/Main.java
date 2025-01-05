package javap_web;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Main {
  private static final Method javapRunMethod;
  private static final Method javacCompileMethod;

  static {
    try {
      final Class<?> javapClass = Class.forName("com.sun.tools.javap.Main");
      javapRunMethod = javapClass.getMethod("run", String[].class, PrintWriter.class);
      final Class<?> javacClass = Class.forName("com.sun.tools.javac.Main");
      javacCompileMethod = javacClass.getMethod("compile", String[].class, PrintWriter.class);
    } catch (NoSuchMethodException e) {
      e.getStackTrace();
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      e.getStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static void mkdir(String path) {
    new java.io.File(path).mkdir();
  }

  public static javap_web.Result javap(String dir) {
    try {
      final List<String> argList = new ArrayList<>();
      argList.add("-v");
      argList.addAll(
          java.nio.file.Files.find(
                  new File(dir).toPath(),
                  20,
                  (f, x) -> x.isRegularFile() && f.toFile().getName().endsWith(".class"))
              .map(
                  f -> {
                    try {
                      return "file://" + f.toFile().getCanonicalPath();
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .collect(Collectors.toList()));
      final String[] args = argList.toArray(new String[0]);
      final StringWriter s = new StringWriter();
      final PrintWriter writer = new PrintWriter(s);
      final int result = (int) javapRunMethod.invoke(null, args, writer);
      s.flush();
      if (result != 0) {
        return new javap_web.Result(null, s.toString());
      } else {
        return new javap_web.Result(s.toString(), null);
      }
    } catch (Exception e) {
      final String err =
          String.join(
              "<br />",
              Arrays.asList(
                  Arrays.stream(e.getStackTrace())
                      .map(Object::toString)
                      .collect(Collectors.joining())));
      return new javap_web.Result(null, err);
    }
  }

  public static String javac(String[] args) {
    try {
      final StringWriter s = new StringWriter();
      final PrintWriter writer = new PrintWriter(s);
      final int result = (int) javacCompileMethod.invoke(null, args, writer);
      s.flush();
      return s.toString();
    } catch (Exception e) {
      return String.join(
          "\n",
          Arrays.asList(
              Arrays.stream(e.getStackTrace())
                  .map(Object::toString)
                  .collect(Collectors.joining())));
    }
  }

  public static String format(final String source) {
    try {
      return new Formatter().formatSourceAndFixImports(source);
    } catch (FormatterException e) {
      e.printStackTrace();
      return source;
    }
  }
}
