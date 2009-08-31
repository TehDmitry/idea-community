/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Feb 7, 2002
 * Time: 1:25:41 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.dataFlow.instructions;

import com.intellij.codeInspection.dataFlow.DataFlowRunner;
import com.intellij.codeInspection.dataFlow.DfaInstructionState;
import com.intellij.codeInspection.dataFlow.DfaMemoryState;
import com.intellij.codeInspection.dataFlow.InstructionVisitor;
import com.intellij.codeInspection.dataFlow.value.DfaUnknownValue;
import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.psi.PsiExpression;
import org.jetbrains.annotations.NotNull;

public class PushInstruction extends Instruction {
  private final DfaValue myValue;
  private final PsiExpression myPlace;

  public PushInstruction(DfaValue value, PsiExpression place) {
    myValue = value != null ? value : DfaUnknownValue.getInstance();
    myPlace = place;
  }

  @NotNull
  public DfaValue getValue() {
    return myValue;
  }

  public PsiExpression getPlace() {
    return myPlace;
  }

  @Override
  public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
    return visitor.visitPush(this, runner, stateBefore);
  }

  public String toString() {
    return "PUSH " + myValue;
  }
}