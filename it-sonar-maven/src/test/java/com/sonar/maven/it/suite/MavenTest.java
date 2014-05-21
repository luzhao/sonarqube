/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class MavenTest extends AbstractMavenTest {

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void shouldSupportJarWithoutSources() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/project-with-module-without-sources"))
      .setGoals(cleanSonarGoal());
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.project-with-module-without-sources:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);

    Resource subProject = orchestrator.getServer().getWsClient().find(ResourceQuery.create("com.sonarsource.it.samples.project-with-module-without-sources:without-sources"));
    assertThat(subProject).isNotNull();
  }

  /**
   * See SONAR-594
   */
  @Test
  public void shouldSupportJeeProjects() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/jee"))
      .setGoals(cleanInstallSonarGoal());
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.jee:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);

    List<Resource> modules = orchestrator.getServer().getWsClient().findAll(ResourceQuery.create("com.sonarsource.it.samples.jee:parent").setDepth(-1).setQualifiers("BRC"));
    assertThat(modules).hasSize(4);
  }

  /**
   * See SONAR-222
   */
  @Test
  public void shouldSupportMavenExtensions() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-extensions"))
      .setGoals(cleanSonarGoal());
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-extensions", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);
  }

  /**
   * This test should be splitted. It checks multiple use-cases at the same time : SONAR-518, SONAR-519 and SONAR-593
   */
  @Test
  public void testBadMavenParameters() {
    // should not fail
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-bad-parameters"))
      .setGoals(cleanSonarGoal());
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.maven-bad-parameters:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isGreaterThan(0);
  }

  @Test
  public void shouldAnalyzeMultiModules() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/modules-order"))
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Sonar sonar = orchestrator.getServer().getWsClient();
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:root")).getName()).isEqualTo("Sonar tests - modules order");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:parent")).getName()).isEqualTo("Parent");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_a")).getName()).isEqualTo("Module A");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_b")).getName()).isEqualTo("Module B");

    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_a:src/main/java/HelloA.java")).getName()).isEqualTo("HelloA.java");
      assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_b:src/main/java/HelloB.java")).getName()).isEqualTo("HelloB.java");
    }
  }

  /**
   * See SONAR-2735
   */
  @Test
  public void shouldSupportDifferentDeclarationsForModules() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/modules-declaration"))
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
    Sonar sonar = orchestrator.getServer().getWsClient();

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:root")).getName()).isEqualTo("Root");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_a")).getName()).isEqualTo("Module A");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_b")).getName()).isEqualTo("Module B");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_c")).getName()).isEqualTo("Module C");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_d")).getName()).isEqualTo("Module D");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_e")).getName()).isEqualTo("Module E");

    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_a:src/main/java/HelloA.java")).getName()).isEqualTo("HelloA.java");
      assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_b:src/main/java/HelloB.java")).getName()).isEqualTo("HelloB.java");
      assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_c:src/main/java/HelloC.java")).getName()).isEqualTo("HelloC.java");
      assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_d:src/main/java/HelloD.java")).getName()).isEqualTo("HelloD.java");
      assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_e:src/main/java/HelloE.java")).getName()).isEqualTo("HelloE.java");
    }
  }

  /**
   * See SONAR-2896:
   * if Sonar unable to configure cobertura-maven-plugin, then coverage.xml wouldn't be generated
   */
  @Test
  @Ignore("TODO should be migrated as it uses Cobertura")
  public void testMavenPluginConfiguration() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-plugin-configuration"))
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.java.coveragePlugin", "cobertura");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-plugin-configuration", "coverage"));
    assertThat(project.getMeasureIntValue("coverage")).isGreaterThan(0);
  }

  @Test
  public void build_helper_plugin_should_add_dirs_when_dynamic_analysis() {
    // sonar.phase is no more supported in SQ 4.3
    assumeFalse(orchestrator.getServer().version().isGreaterThanOrEquals("4.3"));
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/many-source-dirs"))
      .setGoals(cleanPackageSonarGoal())
      .setProperty("sonar.dynamicAnalysis", "true");
    orchestrator.executeBuild(build);

    checkBuildHelperFiles();
    checkBuildHelperTestFiles();
  }

  /**
   * There was a regression in 2.9 and 2.10, which was fixed in 2.10.1 and 2.11 - SONAR-2744
   */
  @Test
  public void build_helper_plugin_should_add_dirs_when_static_analysis() {
    // sonar.phase is no more supported in SQ 4.3
    assumeFalse(orchestrator.getServer().version().isGreaterThanOrEquals("4.3"));
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/many-source-dirs"))
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    checkBuildHelperFiles();
  }

  /**
   * See SONAR-3843
   */
  @Test
  public void should_support_shade_with_dependency_reduced_pom_with_clean_install_sonar_goals() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/shade-with-dependency-reduced-pom"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setGoals(cleanInstallSonarGoal());
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isEqualTo(0);
    assertThat(result.getLogs()).doesNotContain(
      "Unable to determine structure of project. Probably you use Maven Advanced Reactor Options, which is not supported by Sonar and should not be used.");
  }

  /**
   * SONAR-4245
   */
  @Test
  public void should_prevent_analysis_of_module_then_project() {
    MavenBuild scan = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample/module_a"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setGoals(cleanSonarGoal());
    orchestrator.executeBuild(scan);

    scan = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setGoals(cleanSonarGoal());
    BuildResult result = orchestrator.executeBuildQuietly(scan);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("The project 'com.sonarsource.it.samples:module_a' is already defined in SonarQube "
      + "but not as a module of project 'com.sonarsource.it.samples:multi-modules-sample'. "
      + "If you really want to stop directly analysing project 'com.sonarsource.it.samples:module_a', "
      + "please first delete it from SonarQube and then relaunch the analysis of project 'com.sonarsource.it.samples:multi-modules-sample'.");
  }

  /**
   * src/main/java is missing
   */
  @Test
  public void maven_project_with_only_test_dir() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-only-test-dir")).setGoals(cleanPackageSonarGoal());
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-only-test-dir", "tests", "files"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(1);
    assertThat(project.getMeasure("files")).isNull();
  }

  /**
   * The property sonar.sources overrides the source dirs as declared in Maven
   */
  @Test
  public void override_sources() {
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("4.2"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-override-sources")).setGoals(sonarGoal());
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-override-sources", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);

    Resource file = orchestrator.getServer().getWsClient().find(ResourceQuery.create("com.sonarsource.it.samples:maven-override-sources:src/main/java2/Hello2.java"));
    assertThat(file).isNotNull();
  }

  /**
   * The property sonar.inclusions overrides the property sonar.sources
   */
  @Test
  public void inclusions_apply_to_source_dirs() {
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("4.2"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/inclusions_apply_to_source_dirs")).setGoals(sonarGoal());
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:inclusions_apply_to_source_dirs", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);

    Resource file = orchestrator.getServer().getWsClient().find(ResourceQuery.create("com.sonarsource.it.samples:inclusions_apply_to_source_dirs:src/main/java/Hello2.java"));
    assertThat(file).isNotNull();
  }

  /**
   * The property sonar.sources has a typo -> fail, like in sonar-runner
   */
  @Test
  public void fail_if_bad_value_of_sonar_sources_property() {
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("4.2"));
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-bad-sources-property")).setGoals(sonarGoal());
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains(
      "java2' does not exist for Maven module com.sonarsource.it.samples:maven-bad-sources-property:jar:1.0-SNAPSHOT. Please check the property sonar.sources");
  }

  /**
   * The property sonar.sources has a typo -> fail, like in sonar-runner
   */
  @Test
  public void fail_if_bad_value_of_sonar_tests_property() {
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("4.2"));
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-bad-tests-property")).setGoals(sonarGoal());
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains(
      "java2' does not exist for Maven module com.sonarsource.it.samples:maven-bad-tests-property:jar:1.0-SNAPSHOT. Please check the property sonar.tests");
  }

  private void checkBuildHelperFiles() {
    Resource project = getResource("com.sonarsource.it.samples:many-source-dirs");
    assertThat(project).isNotNull();
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      assertThat(getResource("com.sonarsource.it.samples:many-source-dirs:src/main/java/FirstClass.java")).isNotNull();
      assertThat(getResource("com.sonarsource.it.samples:many-source-dirs:src/main/java2/SecondClass.java")).isNotNull();
    } else {
      assertThat(getResource("com.sonarsource.it.samples:many-source-dirs:[default].FirstClass")).isNotNull();
      assertThat(getResource("com.sonarsource.it.samples:many-source-dirs:[default].SecondClass")).isNotNull();
    }
  }

  private void checkBuildHelperTestFiles() {
    Resource project = getResource("com.sonarsource.it.samples:many-source-dirs");
    assertThat(project).isNotNull();
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "files", "tests"));
  }

}