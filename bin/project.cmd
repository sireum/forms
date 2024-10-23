::/*#! 2> /dev/null                                   #
@ 2>/dev/null # 2>nul & echo off & goto BOF           #
if [ -z "${SIREUM_HOME}" ]; then                      #
  echo "Please set SIREUM_HOME env var"               #
  exit -1                                             #
fi                                                    #
exec "${SIREUM_HOME}/bin/sireum" slang run "$0" "$@"  #
:BOF
setlocal
if not defined SIREUM_HOME (
  echo Please set SIREUM_HOME env var
  exit /B -1
)
"%SIREUM_HOME%\bin\sireum.bat" slang run "%0" %*
exit /B %errorlevel%
::!#*/
// #Sireum

import org.sireum._
import org.sireum.project.ProjectUtil._
import org.sireum.project.Project

val homeDir = Os.slashDir.up.canon

val formsJvm = moduleJvmPub(
  id = "forms",
  baseDir = homeDir,
  jvmDeps = ISZ(),
  jvmIvyDeps = ISZ(
    "com.intellij:forms_rt:"
  ),
  pubOpt = pub(
    desc = "Sireum Forms",
    url = "github.com/sireum/forms",
    licenses = bsd2,
    devs = ISZ(robby)
  )
)

val appJvm = moduleJvmPub(
  id = "forms-app",
  baseDir = homeDir / "app",
  jvmDeps = ISZ(formsJvm.id),
  jvmIvyDeps = ISZ(
    "com.formdev:flatlaf:",
    "com.formdev:flatlaf-intellij-themes:",
    "com.github.Dansoftowner:jSystemThemeDetector:",
    "org.slf4j:slf4j-nop:",
    "org.sireum.kekinian::cli:"
  ),
  pubOpt = None()
)

val project = Project.empty + formsJvm + appJvm

projectCli(Os.cliArgs, project)
