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

object HAMRCodeGenFormEx {
  val red: Color = new Color(0xFF, 0x60, 0x60)
  
  def show(anchorPath: String, insertCallback: HAMRCodeGenForm => Unit, closeCallback: => Unit): Unit = {
    def canWrite(f: java.io.File): Boolean = {
      var file = f
      while (!file.exists()) {
        file = file.getParentFile()
      }
      file.canWrite
    }
    val title = "Configure HAMR Code Generation Options"
    val dialog = new JDialog(new JFrame(title), title, true) {
      override def getInsets: Insets = {
        val s = super.getInsets
        new Insets(s.top + 10, s.left + 10, s.bottom + 10, s.right + 10)
      }
    }
    val f = new HAMRCodeGenForm()
    val labelColor = f.outputLabel.getForeground
    def isValid: Boolean = {
      f.platformComboBox.getSelectedItem.toString match {
        case "JVM" =>
          f.outputLabel.getForeground != red &&
            f.stringSizeLabel.getForeground != red &&
            f.seqSizeLabel.getForeground != red
        case "macOS" | "Linux" | "Cygwin" | "seL4" =>
          f.outputLabel.getForeground != red &&
            f.stringSizeLabel.getForeground != red &&
            f.seqSizeLabel.getForeground != red &&
            f.cOutputLabel.getForeground != red &&
            f.auxLabel.getForeground != red &&
            f.seL4OutputLabel.getForeground != red &&
            f.auxSeL4Label.getForeground != red
        case "seL4_Only" | "seL4_TB" =>
          f.seL4OutputLabel.getForeground != red &&
            f.auxSeL4Label.getForeground != red
      }
    }
    def updateLabel(pred: () => Boolean, label: JLabel): Unit = {
      if (pred()) {
        label.setForeground(labelColor)
      } else {
        label.setForeground(red)
      }
      f.okButton.setEnabled(isValid)
    }
    def addChangeListener(d: Document, f: () => Unit): Unit = {
      d.addDocumentListener(new DocumentListener {
        override def insertUpdate(e: DocumentEvent): Unit = f()
        override def removeUpdate(e: DocumentEvent): Unit = f()
        override def changedUpdate(e: DocumentEvent): Unit = f()
      })
    }
    def isPosLongOpt(lOpt: Option[Long]): Boolean = {
      lOpt match {
        case Some(l) => l > 0
        case _ => false
      }
    }
    val anchor = new java.io.File(anchorPath)
    def updateOutput(): Unit = updateLabel(() => f.outputTextField.getText.nonEmpty && canWrite(new java.io.File(anchor, f.outputTextField.getText)), f.outputLabel)
    def updatePackage(): Unit = updateLabel(() => {
      val text = f.packageTextField.getText
      text.nonEmpty &&
        text.headOption.forall(Character.isJavaIdentifierStart) &&
        text.tail.forall(Character.isJavaIdentifierPart)
    }, f.packageLabel)
    def updateSeqSize(): Unit = updateLabel(() => isPosLongOpt(f.seqSizeTextField.getText.toLongOption), f.seqSizeLabel)
    def updateStringSize(): Unit = updateLabel(() => isPosLongOpt(f.stringSizeTextField.getText.toLongOption), f.stringSizeLabel)
    def updateCOutput(): Unit = updateLabel(() => f.cOutputTextField.getText.nonEmpty && canWrite(new java.io.File(f.cOutputTextField.getText)), f.cOutputLabel)
    def updateAux(): Unit = updateLabel(() => f.auxTextField.getText.isEmpty || new java.io.File(f.auxTextField.getText).exists, f.auxLabel)
    def updateSeL4Output(): Unit = updateLabel(() => f.seL4OutputTextField.getText.nonEmpty && canWrite(new java.io.File(f.seL4OutputTextField.getText)), f.seL4OutputLabel)
    def updateAuxSeL4(): Unit = updateLabel(() => f.auxSeL4TextField.getText.isEmpty || new java.io.File(f.auxSeL4TextField.getText).exists, f.auxSeL4Label)
    updateOutput()
    updatePackage()
    updateSeqSize()
    updateStringSize()
    updateCOutput()
    updateAux()
    updateSeL4Output()
    updateAuxSeL4()
    addChangeListener(f.outputTextField.getDocument, updateOutput _)
    addChangeListener(f.packageTextField.getDocument, updatePackage _)
    addChangeListener(f.seqSizeTextField.getDocument, updateSeqSize _)
    addChangeListener(f.stringSizeTextField.getDocument, updateStringSize _)
    addChangeListener(f.cOutputTextField.getDocument, updateCOutput _)
    addChangeListener(f.auxTextField.getDocument, updateAux _)
    addChangeListener(f.seL4OutputTextField.getDocument, updateSeL4Output _)
    addChangeListener(f.auxSeL4TextField.getDocument, updateAuxSeL4 _)
    f.platformComboBox.addActionListener((_: ActionEvent) => {
      f.platformComboBox.getSelectedItem.toString match {
        case "JVM" =>
          f.outputLabel.setVisible(true)
          f.outputTextField.setVisible(true)
          f.browseOutputButton.setVisible(true)
          f.packageLabel.setVisible(true)
          f.packageTextField.setVisible(true)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(false)
          f.camkesPanel.setVisible(false)
        case "macOS" | "Linux" | "Cygwin" =>
          f.outputLabel.setVisible(true)
          f.outputTextField.setVisible(true)
          f.browseOutputButton.setVisible(true)
          f.packageLabel.setVisible(true)
          f.packageTextField.setVisible(true)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(true)
          f.camkesPanel.setVisible(false)
        case "seL4" =>
          f.outputLabel.setVisible(true)
          f.outputTextField.setVisible(true)
          f.browseOutputButton.setVisible(true)
          f.packageLabel.setVisible(true)
          f.packageTextField.setVisible(true)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(true)
          f.camkesPanel.setVisible(true)
        case "seL4_Only" | "seL4_TB" =>
          f.outputLabel.setVisible(false)
          f.outputTextField.setVisible(false)
          f.browseOutputButton.setVisible(false)
          f.packageLabel.setVisible(false)
          f.packageTextField.setVisible(false)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(false)
          f.camkesPanel.setVisible(true)
      }
    })
    def addFileChooser(button: JButton, tf: JTextField): Unit = {
      button.addActionListener((_: ActionEvent) => {
        val fileChooser = new JFileChooser
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
          tf.setText(new java.io.File(anchorPath).getCanonicalFile.toPath.relativize(fileChooser.getSelectedFile.getCanonicalFile.toPath).toString)
        }
      })
    }
    addFileChooser(f.browseOutputButton, f.outputTextField)
    addFileChooser(f.cOutputBrowseButton, f.cOutputTextField)
    addFileChooser(f.auxBrowseButton, f.auxTextField)
    addFileChooser(f.seL4OutputBrowseButton, f.seL4OutputTextField)
    addFileChooser(f.auxSeL4BrowseButton, f.auxSeL4TextField)
    dialog.add(f.contentPanel)
    dialog.pack()
    f.platformComboBox.setSelectedIndex(0)
    f.bitWidthComboBox.setSelectedItem("32")
    dialog.setLocationRelativeTo(null)
    f.okButton.addActionListener(_ => insertCallback(f))
    f.contentPanel.registerKeyboardAction(_ => {
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
