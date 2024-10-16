/*
 Copyright (c) 2021, Robby, Kansas State University
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.sireum.forms

import com.formdev.flatlaf.intellijthemes.{FlatDarkFlatIJTheme, FlatLightFlatIJTheme}
import com.formdev.flatlaf.FlatLaf
import com.jthemedetecor.OsThemeDetector

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader


object FormsApp extends App {
  lazy val sireumJar: java.io.File = Option(System.getenv("SIREUM_HOME")) match {
    case Some(homePath) => new java.io.File(new java.io.File(new java.io.File(homePath), "bin"), "sireum.jar")
    case _ => throw new Error("Please define SIREUM_HOME environment variable")
  }
  lazy val cl: ClassLoader = new URLClassLoader(Seq(sireumJar.toURI.toURL), getClass.getClassLoader)
  lazy val sireumHome = invokeStatic("org.sireum.Os", "path", sireumJar.getParentFile.getParentFile.getAbsolutePath)

  def init(isDark: Boolean = OsThemeDetector.getDetector.isDark): Unit = {
    FlatLaf.setup(if (isDark) new FlatDarkFlatIJTheme else new FlatLightFlatIJTheme)
  }

  def encodeName(name: String): String =
    name.replace("+", "$plus").replace("-", "$minus")

  def construct(className: String, args: Any*): Any = {
    val c = cl.loadClass(className)
    val constructor = c.getConstructors()(0)
    constructor.newInstance(args: _*)
  }

  def invokeStatic(className: String, methodName: String, args: Any*): Any = {
    val c = cl.loadClass(s"$className$$")
    val companion = c.getField("MODULE$").get(null)
    val name = encodeName(methodName)
    for (m <- c.getMethods.toSeq) {
      if (m.getName == name) {
        return m.invoke(companion, args: _*)
      }
    }
    throw new Error(s"Could not find $className.$methodName")
  }

  def invokeInstance(className: String, methodName: String, receiver: Any, args: Any*): Any = {
    val c = cl.loadClass(className)
    val name = encodeName(methodName)
    for (m <- c.getMethods.toSeq) {
      if (m.getName == name) {
        return m.invoke(receiver, args: _*)
      }
    }
    throw new Error(s"Could not find $className#$methodName")
  }

  def insert(file: java.io.File, form: HAMRCodeGenForm): Unit = {
    // TODO: insert HAMR config
  }

  def insert(file: java.io.File): Unit = {
    // TODO: insert Logika config
  }

  def run(): Unit = {
    args match {
      case Array("hamr", p, _*) if args.length <= 3 && new java.io.File(p).isFile =>
        if (args.length == 3) {
          init(args(2) == "dark")
        } else {
          init()
        }
        val file = new java.io.File(p).getCanonicalFile
        HAMRCodeGenFormEx.show(file.getParentFile.getAbsolutePath, form => insert(file, form), System.exit(0))
      case Array("logika", p, _*) if args.length <= 3 && new java.io.File(p).isFile =>
        if (args.length == 3) {
          init(args(2) == "dark")
        } else {
          init()
        }
        val file = new java.io.File(p).getCanonicalFile
        val param = new LogikaFormEx.Parameter[Any] {
          override def defaultTimeout: Int = 2000
          override def defaultRLimit: Long = 2000000
          override def defaultSmt2ValidOpts: String = "cvc4,--full-saturate-quant; z3; cvc5,--full-saturate-quant"
          override def defaultSmt2SatOpts: String = "z3"
          override def parseConfigs(nameExePathMap: Map[String, String], isSat: Boolean, options: String): Either[Any, String] = {
            var map = invokeStatic("org.sireum.HashMap", "empty")
            for ((k, v) <- nameExePathMap.toSeq) {
              map = invokeInstance("org.sireum.HashMap", "+", map, (construct("org.sireum.String", k), construct("org.sireum.String", v)))
            }
            val either = invokeStatic("org.sireum.logika.Smt2", "parseConfigs", map, isSat, options)
            val r = if (either.getClass.getName == "org.sireum.Either$Left") Left(invokeInstance("org.sireum.Either$Left", "value", either))
            else Right(invokeInstance("org.sireum.Either$Right", "value", either).toString)
            r
          }
          override def hasSolver(solver: String): Boolean = {
            val map = invokeStatic("org.sireum.logika.Smt2Invoke", "nameExePathMap", sireumHome)
            val key = construct("org.sireum.String", solver)
            val exePathOpt = invokeInstance("org.sireum.HashMap", "get", map, key)
            if (exePathOpt.getClass.getName == "org.sireum.None") return false
            val exePath = invokeInstance("org.sireum.Some", "get", exePathOpt)
            new java.io.File(exePath.toString).exists
          }
        }
        LogikaFormEx.show(param, () => insert(file), System.exit(0))
      case _ =>
        println("Usage: ( hamr | logika ) <file> [ dark | light ]")
        System.exit(0)
    }

  }

  run()
}
