package org.sireum.forms

import org.sireum.Cli
import org.sireum._

object HAMRUtil {


// BEGIN build option store
  def decasePlatform(e: Cli.SireumHamrSysmlCodegenHamrPlatform.Type): Predef.String = {
    e match {
      case Cli.SireumHamrSysmlCodegenHamrPlatform.JVM => "JVM"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.Linux => "Linux"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.Cygwin => "Cygwin"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.MacOS => "MacOS"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.SeL4 => "seL4"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.SeL4_Only => "seL4_Only"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.SeL4_TB => "seL4_TB"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.Microkit => "Microkit"
      case Cli.SireumHamrSysmlCodegenHamrPlatform.Ros2 => "ros2"
    }
  }
  def decaseRos2NodesLanguage(e: Cli.SireumHamrSysmlCodegenNodesCodeLanguage.Type): Predef.String = {
    e match {
      case Cli.SireumHamrSysmlCodegenNodesCodeLanguage.Python => "Python"
      case Cli.SireumHamrSysmlCodegenNodesCodeLanguage.Cpp => "Cpp"
    }
  }
  def decaseRos2LaunchLanguage(e: Cli.SireumHamrSysmlCodegenLaunchCodeLanguage.Type): Predef.String = {
    e match {
      case Cli.SireumHamrSysmlCodegenLaunchCodeLanguage.Python => "Python"
      case Cli.SireumHamrSysmlCodegenLaunchCodeLanguage.Xml => "Xml"
    }
  }
  def buildHamrOptionStore(o: Cli.SireumHamrSysmlCodegenOption): HAMRCodeGenFormEx.CodegenOptionStore = {
    val store = HAMRCodeGenFormEx.CodegenOptionStore()
    store.runtimeMonitoring = o.runtimeMonitoring
    store.platform = decasePlatform(o.platform)
    store.outputDir = if (o.outputDir.nonEmpty) o.outputDir.get.native else ""
    store.slangOutputDir = if (o.slangOutputDir.nonEmpty) o.slangOutputDir.get.native else ""
    store.packageName = if (o.packageName.nonEmpty) o.packageName.get.native else ""
    store.slangAuxCodeDirs = st"""${(o.slangAuxCodeDirs, Os.pathSep)}""".render.native
    store.slangOutputCDir = if (o.slangOutputCDir.nonEmpty) o.slangOutputCDir.get.native else ""
    store.excludeComponentImpl = o.excludeComponentImpl
    store.bitWidth = o.bitWidth.string.native
    store.maxStringSize = o.maxStringSize.string.native
    store.maxArraySize = o.maxArraySize.string.native
    store.sel4OutputDir = if (o.sel4OutputDir.nonEmpty) o.sel4OutputDir.get.native else ""
    store.sel4AuxCodeDirs = st"""${(o.sel4AuxCodeDirs, Os.pathSep)}""".render.native
    store.strictAadlMode = o.strictAadlMode
    store.ros2OutputWorkspaceDir = if (o.ros2OutputWorkspaceDir.nonEmpty) o.ros2OutputWorkspaceDir.get.native else ""
    store.ros2Dir = if (o.ros2Dir.nonEmpty) o.ros2Dir.get.native else ""
    store.ros2NodesLanguage = decaseRos2NodesLanguage(o.ros2NodesLanguage)
    store.ros2LaunchLanguage = decaseRos2LaunchLanguage(o.ros2LaunchLanguage)
    store.invertTopicBinding = o.invertTopicBinding
    return store
  }
// END build option store
}
