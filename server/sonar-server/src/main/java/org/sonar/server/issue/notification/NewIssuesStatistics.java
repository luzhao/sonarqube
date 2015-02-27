/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.issue.notification;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.sonar.api.issue.Issue;

import static org.sonar.server.issue.notification.NewIssuesStatistics.METRICS.SEVERITY;
import static org.sonar.server.issue.notification.NewIssuesStatistics.METRICS.TAGS;

public class NewIssuesStatistics {
  // TODO TBE get rid of this magic value !
  private static final String GLOBAL_LOGIN = "#global.login#";
  private final Table<String, METRICS, String> stats = HashBasedTable.create();

  public void add(Issue issue) {
    addForLogin(GLOBAL_LOGIN, issue);
    if (issue.assignee() != null) {
      addForLogin(issue.assignee(), issue);
    }
  }

  private void addForLogin(String login, Issue issue) {
    stats.put(login, SEVERITY, issue.severity());
    for (String tag : issue.tags()) {
      stats.put(login, TAGS, tag);
    }

  }

  public enum METRICS {
    SEVERITY, TAGS, FILE
  }
}
