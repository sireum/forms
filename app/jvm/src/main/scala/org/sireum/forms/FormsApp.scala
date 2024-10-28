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
    import org.sireum._
    def toSmt2Configs(isSat: B, cs: Array[Predef.String]): ISZ[logika.Smt2Config] = {
      var r = ISZ[logika.Smt2Config]()
      for (c <- cs) {
        val opts = c.split(',')
        r = r :+ logika.Smt2Config(isSat, opts.head.trim, "", ISZ((for (t <- opts.tail.toSeq) yield String(t.trim)): _*))
      }
      r
    }
    val maxCores = Runtime.getRuntime.availableProcessors
    val map = toSireum(LogikaFormEx.nameExePathMap)
    var config = logika.options.OptionsUtil.toConfig(
      server.service.AnalysisService.defaultConfig, maxCores, "default", map, "").left
    if (LogikaFormEx.backgroundAnalysis) {
      config = config(background = logika.Config.BackgroundMode.Save)
    } else {
      config = config(background = logika.Config.BackgroundMode.Disabled)
    }
    config = config(timeoutInMs = LogikaFormEx.timeout)
    config = config(rlimit = LogikaFormEx.rlimit)
    config = config(sat = LogikaFormEx.checkSat)
    config = config(atRewrite = LogikaFormEx.hintAtRewrite)
    config = config(atLinesFresh = LogikaFormEx.hintLinesFresh)
    config = config(loopBound = LogikaFormEx.loopBound)
    config = config(callBound = LogikaFormEx.callBound)
    config = config(useReal = LogikaFormEx.useReal)
    config = config(fpRoundingMode = LogikaFormEx.fpRoundingMode)
    config = config(smt2Configs = toSmt2Configs(F, LogikaFormEx.smt2ValidOpts.split(';')) ++
      toSmt2Configs(T, LogikaFormEx.smt2SatOpts.split(';')))
    config = config(smt2Caching = LogikaFormEx.smt2Cache)
    config = config(simplifiedQuery = LogikaFormEx.smt2Simplify)
    config = config(branchPar = logika.Config.BranchPar.byName(LogikaFormEx.branchPar).get)
    config = config(parCores = LogikaFormEx.branchParCores)
    config = config(splitIf = LogikaFormEx.splitConds)
    config = config(splitMatch = LogikaFormEx.splitMatchCases)
    config = config(splitContract = LogikaFormEx.splitContractCases)
    config = config(splitAll = LogikaFormEx.splitConds | LogikaFormEx.splitMatchCases | LogikaFormEx.splitContractCases)
    config = config(interpContracts = LogikaFormEx.interpContracts)
    config = config(strictPureMode = logika.Config.StrictPureMode.byName(LogikaFormEx.strictPureMode).get)
    config = config(rawInscription = LogikaFormEx.rawInscription)
    config = config(elideEncoding = LogikaFormEx.elideEncoding)
    config = config(transitionCache = LogikaFormEx.transitionCache)
    config = config(patternExhaustive = LogikaFormEx.patternExhaustive)
    config = config(pureFun = LogikaFormEx.pureFun)
    config = config(detailedInfo = LogikaFormEx.detailedInfo)
    config = config(satTimeout = LogikaFormEx.satTimeout)
    config = config(isAuto = LogikaFormEx.auto)
    config = config(searchPc = LogikaFormEx.searchPc)
    config = config(rwTrace = LogikaFormEx.rwTrace)
    config = config(rwMax = LogikaFormEx.rwMax)
    config = config(rwPar = LogikaFormEx.rwPar)
    config = config(rwEvalTrace = LogikaFormEx.rwEvalTrace)
    val p = Os.path(file.getCanonicalPath)
    var lines = ISZ(p.readLineStream.take(1).string)
    lines = lines :+ logika.options.OptionsUtil.fromConfig(p.ext, maxCores, map, config)
    lines = lines ++ p.readLineStream.drop(1).toISZ
    p.writeOver(st"${(lines, Os.lineSep)}".render)
  }

  def toSireum(nameExePathMap: Predef.Map[Predef.String, Predef.String]): org.sireum.HashMap[org.sireum.String, org.sireum.String] = {
    var map = org.sireum.HashMap.empty[org.sireum.String, org.sireum.String]
    for ((k, v) <- nameExePathMap.toSeq) {
      map = map + (org.sireum.String(k), org.sireum.String(v))
    }
    map
  }

  def run(): Unit = {
    def hamr(p: String, cancelCallback: () => Unit = () => System.exit(0)): Unit = {
      val file = new java.io.File(p).getCanonicalFile
      HAMRCodeGenFormEx.show(file.getParentFile.getAbsolutePath, form => insert(file, form), cancelCallback())
    }
    def logikaForm(p: String, cancelCallback: () => Unit = () => System.exit(0)): Unit = {
      import org.sireum._
      def smt2configString(isSat: B, cs: ISZ[logika.Smt2Config]): Predef.String = {
        st"${(for (c <- cs if isSat == c.isSat) yield st"${(c.name +: c.opts, ",")}", "; ")}".render.value
      }
      def updateLogikaForm(config: logika.Config): Unit = {
        config.background match {
          case logika.Config.BackgroundMode.Save =>
            LogikaFormEx.backgroundAnalysis = true
          case logika.Config.BackgroundMode.Type =>
            LogikaFormEx.backgroundAnalysis = true
          case logika.Config.BackgroundMode.Disabled =>
            LogikaFormEx.backgroundAnalysis = false
        }
        LogikaFormEx.timeout = config.timeoutInMs.toInt
        LogikaFormEx.rlimit = config.rlimit.toLong
        LogikaFormEx.checkSat = config.sat
        LogikaFormEx.hintAtRewrite = config.atRewrite
        LogikaFormEx.hintLinesFresh = config.atLinesFresh
        LogikaFormEx.loopBound = config.loopBound.toInt
        LogikaFormEx.callBound = config.callBound.toInt
        LogikaFormEx.useReal = config.useReal
        LogikaFormEx.fpRoundingMode = config.fpRoundingMode.value
        LogikaFormEx.smt2ValidOpts = smt2configString(F, config.smt2Configs)
        LogikaFormEx.smt2SatOpts = smt2configString(T, config.smt2Configs)
        LogikaFormEx.smt2Cache = config.smt2Caching
        LogikaFormEx.smt2Seq = config.smt2Seq
        LogikaFormEx.smt2Simplify = config.simplifiedQuery
        LogikaFormEx.branchPar = config.branchPar.toString
        LogikaFormEx.branchParCores = config.parCores.toInt
        LogikaFormEx.splitConds = config.splitIf | config.splitAll
        LogikaFormEx.splitMatchCases = config.splitMatch | config.splitAll
        LogikaFormEx.splitContractCases = config.splitContract | config.splitAll
        LogikaFormEx.interpContracts = config.interpContracts
        LogikaFormEx.strictPureMode = config.strictPureMode.toString
        LogikaFormEx.infoFlow = false
        LogikaFormEx.rawInscription = config.rawInscription
        LogikaFormEx.elideEncoding = config.elideEncoding
        LogikaFormEx.transitionCache = config.transitionCache
        LogikaFormEx.patternExhaustive = config.patternExhaustive
        LogikaFormEx.pureFun = config.pureFun
        LogikaFormEx.detailedInfo = config.detailedInfo
        LogikaFormEx.satTimeout = config.satTimeout
        LogikaFormEx.auto = config.isAuto
        LogikaFormEx.searchPc = config.searchPc
        LogikaFormEx.rwTrace = config.rwTrace
        LogikaFormEx.rwMax = config.rwMax.toInt
        LogikaFormEx.rwPar = config.rwPar
        LogikaFormEx.rwEvalTrace = config.rwEvalTrace
      }
      val file = Os.path(p)
      val rep = message.Reporter.create
      val map = toSireum(LogikaFormEx.nameExePathMap)
      val maxCores = Runtime.getRuntime.availableProcessors
      val config = logika.options.OptionsUtil.mineConfig(logika.options.OptionsUtil.toConfig(
        server.service.AnalysisService.defaultConfig, maxCores, "default", map, "").left,
        maxCores, "Logika Configurator", map, file.read, None(), rep)
      updateLogikaForm(if (rep.hasError) server.service.AnalysisService.defaultConfig else config)
      val param = new LogikaFormEx.Parameter[Any] {
        override def defaultTimeout: Int = 2000
        override def defaultRLimit: Long = 2000000
        override def defaultSmt2ValidOpts: Predef.String = "cvc4,--full-saturate-quant; z3; cvc5,--full-saturate-quant"
        override def defaultSmt2SatOpts: Predef.String = "z3"
        override def parseConfigs(nameExePathMap: Predef.Map[Predef.String, Predef.String], isSat: Boolean, options: Predef.String): scala.Either[Any, Predef.String] = {
          org.sireum.logika.Smt2.parseConfigs(toSireum(nameExePathMap), isSat, options) match {
            case Either.Left(l) => scala.Left(l)
            case Either.Right(r) => scala.Right(r.toString)
          }
        }
        override def hasSolver(solver: Predef.String): Boolean = LogikaFormEx.nameExePathMap.contains(solver)
      }
      LogikaFormEx.show(param, () => insert(new java.io.File(p).getCanonicalFile), cancelCallback())
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
        logikaForm(p)
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
              logikaForm(p, () => ())
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
