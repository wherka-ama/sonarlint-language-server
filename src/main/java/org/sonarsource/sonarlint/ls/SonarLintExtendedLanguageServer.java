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
package org.sonarsource.sonarlint.ls;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

public interface SonarLintExtendedLanguageServer extends LanguageServer {

  @JsonRequest("sonarlint/listAllRules")
  CompletableFuture<Map<String, List<Rule>>> listAllRules();

  /**
   * Undocumented VSCode message
   * https://github.com/Microsoft/vscode-languageserver-node/issues/170
   * https://github.com/eclipse/lsp4j/issues/22
   * https://github.com/microsoft/vscode-languageserver-node/blob/5c446d0620fc5fa6c57b1addcdfaff89a47624ae/jsonrpc/src/main.ts#L204
   */
  @JsonNotification("$/setTraceNotification")
  void setTraceNotification(SetTraceNotificationParams params);

  class SetTraceNotificationParams {
    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public enum TraceValues {
    @SerializedName("off")
    OFF,
    @SerializedName("messages")
    MESSAGES,
    @SerializedName("verbose")
    VERBOSE
  }

  @JsonNotification("sonarlint/didClasspathUpdate")
  void didClasspathUpdate(String projectUri);

  /**
   * Possible server modes for the <code>redhat.vscode-java</code> Language Server
   * https://github.com/redhat-developer/vscode-java/blob/5642bf24b89202acf3911fe7a162b6dbcbeea405/src/settings.ts#L198
   */
  enum ServerMode {
    LIGHTWEIGHT("LightWeight"),
    HYBRID("Hybrid"),
    STANDARD("Standard");

    private final String serializedForm;

    ServerMode(String serializedForm) {
      this.serializedForm = serializedForm;
    }

    static ServerMode of(String serializedForm) {
      return Stream.of(values())
        .filter(m -> m.serializedForm.equals(serializedForm))
        .findFirst()
        .orElse(ServerMode.LIGHTWEIGHT);
    }
  }

  @JsonNotification("sonarlint/didJavaServerModeChange")
  void didJavaServerModeChange(String serverMode);

  class LocalBranchNameChangeEvent {
    private String folderUri;
    @Nullable
    private String branchName;

    public LocalBranchNameChangeEvent(String folderUri, @Nullable String branchName) {
      setFolderUri(folderUri);
      setBranchName(branchName);
    }

    public String getFolderUri() {
      return folderUri;
    }

    @CheckForNull
    public String getBranchName() {
      return branchName;
    }

    public void setBranchName(@Nullable String branchName) {
      this.branchName = branchName;
    }

    public void setFolderUri(String folderUri) {
      this.folderUri = folderUri;
    }
  }

  @JsonNotification("sonarlint/didLocalBranchNameChange")
  void didLocalBranchNameChange(LocalBranchNameChangeEvent event);
}
