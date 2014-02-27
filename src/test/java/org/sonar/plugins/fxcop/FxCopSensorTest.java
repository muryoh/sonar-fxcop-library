/*
 * SonarQube FxCop Library
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.fxcop;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FxCopSensorTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void shouldExecuteOnProject() {
    Settings settings = mock(Settings.class);
    RulesProfile profile = mock(RulesProfile.class);
    ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);

    Project project = mock(Project.class);

    FxCopSensor sensor = new FxCopSensor(
      "foo", "foo-fxcop", "assemblyKey", "fxcopcmdPath",
      settings, profile, fileSystem, perspectives);

    when(settings.hasKey("assemblyKey")).thenReturn(false);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(settings.hasKey("assemblyKey")).thenReturn(true);
    when(fileSystem.files(Mockito.any(FileQuery.class))).thenReturn(ImmutableList.<File>of());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(settings.hasKey("assemblyKey")).thenReturn(true);
    when(fileSystem.files(Mockito.any(FileQuery.class))).thenReturn(ImmutableList.of(mock(File.class)));
    when(profile.getActiveRulesByRepository("foo-fxcop")).thenReturn(ImmutableList.<ActiveRule>of());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(settings.hasKey("assemblyKey")).thenReturn(true);
    when(fileSystem.files(Mockito.any(FileQuery.class))).thenReturn(ImmutableList.of(mock(File.class)));
    when(profile.getActiveRulesByRepository("foo-fxcop")).thenReturn(ImmutableList.of(mock(ActiveRule.class)));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void analyze() throws Exception {
    Settings settings = mock(Settings.class);
    RulesProfile profile = mock(RulesProfile.class);
    ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);

    FxCopSensor sensor = new FxCopSensor(
      "foo", "foo-fxcop", "assemblyKey", "fxcopcmdPath",
      settings, profile, fileSystem, perspectives);

    SensorContext context = mock(SensorContext.class);
    FileProvider fileProvider = mock(FileProvider.class);
    FxCopExecutor executor = mock(FxCopExecutor.class);

    File workingDir = tmp.newFolder(this.getClass().getSimpleName());
    when(fileSystem.workingDir()).thenReturn(workingDir);

    when(settings.getString("assemblyKey")).thenReturn("MyLibrary.dll");
    when(settings.getString("fxcopcmdPath")).thenReturn("FxCopCmd.exe");

    Files.copy(new File("src/test/resources/FxCopSensorTest/fxcop-report.xml"), new File(workingDir, "fxcop-report.xml"));

    org.sonar.api.resources.File fooSonarFileWithIssuable = mockSonarFile("foo");
    org.sonar.api.resources.File fooSonarFileWithoutIssuable = mockSonarFile("foo");
    org.sonar.api.resources.File barSonarFile = mockSonarFile("bar");

    when(fileProvider.fromIOFile(new File(new File("basePath"), "Class1.cs"))).thenReturn(null);
    when(fileProvider.fromIOFile(new File(new File("basePath"), "Class2.cs"))).thenReturn(fooSonarFileWithIssuable);
    when(fileProvider.fromIOFile(new File(new File("basePath"), "Class3.cs"))).thenReturn(fooSonarFileWithIssuable);
    when(fileProvider.fromIOFile(new File(new File("basePath"), "Class4.cs"))).thenReturn(fooSonarFileWithoutIssuable);
    when(fileProvider.fromIOFile(new File(new File("basePath"), "Class5.cs"))).thenReturn(barSonarFile);

    Issue issue1 = mock(Issue.class);
    IssueBuilder issueBuilder1 = mockIssueBuilder();
    when(issueBuilder1.build()).thenReturn(issue1);

    Issue issue2 = mock(Issue.class);
    IssueBuilder issueBuilder2 = mockIssueBuilder();
    when(issueBuilder2.build()).thenReturn(issue2);

    Issuable issuable = mock(Issuable.class);
    when(perspectives.as(Issuable.class, fooSonarFileWithIssuable)).thenReturn(issuable);
    when(issuable.newIssueBuilder()).thenReturn(issueBuilder1, issueBuilder2);

    sensor.analyse(context, fileProvider, executor);

    verify(executor).execute("FxCopCmd.exe", "MyLibrary.dll", new File(workingDir, "fxcop-sonarqube.ruleset"), new File(workingDir, "fxcop-report.xml"));

    verify(issuable).addIssue(issue1);
    verify(issuable).addIssue(issue2);

    verify(issueBuilder1).line(11);
    verify(issueBuilder1).message("First issue.");

    verify(issueBuilder2).line(12);
    verify(issueBuilder2).message("Second issue.");
  }

  private static org.sonar.api.resources.File mockSonarFile(String languageKey) {
    Language language = mock(Language.class);
    when(language.getKey()).thenReturn(languageKey);
    org.sonar.api.resources.File sonarFile = mock(org.sonar.api.resources.File.class);
    when(sonarFile.getLanguage()).thenReturn(language);
    return sonarFile;
  }

  private static IssueBuilder mockIssueBuilder() {
    IssueBuilder issueBuilder = mock(IssueBuilder.class);
    when(issueBuilder.ruleKey(Mockito.any(RuleKey.class))).thenReturn(issueBuilder);
    when(issueBuilder.line(Mockito.anyInt())).thenReturn(issueBuilder);
    when(issueBuilder.message(Mockito.anyString())).thenReturn(issueBuilder);
    return issueBuilder;
  }

}
