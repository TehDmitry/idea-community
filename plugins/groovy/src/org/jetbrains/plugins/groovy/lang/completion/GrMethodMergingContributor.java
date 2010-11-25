/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.lang.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.psi.*;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;

import java.util.ArrayList;

/**
 * @author Maxim.Medvedev
 */
public class GrMethodMergingContributor extends CompletionContributor {
  @Override
  public AutoCompletionDecision handleAutoCompletionPossibility(AutoCompletionContext context) {
    final CompletionParameters parameters = context.getParameters();

    if (parameters.getCompletionType() != CompletionType.SMART && parameters.getCompletionType() != CompletionType.BASIC) {
      return null;
    }

    boolean needInsertBrace = false;
    boolean needInsertParenth = false;

    final LookupElement[] items = context.getItems();
    if (items.length > 1) {
      String commonName = null;
      LookupElement best = null;
      final ArrayList<PsiMethod> allMethods = new ArrayList<PsiMethod>();
      for (LookupElement item : items) {
        Object o = item.getObject();
        if (o instanceof ResolveResult) {
          o = ((ResolveResult)o).getElement();
        }
        if (item.getUserData(LookupItem.FORCE_SHOW_SIGNATURE_ATTR) != null || !(o instanceof PsiMethod)) {
          return AutoCompletionDecision.SHOW_LOOKUP;
        }

        final PsiMethod method = (PsiMethod)o;
        final JavaChainLookupElement chain = item.as(JavaChainLookupElement.class);
        final String name = method.getName() + "#" + (chain == null ? "" : chain.getQualifier().getLookupString());

        if (commonName != null && !commonName.equals(name)) {
          return AutoCompletionDecision.SHOW_LOOKUP;
        }

        if (hasOnlyClosureParams(method)) {
          needInsertBrace = true;
        }
        else {
          needInsertParenth = true;
        }

        if (needInsertBrace && needInsertParenth) {
          return AutoCompletionDecision.SHOW_LOOKUP;
        }

        if (best == null && method.getParameterList().getParametersCount() > 0) {
          best = item;
        }
        commonName = name;
        allMethods.add(method);
        item.putUserData(JavaCompletionUtil.ALL_METHODS_ATTRIBUTE, allMethods);
      }
      if (best == null) {
        best = items[0];
      }
      return AutoCompletionDecision.insertItem(best);
    }

    return super.handleAutoCompletionPossibility(context);

  }

  private static boolean hasOnlyClosureParams(PsiMethod method) {
    final PsiParameter[] params = method.getParameterList().getParameters();
    for (PsiParameter param : params) {
      final PsiType type = param.getType();
      if (!TypesUtil.typeEqualsToText(type, GrClosableBlock.GROOVY_LANG_CLOSURE)) {
        return false;
      }
    }
    return params.length > 0;
  }
}
