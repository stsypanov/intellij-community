package org.jetbrains.idea.maven.indices;

import org.jetbrains.idea.maven.MavenImportingTestCase;

import java.util.List;

public class MavenProjectIndicesManagerTest extends MavenImportingTestCase {
  private MavenIndicesTestFixture myIndicesFixture;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myIndicesFixture = new MavenIndicesTestFixture(myDir, myProject);
    myIndicesFixture.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    myIndicesFixture.tearDown();
    super.tearDown();
  }

  public void testAutomaticallyAddingAndUpdatingLocalRepository() throws Exception {
    List<MavenIndex> indices = myIndicesFixture.getProjectIndicesManager().getIndices();

    assertEquals(1, indices.size());

    assertEquals(MavenIndex.Kind.LOCAL, indices.get(0).getKind());
    assertTrue(indices.get(0).getRepositoryPathOrUrl().endsWith("local1"));
    assertTrue(myIndicesFixture.getProjectIndicesManager().hasVersion("junit", "junit", "4.0"));
  }

  public void testAutomaticallyRemoteRepositoriesOnProjectUpdate() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    List<MavenIndex> indices = myIndicesFixture.getProjectIndicesManager().getIndices();
    assertEquals(2, indices.size());

    assertTrue(indices.get(0).getRepositoryPathOrUrl().endsWith("local1"));
    assertEquals("http://repo1.maven.org/maven2", indices.get(1).getRepositoryPathOrUrl());
  }

  public void testUpdatingIndicesOnResolution() throws Exception {
    removeFromLocalRepository("junit/junit/4.0");
    myIndicesFixture.getProjectIndicesManager().scheduleUpdate(myIndicesFixture.getProjectIndicesManager().getIndices());

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<dependencies>" +
                  "  <dependency>" +
                  "    <groupId>junit</groupId>" +
                  "    <artifactId>junit</artifactId>" +
                  "    <version>4.0</version>" +
                  "  </dependency>" +
                  "</dependencies>");

    assertFalse(myIndicesFixture.getProjectIndicesManager().hasVersion("junit", "junit", "4.0"));

    resolveProject();

    assertTrue(myIndicesFixture.getProjectIndicesManager().hasVersion("junit", "junit", "4.0"));
  }

  public void testCheckingLocalRepositoryForAbsentIndices() throws Exception {
    myIndicesFixture.tearDown();
    myIndicesFixture = new MavenIndicesTestFixture(myDir, myProject, "local2");
    myIndicesFixture.setUp();

    myIndicesFixture.addToRepository("local1");
    
    assertUnorderedElementsAreEqual(
        myIndicesFixture.getProjectIndicesManager().getGroupIds(), "jmock");

    assertTrue(myIndicesFixture.getProjectIndicesManager().hasGroupId("junit"));
    assertFalse(myIndicesFixture.getProjectIndicesManager().hasGroupId("xxx"));

    assertTrue(myIndicesFixture.getProjectIndicesManager().hasArtifactId("junit", "junit"));
    assertFalse(myIndicesFixture.getProjectIndicesManager().hasArtifactId("junit", "xxx"));
    assertFalse(myIndicesFixture.getProjectIndicesManager().hasArtifactId("xxx", "junit"));

    assertTrue(myIndicesFixture.getProjectIndicesManager().hasVersion("junit", "junit", "4.0"));
    assertFalse(myIndicesFixture.getProjectIndicesManager().hasVersion("junit", "junit", "xxx"));
    assertFalse(myIndicesFixture.getProjectIndicesManager().hasVersion("junit", "xxx", "4.0"));
    assertFalse(myIndicesFixture.getProjectIndicesManager().hasVersion("xxx", "junit", "4.0"));
    //assertUnorderedElementsAreEqual(
    //    myIndicesFixture.getProjectIndicesManager().getGroupIds(), "junit", "jmock");
    //assertUnorderedElementsAreEqual(
    //    myIndicesFixture.getProjectIndicesManager().getArtifactIds("junit"), "junit");
    //assertUnorderedElementsAreEqual(
    //    myIndicesFixture.getProjectIndicesManager().getVersions("junit", "junit"), "4.0");
  }
}
