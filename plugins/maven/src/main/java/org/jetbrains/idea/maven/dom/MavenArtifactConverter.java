package org.jetbrains.idea.maven.dom;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.utils.MavenArtifactUtil;
import org.jetbrains.idea.maven.utils.MavenId;
import org.jetbrains.idea.maven.dom.model.Dependency;
import org.jetbrains.idea.maven.dom.model.MavenParent;
import org.jetbrains.idea.maven.dom.model.Plugin;
import org.jetbrains.idea.maven.indices.MavenProjectIndicesManager;
import org.jetbrains.idea.maven.project.MavenProjectModel;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class MavenArtifactConverter extends ResolvingConverter<String> {
  public String fromString(@Nullable @NonNls String s, ConvertContext context) {
    if (s == null) return null;

    Dependency dep = MavenArtifactConverterHelper.getMavenDependency(context);
    if (dep != null) {
      if ("system".equals(dep.getScope().getStringValue())) {
        return s;
      }
    }

    Project p = getProject(context);
    MavenProjectIndicesManager manager = MavenProjectIndicesManager.getInstance(p);
    MavenId id = MavenArtifactConverterHelper.getId(context);

    Plugin plugin = MavenArtifactConverterHelper.getMavenPlugin(context);
    if (StringUtil.isEmpty(id.groupId) && plugin != null) {
      for (String each : MavenArtifactUtil.DEFAULT_GROUPS) {
        id.groupId = each;
        if (isValid(p, manager, id, context)) return s;
      }
      return null;
    }
    else {
      return isValid(p, manager, id, context) ? s : null;
    }
  }

  protected abstract boolean isValid(Project project, MavenProjectIndicesManager manager, MavenId id, ConvertContext context);

  public String toString(@Nullable String s, ConvertContext context) {
    return s;
  }

  @NotNull
  public Collection<String> getVariants(ConvertContext context) {
    Project p = getProject(context);
    MavenProjectIndicesManager manager = MavenProjectIndicesManager.getInstance(p);
    MavenId id = MavenArtifactConverterHelper.getId(context);

    Plugin plugin = MavenArtifactConverterHelper.getMavenPlugin(context);
    if (StringUtil.isEmpty(id.groupId) && plugin != null) {
      Set<String> result = new HashSet<String>();
      for (String each : MavenArtifactUtil.DEFAULT_GROUPS) {
        id.groupId = each;
        result.addAll(getVariants(p, manager, id, context));
      }
      return result;
    }

    return getVariants(p, manager, id, context);
  }

  protected abstract Set<String> getVariants(Project project, MavenProjectIndicesManager manager, MavenId id, ConvertContext context);

  @Override
  public PsiElement resolve(String o, ConvertContext context) {
    Project p = getProject(context);
    MavenProjectsManager manager = MavenProjectsManager.getInstance(p);

    PsiFile f = getSpecifiedFile(context);
    if (f != null) return f;

    MavenId id = MavenArtifactConverterHelper.getId(context);

    MavenProjectModel project = manager.findProject(id);
    if (project != null) {
      return PsiManager.getInstance(p).findFile(project.getFile());
    }

    File file = resolveArtifactFile(context, manager, id);
    if (file != null) {
      VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
      if (vf != null) {
        return PsiManager.getInstance(p).findFile(vf);
      }
    }

    return super.resolve(o, context);
  }

  private Project getProject(ConvertContext context) {
    return context.getFile().getProject();
  }

  private PsiFile getSpecifiedFile(ConvertContext context) {
    MavenParent parent = MavenArtifactConverterHelper.getMavenParent(context);
    if (parent != null) {
      return parent.getRelativePath().getValue();
    }

    Dependency dep = MavenArtifactConverterHelper.getMavenDependency(context);
    if (dep != null) {
      return dep.getSystemPath().getValue();
    }

    return null;
  }

  private File resolveArtifactFile(ConvertContext context, MavenProjectsManager manager, MavenId id) {
    Plugin plugin = MavenArtifactConverterHelper.getMavenPlugin(context);
    if (plugin != null) {
      return MavenArtifactUtil.getArtifactFile(manager.getLocalRepository(), id.groupId, id.artifactId, id.version, "pom");
    }

    String relPath = ("" + id.groupId).replace(".", "/");
    relPath += "/" + id.artifactId;
    relPath += "/" + id.version;
    relPath += "/" + id.artifactId + "-" + id.version + ".pom";

    return new File(manager.getLocalRepository(), relPath);
  }

  @Override
  public LocalQuickFix[] getQuickFixes(ConvertContext context) {
    return ArrayUtil.append(super.getQuickFixes(context), new MyUpdateIndicesFix());
  }

  private class MyUpdateIndicesFix implements LocalQuickFix {
    @NotNull
    public String getFamilyName() {
      return MavenDomBundle.message("inspection.group");
    }

    @NotNull
    public String getName() {
      return MavenDomBundle.message("fix.update.indices");
    }

    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      MavenProjectIndicesManager.getInstance(project).scheduleUpdateAll();
    }
  }
}