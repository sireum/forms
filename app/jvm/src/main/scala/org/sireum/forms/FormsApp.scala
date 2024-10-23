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

import java.net.URLClassLoader

object FormsApp extends App {
  def init(isDark: Boolean = OsThemeDetector.getDetector.isDark): Unit = {
    FlatLaf.setup(if (isDark) new FlatDarkFlatIJTheme else new FlatLightFlatIJTheme)
  }

  def insert(file: java.io.File, form: HAMRCodeGenForm): Unit = {
    // TODO: insert HAMR config
  }

  def insert(file: java.io.File): Unit = {
    // TODO: insert Logika config
  }

  def run(): Unit = {
    def hamr(p: String, cancelCallback: () => Unit = () => System.exit(0)): Unit = {
      val file = new java.io.File(p).getCanonicalFile
      HAMRCodeGenFormEx.show(file.getParentFile.getAbsolutePath, form => insert(file, form), cancelCallback())
    }
    def logika(p: String, cancelCallback: () => Unit = () => System.exit(0)): Unit = {
      val file = new java.io.File(p).getCanonicalFile
      val param = new LogikaFormEx.Parameter[Any] {
        override def defaultTimeout: Int = 2000
        override def defaultRLimit: Long = 2000000
        override def defaultSmt2ValidOpts: String = "cvc4,--full-saturate-quant; z3; cvc5,--full-saturate-quant"
        override def defaultSmt2SatOpts: String = "z3"
        override def parseConfigs(nameExePathMap: Map[String, String], isSat: Boolean, options: String): Either[Any, String] = {
          import org.sireum._
          var map = HashMap.empty[String, String]
          for ((k, v) <- nameExePathMap.toSeq) {
            map = map + k ~> v
          }
          org.sireum.logika.Smt2.parseConfigs(map, isSat, options) match {
            case Either.Left(l) => scala.Left(l)
            case Either.Right(r) => scala.Right(r.toString)
          }
        }
        override def hasSolver(solver: String): Boolean = LogikaFormEx.nameExePathMap.contains(solver)
      }
      LogikaFormEx.show(param, () => insert(file), cancelCallback())
    }
    args match {
      case Array("hamr", p, _*) if args.length <= 3 && new java.io.File(p).isFile =>
        if (args.length == 3) {
          init(args(2) == "dark")
        } else {
          init()
        }
        hamr(p)
      case Array("logika", p, _*) if args.length <= 3 && new java.io.File(p).isFile =>
        if (args.length == 3) {
          init(args(2) == "dark")
        } else {
          init()
        }
        logika(p)
      case Array(p) if new java.io.File(p).isFile =>
        init()
        print("Enter [h]amr, [l]ogika, [d]ark, l[i]ght, [e]xit: ")
        var line = Console.in.readLine().trim()
        var exit = line == "e"
        while (!exit) {
          line match {
            case "h" =>
              line = ""
              hamr(p, () => ())
            case "l" =>
              line = ""
              logika(p, () => ())
            case "d" =>
              line = ""
              init(isDark = true)
            case "i" =>
              line = ""
              init(isDark = false)
            case _ =>
              print("Enter [h]amr, [l]ogika, [d]ark, l[i]ght, [e]xit: ")
              line = Console.in.readLine().trim()
              exit = line == "e"
          }
        }
        System.exit(0)
      case _ =>
        println("Usage: [ hamr | logika ] <file> [ dark | light ]")
        System.exit(0)
    }

  }

  run()
}
