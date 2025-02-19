/*
 * SonarLint Language Server
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.ls.connected;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBinding;
import org.sonarsource.sonarlint.core.tracking.CachingIssueTracker;
import org.sonarsource.sonarlint.core.tracking.CachingIssueTrackerImpl;
import org.sonarsource.sonarlint.core.tracking.InMemoryIssueTrackerCache;
import org.sonarsource.sonarlint.core.tracking.IssueTrackable;
import org.sonarsource.sonarlint.core.tracking.IssueTrackerCache;
import org.sonarsource.sonarlint.core.tracking.Trackable;
import org.sonarsource.sonarlint.ls.settings.ServerConnectionSettings;

import static java.util.function.Predicate.not;

public class ServerIssueTrackerWrapper {

  private final ConnectedSonarLintEngine engine;
  private final ServerConnectionSettings.EndpointParamsAndHttpClient endpointParamsAndHttpClient;
  private final ProjectBinding projectBinding;

  private final IssueTrackerCache issueTrackerCache;
  private final CachingIssueTracker cachingIssueTracker;
  private final org.sonarsource.sonarlint.core.tracking.ServerIssueTracker tracker;

  ServerIssueTrackerWrapper(ConnectedSonarLintEngine engine, ServerConnectionSettings.EndpointParamsAndHttpClient endpointParamsAndHttpClient, ProjectBinding projectBinding) {
    this.engine = engine;
    this.endpointParamsAndHttpClient = endpointParamsAndHttpClient;
    this.projectBinding = projectBinding;

    this.issueTrackerCache = new InMemoryIssueTrackerCache();
    this.cachingIssueTracker = new CachingIssueTrackerImpl(issueTrackerCache);
    this.tracker = new org.sonarsource.sonarlint.core.tracking.ServerIssueTracker(cachingIssueTracker);
  }

  public void matchAndTrack(String filePath, Collection<Issue> issues, IssueListener issueListener, boolean shouldFetchServerIssues) {
    if (issues.isEmpty()) {
      issueTrackerCache.put(filePath, Collections.emptyList());
      return;
    }

    cachingIssueTracker.matchAndTrackAsNew(filePath, toTrackables(issues));
    if (shouldFetchServerIssues) {
      tracker.update(endpointParamsAndHttpClient.getEndpointParams(), endpointParamsAndHttpClient.getHttpClient(), engine, projectBinding, Collections.singleton(filePath), true);
    } else {
      tracker.update(engine, projectBinding, Collections.singleton(filePath));
    }

    issueTrackerCache.getLiveOrFail(filePath).stream()
      .filter(not(Trackable::isResolved))
      .forEach(trackable -> issueListener.handle(new DelegatingIssue(trackable.getIssue()) {
        @Override
        public String getSeverity() {
          return trackable.getSeverity();
        }

        @CheckForNull
        @Override
        public String getType() {
          return trackable.getType();
        }
      }));
  }

  private static Collection<Trackable> toTrackables(Collection<Issue> issues) {
    return issues.stream().map(IssueTrackable::new).collect(Collectors.toList());
  }
}
