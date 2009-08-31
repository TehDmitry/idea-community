/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.psi.impl.beanProperties;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.meta.PsiPresentableMetaData;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author peter
*/
public class BeanPropertyElement extends FakePsiElement implements PsiMetaOwner, PsiPresentableMetaData {
  private final PsiMethod myMethod;
  private final String myName;

  public BeanPropertyElement(final PsiMethod method, final String name) {
    myMethod = method;
    myName = name;
  }

  @Nullable
  public PsiType getPropertyType() {
    return PropertyUtil.getPropertyType(myMethod);
  }

  @NotNull
  public PsiMethod getMethod() {
    return myMethod;
  }

  public PsiElement getNavigationElement() {
    return myMethod;
  }

  public PsiManager getManager() {
    return myMethod.getManager();
  }

  public PsiElement getDeclaration() {
    return this;
  }

  @NonNls
  public String getName(PsiElement context) {
    return getName();
  }

  @NotNull
  public String getName() {
    return myName;
  }

  public void init(PsiElement element) {

  }

  public Object[] getDependences() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Nullable
  public Icon getIcon(boolean flags) {
    return BeanProperty.ICON;
  }

  public PsiElement getParent() {
    return myMethod;
  }

  @Nullable
  public PsiMetaData getMetaData() {
    return this;
  }

  public String getTypeName() {
    return IdeBundle.message("bean.property");
  }

  @Nullable
  public Icon getIcon() {
    return getIcon(0);
  }

  public TextRange getTextRange() {
    return TextRange.from(0, 0);
  }
}