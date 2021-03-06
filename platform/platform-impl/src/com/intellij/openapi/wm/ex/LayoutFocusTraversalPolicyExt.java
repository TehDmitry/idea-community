/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.wm.ex;

import javax.swing.*;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
public class LayoutFocusTraversalPolicyExt extends LayoutFocusTraversalPolicy{
  /**
   * Overriden default component
   */
  private static JComponent myOverridenDefaultComponent;

  /**
   * !!!!! CAUTION !!!!!
   * !!!!! CAUTION !!!!!
   * !!!!! CAUTION !!!!!
   *
   * THIS IS AN "ABSOLUTELY-GURU METHOD".
   * NOBODY SHOULD ADD OTHER USAGES OF IT :)
   * ONLY ANTON AND VOVA ARE PERMITTED TO USE THIS METHOD!!!
   *
   * !!!!! CAUTION !!!!!
   * !!!!! CAUTION !!!!!
   * !!!!! CAUTION !!!!!
   *
   * This is absolutely guru method. Please do not use it without deep understanding
   * of Swing concepts, especially Swing focus management. <b>do not forget to clear
   * this "overiden" component!</b>
   */
  public static void setOverridenDefaultComponent(final JComponent overridenDefaultComponent) {
    myOverridenDefaultComponent = overridenDefaultComponent;
  }

// Made non-final for Fabrique
  public Component getDefaultComponent(final Container focusCycleRoot) {
    if (myOverridenDefaultComponent != null) {
      return myOverridenDefaultComponent;
    }
    return getDefaultComponentImpl(focusCycleRoot);
  }

  protected Component getDefaultComponentImpl(final Container focusCycleRoot) {
    return super.getDefaultComponent(focusCycleRoot);
  }

  public Component getFirstComponent(final Container focusCycleRoot) {
    if (myOverridenDefaultComponent != null) {
      return myOverridenDefaultComponent;
    }
    return getFirstComponentImpl(focusCycleRoot);
  }

  protected Component getFirstComponentImpl(final Container focusCycleRoot) {
    return super.getFirstComponent(focusCycleRoot);
  }

  public Component getLastComponent(final Container focusCycleRoot) {
    if (myOverridenDefaultComponent != null) {
      return myOverridenDefaultComponent;
    }
    return getLastComponentImpl(focusCycleRoot);
  }

  protected Component getLastComponentImpl(final Container focusCycleRoot) {
    return super.getLastComponent(focusCycleRoot);
  }

  public Component getComponentAfter(final Container focusCycleRoot, final Component aComponent) {
    if (myOverridenDefaultComponent != null) {
      return myOverridenDefaultComponent;
    }
    return getComponentAfterImpl(focusCycleRoot, aComponent);
  }

  protected Component getComponentAfterImpl(final Container focusCycleRoot, final Component aComponent) {
    return super.getComponentAfter(focusCycleRoot, aComponent);
  }

  public Component getComponentBefore(final Container focusCycleRoot, final Component aComponent) {
    if (myOverridenDefaultComponent != null) {
      return myOverridenDefaultComponent;
    }
    return getComponentBeforeImpl(focusCycleRoot, aComponent);
  }

  protected Component getComponentBeforeImpl(final Container focusCycleRoot, final Component aComponent) {
    return super.getComponentBefore(focusCycleRoot, aComponent);
  }
}
