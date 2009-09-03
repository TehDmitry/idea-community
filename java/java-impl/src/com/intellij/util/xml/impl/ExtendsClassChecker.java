/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.util.xml.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.ReflectionCache;
import com.intellij.util.SmartList;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.*;
import com.intellij.util.xml.highlighting.DomCustomAnnotationChecker;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementProblemDescriptor;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class ExtendsClassChecker extends DomCustomAnnotationChecker<ExtendClass>{
  private static final GenericValueReferenceProvider ourProvider = new GenericValueReferenceProvider();

  @NotNull
  public Class<ExtendClass> getAnnotationClass() {
    return ExtendClass.class;
  }

  public List<DomElementProblemDescriptor> checkForProblems(@NotNull final ExtendClass extend, @NotNull final DomElement _element, @NotNull final DomElementAnnotationHolder holder,
                            @NotNull final DomHighlightingHelper helper) {
    if (!(_element instanceof GenericDomValue)) return Collections.emptyList();
    GenericDomValue element = (GenericDomValue)_element;

    final Class genericValueParameter = DomUtil.getGenericValueParameter(element.getDomElementType());
    if (genericValueParameter == null || (!ReflectionCache.isAssignable(genericValueParameter, PsiClass.class) &&
                                           !ReflectionCache.isAssignable(genericValueParameter, PsiType.class))) {
      return Collections.emptyList();
    }

    final Object valueObject = element.getValue();
    PsiClass psiClass = null;

    if (valueObject instanceof PsiClass) {
      psiClass = (PsiClass)valueObject;
    } else if (valueObject instanceof PsiClassType) {
      psiClass = ((PsiClassType)valueObject).resolve();
    }

    if (psiClass != null) {
        return checkExtendClass(element, psiClass, extend.value(),
                                extend.instantiatable(), extend.canBeDecorator(), extend.allowInterface(),
                                extend.allowNonPublic(), extend.allowAbstract(), extend.allowEnum(), holder);
    }
    return Collections.emptyList();
  }

  @NotNull
  public static List<DomElementProblemDescriptor> checkExtendClass(final GenericDomValue element, final PsiClass value, final String name,
                                                                   final boolean instantiatable, final boolean canBeDecorator, final boolean allowInterface,
                                                                   final boolean allowNonPublic,
                                                                   final boolean allowAbstract,
                                                                   final boolean allowEnum,
                                                                   final DomElementAnnotationHolder holder) {
    final Project project = element.getManager().getProject();
    PsiClass extendClass = JavaPsiFacade.getInstance(project).findClass(name, GlobalSearchScope.allScope(project));
    final SmartList<DomElementProblemDescriptor> list = new SmartList<DomElementProblemDescriptor>();
    if (extendClass != null) {
      if (!name.equals(value.getQualifiedName()) && !value.isInheritor(extendClass, true)) {
        String message = DomBundle.message("class.is.not.a.subclass", value.getQualifiedName(), extendClass.getQualifiedName());
        list.add(holder.createProblem(element, message));
      }
    }

    if (instantiatable) {
      if (value.hasModifierProperty(PsiModifier.ABSTRACT)) {
        list.add(holder.createProblem(element, DomBundle.message("class.is.not.concrete", value.getQualifiedName())));
      }
      else if (!allowNonPublic && !value.hasModifierProperty(PsiModifier.PUBLIC)) {
        list.add(holder.createProblem(element, DomBundle.message("class.is.not.public", value.getQualifiedName())));
      }
      else if (!hasDefaultConstructor(value)) {
        if (canBeDecorator) {
          boolean hasConstructor = false;

          for (PsiMethod method : value.getConstructors()) {
            final PsiParameterList psiParameterList = method.getParameterList();
            if (psiParameterList.getParametersCount() != 1) continue;
            final PsiType psiType = psiParameterList.getParameters()[0].getTypeElement().getType();
            if (psiType instanceof PsiClassType) {
              final PsiClass psiClass = ((PsiClassType)psiType).resolve();
              if (psiClass != null && InheritanceUtil.isInheritorOrSelf(psiClass, extendClass, true)) {
                hasConstructor = true;
                break;
              }
            }
          }
          if (!hasConstructor) {
            list.add(holder.createProblem(element, DomBundle.message("class.decorator.or.has.default.constructor", value.getQualifiedName())));
          }
        }
        else {
          list.add(holder.createProblem(element, DomBundle.message("class.has.no.default.constructor", value.getQualifiedName())));
        }
      }
    }
    if (!allowInterface && value.isInterface()) {
      list.add(holder.createProblem(element, DomBundle.message("interface.not.allowed", value.getQualifiedName())));
    }
    if (!allowEnum && value.isEnum()) {
      list.add(holder.createProblem(element, DomBundle.message("enum.not.allowed", value.getQualifiedName())));
    }
    if (!allowAbstract && value.hasModifierProperty(PsiModifier.ABSTRACT) && !value.isInterface()) {
      list.add(holder.createProblem(element, DomBundle.message("abstract.class.not.allowed", value.getQualifiedName())));
    }
    return list;
  }

  public static boolean hasDefaultConstructor(PsiClass clazz) {
    final PsiMethod[] constructors = clazz.getConstructors();
    if (constructors.length > 0) {
      for (PsiMethod cls: constructors) {
        if ((cls.hasModifierProperty(PsiModifier.PUBLIC) || cls.hasModifierProperty(PsiModifier.PROTECTED)) && cls.getParameterList().getParametersCount() == 0) {
          return true;
        }
      }
    } else {
      final PsiClass superClass = clazz.getSuperClass();
      return superClass == null || hasDefaultConstructor(superClass);
    }
    return false;
  }

  public static List<DomElementProblemDescriptor> checkExtendsClassInReferences(final GenericDomValue element, final DomElementAnnotationHolder holder) {
    final Object valueObject = element.getValue();
    if (!(valueObject instanceof PsiClass)) return Collections.emptyList();

    final PsiReference[] references = ourProvider.getReferencesByElement(DomUtil.getValueElement(element), new ProcessingContext());
    for (PsiReference reference : references) {
      if (reference instanceof JavaClassReference) {
        final PsiReferenceProvider psiReferenceProvider = ((JavaClassReference)reference).getProvider();
        final String[] value = psiReferenceProvider instanceof JavaClassReferenceProvider ? JavaClassReferenceProvider.EXTEND_CLASS_NAMES
          .getValue(((JavaClassReferenceProvider)psiReferenceProvider).getOptions()) : null;
        if (value != null && value.length != 0) {
          for (String className : value) {
            final List<DomElementProblemDescriptor> problemDescriptors =
              checkExtendClass(element, ((PsiClass)valueObject), className, false, false, true, false, true, true, holder);
            if (!problemDescriptors.isEmpty()) {
              return problemDescriptors;
            }
          }
        }
      }
    }
    return Collections.emptyList();
  }
}