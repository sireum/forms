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

import java.awt.{Color, Insets}
import java.awt.event.{ActionEvent, KeyEvent}
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.Document
import javax.swing._
import HAMRCodeGenFormEx._

object HAMRCodeGenFormEx {

// BEGIN CodegenOptionStore
  def parseInt(value: String): Option[Int] = {
    try {
      Some(value.toInt)
    } catch {
      case _ : Throwable => None
    }
  }

  case class CodegenOptionStore(var runtimeMonitoring: Boolean = false,
                                val runtimeMonitoringDefault: Boolean = false,

                                var platform: String = "JVM",
                                val platformDefault: String = "JVM",
                                var platformValid: Boolean = true,

                                var outputDir: String = "",
                                val outputDirDefault: String = "",
                                var outputDirValid: Boolean = true,

                                var slangOutputDir: String = "",
                                val slangOutputDirDefault: String = "",
                                var slangOutputDirValid: Boolean = true,

                                var packageName: String = "base",
                                val packageNameDefault: String = "base",
                                var packageNameValid: Boolean = true,

                                var slangAuxCodeDirs: String = "",
                                val slangAuxCodeDirsDefault: String = "",
                                var slangAuxCodeDirsValid: Boolean = true,

                                var slangOutputCDir: String = "",
                                val slangOutputCDirDefault: String = "",
                                var slangOutputCDirValid: Boolean = true,

                                var excludeComponentImpl: Boolean = false,
                                val excludeComponentImplDefault: Boolean = false,

                                var bitWidth: String = "64",
                                val bitWidthDefault: String = "64",
                                var bitWidthValid: Boolean = true,

                                var maxStringSize: String = "100",
                                val maxStringSizeDefault: String = "100",
                                var maxStringSizeValid: Boolean = true,

                                var maxArraySize: String = "100",
                                val maxArraySizeDefault: String = "100",
                                var maxArraySizeValid: Boolean = true,

                                var camkesOutputDir: String = "",
                                val camkesOutputDirDefault: String = "",
                                var camkesOutputDirValid: Boolean = true,

                                var camkesAuxCodeDirs: String = "",
                                val camkesAuxCodeDirsDefault: String = "",
                                var camkesAuxCodeDirsValid: Boolean = true,

                                var strictAadlMode: Boolean = false,
                                val strictAadlModeDefault: Boolean = false,

                                var ros2OutputWorkspaceDir: String = "",
                                val ros2OutputWorkspaceDirDefault: String = "",
                                var ros2OutputWorkspaceDirValid: Boolean = true,

                                var ros2Dir: String = "",
                                val ros2DirDefault: String = "",
                                var ros2DirValid: Boolean = true,

                                var ros2NodesLanguage: String = "Python",
                                val ros2NodesLanguageDefault: String = "Python",
                                var ros2NodesLanguageValid: Boolean = true,

                                var ros2LaunchLanguage: String = "Python",
                                val ros2LaunchLanguageDefault: String = "Python",
                                var ros2LaunchLanguageValid: Boolean = true)

// END CodegenOptionStore

  def addChangeListenerRec(component: JComponent, listener: () => Unit): Unit = {
    component match {
      case component: JRadioButton => component.addActionListener(_ => listener())
      case component: JButton => component.addChangeListener(_ => listener())
      case component: JCheckBox => component.addChangeListener(_ => listener())
      case component: JSpinner => component.addChangeListener(_ => listener())
      case component: JComboBox[_] => component.addActionListener(_ => listener())
      case component: JTextField => component.getDocument.addDocumentListener(new DocumentListener {
        override def insertUpdate(e: DocumentEvent): Unit = listener()
        override def removeUpdate(e: DocumentEvent): Unit = listener()
        override def changedUpdate(e: DocumentEvent): Unit = listener()
      })
      case component: JTextArea => component.getDocument.addDocumentListener(new DocumentListener {
        override def insertUpdate(e: DocumentEvent): Unit = listener()
        override def removeUpdate(e: DocumentEvent): Unit = listener()
        override def changedUpdate(e: DocumentEvent): Unit = listener()
      })
      case _ =>
    }
    for (c <- component.getComponents) {
      c match {
        case c: JComponent => addChangeListenerRec(c, listener)
        case _ =>
      }
    }
  }

  def show(anchorPath: String,
           initialStore: Map[String, CodegenOptionStore],
           insertCallback: String => Unit,
           closeCallback: => Unit): Unit = {

    val title = "Configure HAMR Code Generation Options"
    val dialog = new JDialog(new JFrame(title), title, true) {
      override def getInsets: Insets = {
        val s = super.getInsets
        new Insets(s.top + 10, s.left + 10, s.bottom + 10, s.right + 10)
      }
    }
    val f = new HAMRCodeGenFormEx(anchorPath, () => {
      dialog.pack()

      // the border (as set by the insets) would not repaint after making
      // platform options visible/invisible until you manually resized the
      // jdialog box.  A work-around is to force the jdialog to resize.
      dialog.setSize(dialog.getSize.width - 20, dialog.getSize.height - 20)
      dialog.setSize(dialog.getSize.width + 20, dialog.getSize.height + 20)
    })

    addChangeListenerRec(f.codegenPanel, () => f.okButton.setEnabled(f.isValid() && f.hasChanges()))

    f.init(initialStore)
    dialog.add(f.codegenPanel)
    dialog.pack()
    f.updatePlatformCust()

    dialog.setLocationRelativeTo(null)
    f.okButton.addActionListener(_ =>
      f.buildCommandLineArgs() match {
        case Some(args) => insertCallback(args)
        case _ =>
      }
    )

    f.codegenPanel.registerKeyboardAction(_ => {
      dialog.dispose()
      closeCallback
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW)
    f.cancelButton.addActionListener(_ => {
      dialog.dispose()
      closeCallback
    })
    dialog.setVisible(true)
  }
}

class HAMRCodeGenFormEx(val anchorPath: String, pack: () => Unit) extends HAMRCodeGenForm {
  var initialStore: Map[String, HAMRCodeGenFormEx.CodegenOptionStore] = _
  var viewStore: Map[String, HAMRCodeGenFormEx.CodegenOptionStore] = _

  val red: Color = new Color(0xFF, 0x60, 0x60)
  val fgColor: Color = {
    this.platformLabel.getForeground
  }

  val slangOptions: Seq[JComponent with javax.accessibility.Accessible] = Seq(
    slangOptionsPanel,
    runtimeMonitoring,
    slangOutputDirectoryLabel, slangOutputDir, slangOutputDir,
    slangBasePackageNameLabel, packageName)
  val transpilerOptions: Seq[JComponent with javax.accessibility.Accessible] = Seq(
    transpilerPanel,
    transpilerInnerPanel,
    excludeComponentImpl,
    transpilerBitWidthLabel, bitWidth,
    transpilerMaxSeqSizeLabel, maxArraySize,
    transpilerMaxStringSizeLabel, maxStringSize,
    transpilerCOutputLabel, slangOutputCDir, transpilerCOutputBrowseButton,
    transpilerAuxDirLabel, slangAuxCodeDirs, transpilerAuxBrowseButton)
  val camkesOptions: Seq[JComponent with javax.accessibility.Accessible] = Seq(
    camkesPanel,
    camkesSeL4OutputLabel, camkesOutputDir, camkesSeL4OutputBrowseButton,
    camkesAuxDirSeL4Label, camkesAuxCodeDirs, camkesAuxSeL4BrowseButton)
  val ros2Options: Seq[JComponent with javax.accessibility.Accessible] = Seq(
    ros2OptionsPanel,
    ros2NodesLanguageLabel, ros2NodesLanguage,
    ros2LaunchLanguageLabel, ros2LaunchLanguage,
    ros2WorkspaceDirectoryLabel, ros2OutputWorkspaceDir, ros2Ros2DirectoryButton,
    ros2Ros2DirectoryLabel, ros2Dir, ros2Ros2DirectoryButton)
  val options: Seq[Seq[JComponent with javax.accessibility.Accessible]] = Seq(
    slangOptions, transpilerOptions, camkesOptions, ros2Options)

  def setVisible(components: Seq[JComponent with javax.accessibility.Accessible]*): Unit = {
    for (o <- options; c <- o) {
      c.setVisible(components.contains(o))
    }
  }

  def updatePlatformCust(): Unit = {
    platform.getSelectedItem.toString match {
      case "JVM" => setVisible(slangOptions)
      case "MacOS" | "Linux" | "Cygwin" => setVisible(slangOptions, transpilerOptions)
      case "seL4" => setVisible(slangOptions, transpilerOptions, camkesOptions)
      case "seL4_Only" | "seL4_TB" => setVisible(camkesOptions)
      case "ros2" => setVisible(ros2Options)
    }

    pack()

// BEGIN update ui after platform change
    runtimeMonitoring.setSelected(currentStore.runtimeMonitoring)
    platform.setSelectedItem(currentStore.platform)
    outputDir.setText(currentStore.outputDir)
    slangOutputDir.setText(currentStore.slangOutputDir)
    packageName.setText(currentStore.packageName)
    slangAuxCodeDirs.setText(currentStore.slangAuxCodeDirs)
    slangOutputCDir.setText(currentStore.slangOutputCDir)
    excludeComponentImpl.setSelected(currentStore.excludeComponentImpl)
    bitWidth.setSelectedItem(currentStore.bitWidth)
    maxStringSize.setText(currentStore.maxStringSize)
    maxArraySize.setText(currentStore.maxArraySize)
    camkesOutputDir.setText(currentStore.camkesOutputDir)
    camkesAuxCodeDirs.setText(currentStore.camkesAuxCodeDirs)
    strictAadlMode.setSelected(currentStore.strictAadlMode)
    ros2OutputWorkspaceDir.setText(currentStore.ros2OutputWorkspaceDir)
    ros2Dir.setText(currentStore.ros2Dir)
    ros2NodesLanguage.setSelectedItem(currentStore.ros2NodesLanguage)
    ros2LaunchLanguage.setSelectedItem(currentStore.ros2LaunchLanguage)
// END update ui after platform change
  }

  def currentStore: HAMRCodeGenFormEx.CodegenOptionStore = {
    val selectedPlatform: String = this.platform.getSelectedItem.toString
    return viewStore(selectedPlatform)
  }

  def init(optionStore: Map[String, HAMRCodeGenFormEx.CodegenOptionStore]): Unit = {
    initialStore = optionStore
    viewStore = optionStore
    for (o <- optionStore.keys) {
      viewStore = viewStore + (o -> optionStore(o).copy())
    }

    def updateRuntimeMonitoringCust(): Unit = {}

    def updateSlangOutputDirCust(): Unit = {}

    def updateOutputDirCust(): Unit = {}

    def updatePackageNameCust(): Unit = {}

    def updateSlangAuxCodeDirsCust(): Unit = {}

    def updateSlangOutputCDirCust(): Unit = {}

    def updateExcludeComponentImplCust(): Unit = {}

    def updateBitWidthCust(): Unit = {}

    def updateMaxArraySizeCust(): Unit = {}

    def updateMaxStringSizeCust(): Unit = {}

    def updateCamkesOutputDirCust(): Unit = {}

    def updateCamkesAuxCodeDirsCust(): Unit = {}

    def updateStrictAadlModeCust(): Unit = {}

    def updateRos2DirCust(): Unit = {}

    def updateRos2LaunchLanguageCust(): Unit = {}

    def updateRos2OutputWorkspaceDirCust(): Unit = {}

    def updateRos2NodesLanguageCust(): Unit = {}

// BEGIN init additions
    def updateRuntimeMonitoring(): Unit = {
      currentStore.runtimeMonitoring = runtimeMonitoring.isSelected
      updateRuntimeMonitoringCust()
    }

    def updatePlatform(): Unit = {
      val value = platform.getSelectedItem.toString
      currentStore.platform = value
      if (!(value == "JVM" || value == "Linux" || value == "Cygwin" || value == "MacOS" || value == "seL4" || value == "seL4_Only" || value == "seL4_TB" || value == "Microkit" || value == "ros2")){
        currentStore.platformValid = false
        platform.setToolTipText("value must be one of 'JVM, Linux, Cygwin, MacOS, seL4, seL4_Only, seL4_TB, Microkit, ros2'")
        platform.setForeground(Color.red)
      } else {
        currentStore.platformValid = true
        platform.setForeground(fgColor)
        updatePlatformCust()
      }
    }

    def updateOutputDir(): Unit = {
      val value = outputDir.getText
      currentStore.outputDir = value
      outputDir.setForeground(fgColor)
      updateOutputDirCust()
    }

    def updateSlangOutputDir(): Unit = {
      val value = slangOutputDir.getText
      currentStore.slangOutputDir = value
      slangOutputDir.setForeground(fgColor)
      updateSlangOutputDirCust()
    }

    def updatePackageName(): Unit = {
      val value = packageName.getText
      currentStore.packageName = value
      packageName.setForeground(fgColor)
      updatePackageNameCust()
    }

    def updateSlangAuxCodeDirs(): Unit = {
      val value = slangAuxCodeDirs.getText
      currentStore.slangAuxCodeDirs = value
      slangAuxCodeDirs.setForeground(fgColor)
      updateSlangAuxCodeDirsCust()
    }

    def updateSlangOutputCDir(): Unit = {
      val value = slangOutputCDir.getText
      currentStore.slangOutputCDir = value
      slangOutputCDir.setForeground(fgColor)
      updateSlangOutputCDirCust()
    }

    def updateExcludeComponentImpl(): Unit = {
      currentStore.excludeComponentImpl = excludeComponentImpl.isSelected
      updateExcludeComponentImplCust()
    }

    def updateBitWidth(): Unit = {
      val value = bitWidth.getSelectedItem.toString
      currentStore.bitWidth = value
      parseInt(value) match {
        case Some(i) =>
          if (!(i == 64 || i == 32 || i == 16 || i == 8)) {
            bitWidth.setToolTipText("value must be one of '64, 32, 16, 8'")
            currentStore.bitWidthValid = false
            bitWidth.setForeground(Color.red)
          } else {
            currentStore.bitWidthValid = true
            bitWidth.setForeground(fgColor)
            updateBitWidthCust()
          }
        case _ =>
          currentStore.bitWidthValid = false
          bitWidth.setToolTipText(s"'$value' is not a valid integer")
          bitWidth.setForeground(Color.red)
      }
    }

    def updateMaxStringSize(): Unit = {
      val value = maxStringSize.getText
      currentStore.maxStringSize = value
      parseInt(value) match {
        case Some(i) =>
          currentStore.maxStringSizeValid = true
          currentStore.maxStringSize = value
          maxStringSize.setForeground(fgColor)
          updateMaxStringSizeCust()
        case _ =>
           currentStore.maxStringSizeValid = false
           maxStringSize.setToolTipText(s"'$value' is not a valid integer")
           maxStringSize.setForeground(Color.red)
      }
    }

    def updateMaxArraySize(): Unit = {
      val value = maxArraySize.getText
      currentStore.maxArraySize = value
      parseInt(value) match {
        case Some(i) =>
          currentStore.maxArraySizeValid = true
          currentStore.maxArraySize = value
          maxArraySize.setForeground(fgColor)
          updateMaxArraySizeCust()
        case _ =>
           currentStore.maxArraySizeValid = false
           maxArraySize.setToolTipText(s"'$value' is not a valid integer")
           maxArraySize.setForeground(Color.red)
      }
    }

    def updateCamkesOutputDir(): Unit = {
      val value = camkesOutputDir.getText
      currentStore.camkesOutputDir = value
      camkesOutputDir.setForeground(fgColor)
      updateCamkesOutputDirCust()
    }

    def updateCamkesAuxCodeDirs(): Unit = {
      val value = camkesAuxCodeDirs.getText
      currentStore.camkesAuxCodeDirs = value
      camkesAuxCodeDirs.setForeground(fgColor)
      updateCamkesAuxCodeDirsCust()
    }

    def updateStrictAadlMode(): Unit = {
      currentStore.strictAadlMode = strictAadlMode.isSelected
      updateStrictAadlModeCust()
    }

    def updateRos2OutputWorkspaceDir(): Unit = {
      val value = ros2OutputWorkspaceDir.getText
      currentStore.ros2OutputWorkspaceDir = value
      ros2OutputWorkspaceDir.setForeground(fgColor)
      updateRos2OutputWorkspaceDirCust()
    }

    def updateRos2Dir(): Unit = {
      val value = ros2Dir.getText
      currentStore.ros2Dir = value
      ros2Dir.setForeground(fgColor)
      updateRos2DirCust()
    }

    def updateRos2NodesLanguage(): Unit = {
      val value = ros2NodesLanguage.getSelectedItem.toString
      currentStore.ros2NodesLanguage = value
      if (!(value == "Python" || value == "Cpp")){
        currentStore.ros2NodesLanguageValid = false
        ros2NodesLanguage.setToolTipText("value must be one of 'Python, Cpp'")
        ros2NodesLanguage.setForeground(Color.red)
      } else {
        currentStore.ros2NodesLanguageValid = true
        ros2NodesLanguage.setForeground(fgColor)
        updateRos2NodesLanguageCust()
      }
    }

    def updateRos2LaunchLanguage(): Unit = {
      val value = ros2LaunchLanguage.getSelectedItem.toString
      currentStore.ros2LaunchLanguage = value
      if (!(value == "Python" || value == "Xml")){
        currentStore.ros2LaunchLanguageValid = false
        ros2LaunchLanguage.setToolTipText("value must be one of 'Python, Xml'")
        ros2LaunchLanguage.setForeground(Color.red)
      } else {
        currentStore.ros2LaunchLanguageValid = true
        ros2LaunchLanguage.setForeground(fgColor)
        updateRos2LaunchLanguageCust()
      }
    }
// END init additions

    def addChangeListener(d: Document, f: () => Unit): Unit = {
      d.addDocumentListener(new DocumentListener {
        override def insertUpdate(e: DocumentEvent): Unit = f()

        override def removeUpdate(e: DocumentEvent): Unit = f()

        override def changedUpdate(e: DocumentEvent): Unit = f()
      })
    }

    def addFileChooser(button: JButton, tf: JTextField): Unit = {
      button.addActionListener((_: ActionEvent) => {
        val fileChooser = new JFileChooser
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
          tf.setText(new java.io.File(anchorPath).getCanonicalFile.toPath.relativize(fileChooser.getSelectedFile.getCanonicalFile.toPath).toString)
        }
      })
    }

// BEGIN change listeners
    runtimeMonitoring.addChangeListener(_ => updateRuntimeMonitoring())
    platform.addActionListener((e: ActionEvent) => updatePlatform())
    addChangeListener(outputDir.getDocument, updateOutputDir _)
    addChangeListener(slangOutputDir.getDocument, updateSlangOutputDir _)
    addChangeListener(packageName.getDocument, updatePackageName _)
    addChangeListener(slangAuxCodeDirs.getDocument, updateSlangAuxCodeDirs _)
    addChangeListener(slangOutputCDir.getDocument, updateSlangOutputCDir _)
    excludeComponentImpl.addChangeListener(_ => updateExcludeComponentImpl())
    bitWidth.addActionListener((e: ActionEvent) => updateBitWidth())
    addChangeListener(maxStringSize.getDocument, updateMaxStringSize _)
    addChangeListener(maxArraySize.getDocument, updateMaxArraySize _)
    addChangeListener(camkesOutputDir.getDocument, updateCamkesOutputDir _)
    addChangeListener(camkesAuxCodeDirs.getDocument, updateCamkesAuxCodeDirs _)
    strictAadlMode.addChangeListener(_ => updateStrictAadlMode())
    addChangeListener(ros2OutputWorkspaceDir.getDocument, updateRos2OutputWorkspaceDir _)
    addChangeListener(ros2Dir.getDocument, updateRos2Dir _)
    ros2NodesLanguage.addActionListener((e: ActionEvent) => updateRos2NodesLanguage())
    ros2LaunchLanguage.addActionListener((e: ActionEvent) => updateRos2LaunchLanguage())
// END change listeners


    addFileChooser(outputDirectoryButton, outputDir)
    addFileChooser(slangOutputDirectoryButton, slangOutputDir)
    addFileChooser(transpilerCOutputBrowseButton, slangOutputCDir)
    addFileChooser(transpilerAuxBrowseButton, slangAuxCodeDirs)
    addFileChooser(camkesSeL4OutputBrowseButton, camkesOutputDir)
    addFileChooser(camkesAuxSeL4BrowseButton, camkesAuxCodeDirs)
    addFileChooser(ros2WorkspaceDirectoryButton, ros2OutputWorkspaceDir)
    addFileChooser(ros2Ros2DirectoryButton, ros2Dir)
  }

  def isValidCust: Boolean = {
    return true
  }


// BEGIN hasChanges
  def hasChanges(): Boolean = {
    return (
      initialStore(currentStore.platform).runtimeMonitoring != currentStore.runtimeMonitoring ||
      initialStore(currentStore.platform).platform != currentStore.platform ||
      initialStore(currentStore.platform).outputDir != currentStore.outputDir ||
      initialStore(currentStore.platform).slangOutputDir != currentStore.slangOutputDir ||
      initialStore(currentStore.platform).packageName != currentStore.packageName ||
      initialStore(currentStore.platform).slangAuxCodeDirs != currentStore.slangAuxCodeDirs ||
      initialStore(currentStore.platform).slangOutputCDir != currentStore.slangOutputCDir ||
      initialStore(currentStore.platform).excludeComponentImpl != currentStore.excludeComponentImpl ||
      initialStore(currentStore.platform).bitWidth != currentStore.bitWidth ||
      initialStore(currentStore.platform).maxStringSize != currentStore.maxStringSize ||
      initialStore(currentStore.platform).maxArraySize != currentStore.maxArraySize ||
      initialStore(currentStore.platform).camkesOutputDir != currentStore.camkesOutputDir ||
      initialStore(currentStore.platform).camkesAuxCodeDirs != currentStore.camkesAuxCodeDirs ||
      initialStore(currentStore.platform).strictAadlMode != currentStore.strictAadlMode ||
      initialStore(currentStore.platform).ros2OutputWorkspaceDir != currentStore.ros2OutputWorkspaceDir ||
      initialStore(currentStore.platform).ros2Dir != currentStore.ros2Dir ||
      initialStore(currentStore.platform).ros2NodesLanguage != currentStore.ros2NodesLanguage ||
      initialStore(currentStore.platform).ros2LaunchLanguage != currentStore.ros2LaunchLanguage)
  }
// END hasChanges

// BEGIN isValid
  def isValid(): Boolean = {
    return isValidCust &&
      currentStore.platformValid &&
      currentStore.outputDirValid &&
      currentStore.slangOutputDirValid &&
      currentStore.packageNameValid &&
      currentStore.slangAuxCodeDirsValid &&
      currentStore.slangOutputCDirValid &&
      currentStore.bitWidthValid &&
      currentStore.maxStringSizeValid &&
      currentStore.maxArraySizeValid &&
      currentStore.camkesOutputDirValid &&
      currentStore.camkesAuxCodeDirsValid &&
      currentStore.ros2OutputWorkspaceDirValid &&
      currentStore.ros2DirValid &&
      currentStore.ros2NodesLanguageValid &&
      currentStore.ros2LaunchLanguageValid
  }
// END isValid

// BEGIN update initial store
  def updateInitialStore(): Unit = {
    initialStore = initialStore + (currentStore.platform -> currentStore.copy())
  }
// END update initial store



// BEGIN build command line args
  def buildCommandLineArgs(): Option[String] = {
    if (!isValid()) {
      JOptionPane.showMessageDialog(new JFrame(), "Please correct invalid entries", "",
        JOptionPane.ERROR_MESSAGE);
      return None
    }
    if (!hasChanges()) {
      JOptionPane.showMessageDialog(new JFrame(), "No changes detected", "",
        JOptionPane.INFORMATION_MESSAGE);
      return None
    }

    var args: Seq[String] = Seq()

    if (currentStore.platform == currentStore.platformDefault) {
      args = args :+ "--platform" :+ currentStore.platform
    }

    if (currentStore.runtimeMonitoring != currentStore.runtimeMonitoringDefault) {
       args = args :+ "--runtime-monitoring"
    }
    if (currentStore.platform != currentStore.platformDefault) {
       args = args :+ "--platform" :+ currentStore.platform
    }
    if (currentStore.outputDir != currentStore.outputDirDefault) {
       args = args :+ "--output-dir" :+ currentStore.outputDir
    }
    if (currentStore.slangOutputDir != currentStore.slangOutputDirDefault) {
       args = args :+ "--slang-output-dir" :+ currentStore.slangOutputDir
    }
    if (currentStore.packageName != currentStore.packageNameDefault) {
       args = args :+ "--package-name" :+ currentStore.packageName
    }
    if (currentStore.slangAuxCodeDirs != currentStore.slangAuxCodeDirsDefault) {
       args = args :+ "--aux-code-dirs" :+ currentStore.slangAuxCodeDirs
    }
    if (currentStore.slangOutputCDir != currentStore.slangOutputCDirDefault) {
       args = args :+ "--output-c-dir" :+ currentStore.slangOutputCDir
    }
    if (currentStore.excludeComponentImpl != currentStore.excludeComponentImplDefault) {
       args = args :+ "--exclude-component-impl"
    }
    if (currentStore.bitWidth != currentStore.bitWidthDefault) {
       args = args :+ "--bit-width" :+ currentStore.bitWidth
    }
    if (currentStore.maxStringSize != currentStore.maxStringSizeDefault) {
       args = args :+ "--max-string-size" :+ currentStore.maxStringSize
    }
    if (currentStore.maxArraySize != currentStore.maxArraySizeDefault) {
       args = args :+ "--max-array-size" :+ currentStore.maxArraySize
    }
    if (currentStore.camkesOutputDir != currentStore.camkesOutputDirDefault) {
       args = args :+ "--camkes-output-dir" :+ currentStore.camkesOutputDir
    }
    if (currentStore.camkesAuxCodeDirs != currentStore.camkesAuxCodeDirsDefault) {
       args = args :+ "--camkes-aux-code-dirs" :+ currentStore.camkesAuxCodeDirs
    }
    if (currentStore.strictAadlMode != currentStore.strictAadlModeDefault) {
       args = args :+ "--strict-aadl-mode"
    }
    if (currentStore.ros2OutputWorkspaceDir != currentStore.ros2OutputWorkspaceDirDefault) {
       args = args :+ "--ros2-output-workspace-dir" :+ currentStore.ros2OutputWorkspaceDir
    }
    if (currentStore.ros2Dir != currentStore.ros2DirDefault) {
       args = args :+ "--ros2-dir" :+ currentStore.ros2Dir
    }
    if (currentStore.ros2NodesLanguage != currentStore.ros2NodesLanguageDefault) {
       args = args :+ "--ros2-nodes-language" :+ currentStore.ros2NodesLanguage
    }
    if (currentStore.ros2LaunchLanguage != currentStore.ros2LaunchLanguageDefault) {
       args = args :+ "--ros2-launch-language" :+ currentStore.ros2LaunchLanguage
    }

    updateInitialStore()

    if (args.size == 2) {
      JOptionPane.showMessageDialog(new JFrame(), s"Nothing inserted as those are the default options for ${currentStore.platform}", "",
        JOptionPane.INFORMATION_MESSAGE);
      return None
    } else {
      return Some(args.mkString(" "))
    }
  }
// END build command line args
}