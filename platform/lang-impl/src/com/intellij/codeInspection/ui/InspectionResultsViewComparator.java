// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ui;

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeComparator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiQualifiedNamedElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiUtilCore;

import java.util.Comparator;

public class InspectionResultsViewComparator implements Comparator<InspectionTreeNode> {
  private static final Logger LOG = Logger.getInstance(InspectionResultsViewComparator.class);

  public boolean areEqual(InspectionTreeNode o1, InspectionTreeNode o2) {
    return o1.getClass().equals(o2.getClass()) && compare(o1, o2) == 0;
  }

  @Override
  public int compare(InspectionTreeNode node1, InspectionTreeNode node2) {
    if (node1 instanceof InspectionSeverityGroupNode && node2 instanceof InspectionSeverityGroupNode) {
      final InspectionSeverityGroupNode groupNode1 = (InspectionSeverityGroupNode)node1;
      final InspectionSeverityGroupNode groupNode2 = (InspectionSeverityGroupNode)node2;
      return -groupNode1.getSeverityRegistrar().compare(groupNode1.getSeverityLevel().getSeverity(), groupNode2.getSeverityLevel().getSeverity());
    }
    if (node1 instanceof InspectionSeverityGroupNode) return -1;
    if (node2 instanceof InspectionSeverityGroupNode) return 1;

    if (node1 instanceof InspectionGroupNode && node2 instanceof InspectionGroupNode) {
      return ((InspectionGroupNode)node1).getSubGroup().compareTo(((InspectionGroupNode)node2).getSubGroup());
    }
    if (node1 instanceof InspectionGroupNode) return -1;
    if (node2 instanceof InspectionGroupNode) return 1;

    if (node1 instanceof InspectionNode && node2 instanceof InspectionNode)
      return InspectionsConfigTreeComparator.getDisplayTextToSort(node1.toString())
        .compareToIgnoreCase(InspectionsConfigTreeComparator.getDisplayTextToSort(node2.toString()));
    if (node1 instanceof InspectionNode) return -1;
    if (node2 instanceof InspectionNode) return 1;

    if (node1 instanceof InspectionModuleNode && node2 instanceof InspectionModuleNode) {
      return Comparing.compare(node1.toString(), node2.toString());
    }
    if (node1 instanceof InspectionModuleNode) return -1;
    if (node2 instanceof InspectionModuleNode) return 1;

    if (node1 instanceof InspectionPackageNode && node2 instanceof InspectionPackageNode) {
      return ((InspectionPackageNode)node1).getPackageName().compareToIgnoreCase(((InspectionPackageNode)node2).getPackageName());
    }
    if (node1 instanceof InspectionPackageNode) return -1;
    if (node2 instanceof InspectionPackageNode) return 1;

    if (node1 instanceof RefElementNode && node2 instanceof RefElementNode){   //sort by filename and inside file by start offset
      return compareEntities(((RefElementNode)node1).getElement(), ((RefElementNode)node2).getElement());
    }
    if (node1 instanceof ProblemDescriptionNode && node2 instanceof ProblemDescriptionNode) {
      final CommonProblemDescriptor descriptor1 = ((ProblemDescriptionNode)node1).getDescriptor();
      final CommonProblemDescriptor descriptor2 = ((ProblemDescriptionNode)node2).getDescriptor();
      if (descriptor1 instanceof ProblemDescriptor && descriptor2 instanceof ProblemDescriptor) {
        int diff = ((ProblemDescriptor)descriptor1).getLineNumber() - ((ProblemDescriptor)descriptor2).getLineNumber();
        if (diff != 0) {
          return diff;
        }
        diff = ((ProblemDescriptor)descriptor1).getHighlightType().compareTo(((ProblemDescriptor)descriptor2).getHighlightType());
        if (diff != 0) {
          return diff;
        }
        diff = PsiUtilCore.compareElementsByPosition(((ProblemDescriptor)descriptor1).getStartElement(),
                                                     ((ProblemDescriptor)descriptor2).getStartElement());
        if (diff != 0) {
          return diff;
        }
        diff = PsiUtilCore.compareElementsByPosition(((ProblemDescriptor)descriptor2).getEndElement(),
                                                     ((ProblemDescriptor)descriptor1).getEndElement());
        if (diff != 0) return diff;

        final TextRange range1 = ((ProblemDescriptor)descriptor1).getTextRangeInElement();
        final TextRange range2 = ((ProblemDescriptor)descriptor2).getTextRangeInElement();
        if (range1 != null && range2 != null) {
          diff = range1.getStartOffset() - range2.getStartOffset();
          if (diff != 0) return diff;
          diff = range1.getEndOffset() - range2.getEndOffset();
          if (diff != 0) return diff;
        }
      }
      if (descriptor1 != null && descriptor2 != null) {
        return descriptor1.getDescriptionTemplate().compareToIgnoreCase(descriptor2.getDescriptionTemplate());
      }
      if (descriptor1 == null) return descriptor2 == null ? 0 : -1;
      return 1;
    }

    if (node1 instanceof RefElementNode && node2 instanceof ProblemDescriptionNode) {
      final CommonProblemDescriptor descriptor = ((ProblemDescriptionNode)node2).getDescriptor();
      if (descriptor instanceof ProblemDescriptor) {
        return compareEntity(((RefElementNode)node1).getElement(), ((ProblemDescriptor)descriptor).getPsiElement());
      }
      return compareEntities(((RefElementNode)node1).getElement(), ((ProblemDescriptionNode)node2).getElement());
    }

    if (node2 instanceof RefElementNode && node1 instanceof ProblemDescriptionNode) {
      final CommonProblemDescriptor descriptor = ((ProblemDescriptionNode)node1).getDescriptor();
      if (descriptor instanceof ProblemDescriptor) {
        return -compareEntity(((RefElementNode)node2).getElement(), ((ProblemDescriptor)descriptor).getPsiElement());
      }
      return -compareEntities(((RefElementNode)node2).getElement(), ((ProblemDescriptionNode)node1).getElement());
    }
    if (node1 instanceof InspectionRootNode && node2 instanceof InspectionRootNode) {
      //TODO Dmitry Batkovich: optimization, because only one root node is existed
      return 0;
    }

    LOG.error("node1: " + node1 + ", node2: " + node2);
    return 0;
  }

  private static int compareEntity(final RefEntity entity, final PsiElement element) {
    if (entity instanceof RefElement) {
      final PsiElement psiElement = ((RefElement)entity).getPsiElement();
      if (psiElement != null && element != null) {
        return PsiUtilCore.compareElementsByPosition(psiElement, element);
      }
      if (element == null) return psiElement == null ? 0 : 1;
    }
    if (element instanceof PsiQualifiedNamedElement) {
      return StringUtil.compare(entity.getQualifiedName(), ((PsiQualifiedNamedElement)element).getQualifiedName(), true);
    }
    if (element instanceof PsiNamedElement) {
      return StringUtil.compare(entity.getName(), ((PsiNamedElement)element).getName(), true);
    }
    return -1;
  }

  public static int compareEntities(final RefEntity entity1, final RefEntity entity2) {
    if (entity1 != null && entity2 != null) {
      int cmp = compareEntitiesByName(entity1, entity2);
      if (cmp != 0) return cmp;
    }
    if (entity1 instanceof RefFile && entity2 instanceof RefFile) {
      VirtualFile file1 = ((RefFile)entity1).getPointer().getVirtualFile();
      VirtualFile file2 = ((RefFile)entity2).getPointer().getVirtualFile();
      if (file1 instanceof VirtualFileWithId && file2 instanceof VirtualFileWithId) {
        return ((VirtualFileWithId)file1).getId() - ((VirtualFileWithId)file2).getId();
      }
      int cmp = file1.getName().compareToIgnoreCase(file2.getName());
      if (cmp != 0) return cmp;
      cmp = file1.getPath().compareToIgnoreCase(file2.getPath());
      return cmp;
    }
    if (entity1 instanceof RefElement && entity2 instanceof RefElement) {
      final SmartPsiElementPointer p1 = ((RefElement)entity1).getPointer();
      final SmartPsiElementPointer p2 = ((RefElement)entity2).getPointer();
      if (p1 != null && p2 != null) {
        final VirtualFile file1 = p1.getVirtualFile();
        final VirtualFile file2 = p2.getVirtualFile();

        if (file1 != null && file1.isValid() && file2 != null && file2.isValid()) {
          int cmp = PsiUtilCore.compareElementsByPosition(((RefElement)entity1).getPsiElement(), ((RefElement)entity2).getPsiElement());
          if (cmp != 0) return cmp;
          if (file1 instanceof VirtualFileWithId && file2 instanceof VirtualFileWithId) {
            return ((VirtualFileWithId)file1).getId() - ((VirtualFileWithId)file2).getId();
          }
          return file1.getPath().compareToIgnoreCase(file2.getPath());
        }
        return 0;
      }
      if (p1 != null) return -1;
      if (p2 != null) return 1;
      return 0;
    }
    return entity1 != null ? -1 : entity2 == null ? 0 : 1;
  }

  private static int compareEntitiesByName(RefEntity entity1, RefEntity entity2) {
    final int nameComparing = entity1.getName().compareToIgnoreCase(entity2.getName());
    if (nameComparing != 0) {
      return nameComparing;
    }
    return entity1.getQualifiedName().compareToIgnoreCase(entity2.getQualifiedName());
  }

  private static class InspectionResultsViewComparatorHolder {
    private static final InspectionResultsViewComparator ourInstance = new InspectionResultsViewComparator();
  }

  public static InspectionResultsViewComparator getInstance() {
    return InspectionResultsViewComparatorHolder.ourInstance;
  }
}
