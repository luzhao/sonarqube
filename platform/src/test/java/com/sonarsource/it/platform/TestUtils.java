/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonarsource.it.platform;

import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.MavenLocation;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;

public class TestUtils {

  public static final String BATCH_JVM_OPTS = "-server -Xmx512m -XX:MaxPermSize=160m";

  public static void addAllCompatiblePlugins(OrchestratorBuilder builder) {
    org.sonar.updatecenter.common.Version sonarVersion = org.sonar.updatecenter.common.Version.create(builder.getSonarVersion());
    builder.getUpdateCenter().setInstalledSonarVersion(sonarVersion);
    for (Plugin p : builder.getUpdateCenter().findAllCompatiblePlugins()) {
      Release r = p.getLastCompatible(sonarVersion);
      builder.setOrchestratorProperty(p.getKey() + "Version", r.getVersion().toString());
      builder.addPlugin(p.getKey());
      if ("cobol".equals(p.getKey())) {
        builder.addPlugin(MavenLocation.of("com.sonarsource.cobol", "custom-cobol-sample-plugin", r.getVersion().toString()));
      }
    }
  }

  public static void activateLicenses(OrchestratorBuilder builder) {
    builder
      .activateLicense("abap")
      .activateLicense("cobol")
      .activateLicense("cpp")
      .activateLicense("devcockpit")
      .activateLicense("natural")
      .activateLicense("pacbase")
      .activateLicense("pli")
      .activateLicense("plsql")
      .activateLicense("report")
      .activateLicense("rpg")
      .activateLicense("sqale")
      .activateLicense("vb")
      .activateLicense("vbnet")
      .activateLicense("views");
  }

}