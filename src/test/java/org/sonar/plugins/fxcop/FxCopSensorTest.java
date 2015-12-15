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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FxCopSensorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldExecuteOnProject() {
    Settings settings = mock(Settings.class);
    RulesProfile profile = mock(RulesProfile.class);
    DefaultFileSystem fs = new DefaultFileSystem();
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);

    Project project = mock(Project.class);

    FxCopSensor sensor = new FxCopSensor(
      new FxCopConfiguration("foo", "foo-fxcop", "", "", "", "", "", "", "", ""),
      settings, profile, fs, perspectives);

    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    fs.add(new DefaultInputFile("bar").setAbsolutePath("bar").setLanguage("bar"));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    fs.add(new DefaultInputFile("foo").setAbsolutePath("foo").setLanguage("foo"));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(profile.getActiveRulesByRepository("foo-fxcop")).thenReturn(ImmutableList.<ActiveRule>of());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(profile.getActiveRulesByRepository("foo-fxcop")).thenReturn(ImmutableList.of(mock(ActiveRule.class)));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void analyze() throws Exception {
    Settings settings = mock(Settings.class);
    RulesProfile profile = mock(RulesProfile.class);
    DefaultFileSystem fs = new DefaultFileSystem();
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);

    FxCopConfiguration fxCopConf = mock(FxCopConfiguration.class);
    when(fxCopConf.languageKey()).thenReturn("foo");
    when(fxCopConf.repositoryKey()).thenReturn("foo-fxcop");
    when(fxCopConf.assemblyPropertyKey()).thenReturn("assemblyKey");
    when(fxCopConf.fxCopCmdPropertyKey()).thenReturn("fxcopcmdPath");
    when(fxCopConf.timeoutPropertyKey()).thenReturn("timeout");
    when(fxCopConf.aspnetPropertyKey()).thenReturn("aspnet");
    when(fxCopConf.directoriesPropertyKey()).thenReturn("directories");
    when(fxCopConf.referencesPropertyKey()).thenReturn("references");
    when(fxCopConf.assemblyCompareModePropertyKey()).thenReturn("assembly-compare-mode");

    FxCopSensor sensor = new FxCopSensor(
      fxCopConf,
      settings, profile, fs, perspectives);
    when(settings.hasKey("assemblyKey")).thenReturn(true);
    when(settings.hasKey("fxcopcmdPath")).thenReturn(true);

    List<ActiveRule> activeRules = mockActiveRules("CA0000", "CA1000", "CustomRuleTemplate", "CR1000");
    when(profile.getActiveRulesByRepository("foo-fxcop")).thenReturn(activeRules);

    SensorContext context = mock(SensorContext.class);
    FxCopExecutor executor = mock(FxCopExecutor.class);

    File workingDir = new File(new File("target/FxCopSensorTest/working-dir").getAbsolutePath());
    fs.setWorkDir(workingDir);

    when(settings.getString("assemblyKey")).thenReturn("MyLibrary.dll");
    when(settings.getString("fxcopcmdPath")).thenReturn("FxCopCmd.exe");
    when(settings.getInt("timeout")).thenReturn(42);
    when(settings.getBoolean("aspnet")).thenReturn(true);
    when(settings.getString("directories")).thenReturn(" c:/,,  d:/ ");
    when(settings.getString("references")).thenReturn(null);
    when(settings.getString("assembly-compare-mode")).thenReturn("compareUsingMyMode");

    InputFile class5InputFile = new DefaultInputFile("Class5.cs").setAbsolutePath(new File(new File("basePath"), "Class5.cs").getAbsolutePath()).setLanguage("foo");
    InputFile class6InputFile = new DefaultInputFile("Class6.cs").setAbsolutePath(new File(new File("basePath"), "Class6.cs").getAbsolutePath()).setLanguage("foo");
    InputFile class7InputFile = new DefaultInputFile("Class7.cs").setAbsolutePath(new File(new File("basePath"), "Class7.cs").getAbsolutePath()).setLanguage("foo");
    InputFile class8InputFile = new DefaultInputFile("Class8.cs").setAbsolutePath(new File(new File("basePath"), "Class8.cs").getAbsolutePath()).setLanguage("bar");
    InputFile class9InputFile = new DefaultInputFile("Class9.cs").setAbsolutePath(new File(new File("basePath"), "Class9.cs").getAbsolutePath()).setLanguage("foo");

    fs.add(class5InputFile);
    fs.add(class6InputFile);
    fs.add(class7InputFile);
    fs.add(class8InputFile);
    fs.add(class9InputFile);

    Issue issue1 = mock(Issue.class);
    IssueBuilder issueBuilder1 = mockIssueBuilder();
    when(issueBuilder1.build()).thenReturn(issue1);

    Issue issue2 = mock(Issue.class);
    IssueBuilder issueBuilder2 = mockIssueBuilder();
    when(issueBuilder2.build()).thenReturn(issue2);

    Issue issue3 = mock(Issue.class);
    IssueBuilder issueBuilder3 = mockIssueBuilder();
    when(issueBuilder3.build()).thenReturn(issue3);

    Issuable issuable = mock(Issuable.class);
    when(perspectives.as(Issuable.class, class5InputFile)).thenReturn(issuable);
    when(perspectives.as(Issuable.class, class6InputFile)).thenReturn(issuable);
    when(perspectives.as(Issuable.class, class9InputFile)).thenReturn(issuable);
    when(issuable.newIssueBuilder()).thenReturn(issueBuilder1, issueBuilder2, issueBuilder3);

    FxCopRulesetWriter writer = mock(FxCopRulesetWriter.class);

    FxCopReportParser parser = mock(FxCopReportParser.class);
    when(parser.parse(new File(workingDir, "fxcop-report.xml"))).thenReturn(
      ImmutableList.of(
        new FxCopIssue(100, "CA0000", null, "Class1.cs", 1, "Dummy message"),
        new FxCopIssue(200, "CA0000", "basePath", null, 2, "Dummy message"),
        new FxCopIssue(300, "CA0000", "basePath", "Class3.cs", null, "Dummy message"),
        new FxCopIssue(400, "CA0000", "basePath", "Class4.cs", 4, "First message"),
        new FxCopIssue(500, "CA0000", "basePath", "Class5.cs", 5, "Second message"),
        new FxCopIssue(600, "CA1000", "basePath", "Class6.cs", 6, "Third message"),
        new FxCopIssue(700, "CA0000", "basePath", "Class7.cs", 7, "Fourth message"),
        new FxCopIssue(800, "CA0000", "basePath", "Class8.cs", 8, "Fifth message"),
        new FxCopIssue(800, "CR1000", "basePath", "Class9.cs", 9, "Sixth message")));

    sensor.analyse(context, writer, parser, executor);

    verify(writer).write(ImmutableList.of("CA0000", "CA1000", "CR1000"), new File(workingDir, "fxcop-sonarqube.ruleset"));
    verify(executor).execute("FxCopCmd.exe", "MyLibrary.dll", new File(workingDir, "fxcop-sonarqube.ruleset"), new File(workingDir, "fxcop-report.xml"), 42, true,
      ImmutableList.of("c:/", "d:/"), ImmutableList.<String>of(), "compareUsingMyMode");

    verify(issuable).addIssue(issue1);
    verify(issuable).addIssue(issue2);
    verify(issuable).addIssue(issue3);

    verify(issueBuilder1).ruleKey(RuleKey.of("foo-fxcop", "_CA0000"));
    verify(issueBuilder1).line(5);
    verify(issueBuilder1).message("Second message");

    verify(issueBuilder2).ruleKey(RuleKey.of("foo-fxcop", "_CA1000"));
    verify(issueBuilder2).line(6);
    verify(issueBuilder2).message("Third message");

    verify(issueBuilder3).ruleKey(RuleKey.of("foo-fxcop", "CustomRuleTemplate_42"));
    verify(issueBuilder3).line(9);
    verify(issueBuilder3).message("Sixth message");
  }

  @Test
  public void analyze_with_report() {
    Settings settings = new Settings();
    RulesProfile profile = mock(RulesProfile.class);
    FileSystem fs = mock(FileSystem.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);

    FxCopConfiguration fxCopConf = mock(FxCopConfiguration.class);
    when(fxCopConf.repositoryKey()).thenReturn("foo-fxcop");
    when(fxCopConf.reportPathPropertyKey()).thenReturn("reportPath");

    FxCopSensor sensor = new FxCopSensor(
      fxCopConf,
      settings, profile, fs, perspectives);

    File reportFile = new File("src/test/resources/FxCopSensorTest/fxcop-report.xml");
    settings.setProperty("reportPath", reportFile.getAbsolutePath());

    SensorContext context = mock(SensorContext.class);
    FxCopRulesetWriter writer = mock(FxCopRulesetWriter.class);
    FxCopReportParser parser = mock(FxCopReportParser.class);
    FxCopExecutor executor = mock(FxCopExecutor.class);

    sensor.analyse(context, writer, parser, executor);

    verify(writer, Mockito.never()).write(Mockito.anyList(), Mockito.any(File.class));
    verify(executor, Mockito.never()).execute(
      Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class), Mockito.any(File.class), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyList(), Mockito.anyList(), Mockito.anyString());

    verify(parser).parse(new File(reportFile.getAbsolutePath()));
  }

  @Test
  public void check_properties() {
    thrown.expectMessage("fooAssemblyKey");

    FxCopConfiguration fxCopConf = new FxCopConfiguration("", "", "fooAssemblyKey", "", "", "", "", "", "", "");
    new FxCopSensor(fxCopConf, mock(Settings.class), mock(RulesProfile.class), mock(FileSystem.class), mock(ResourcePerspectives.class))
      .analyse(mock(Project.class), mock(SensorContext.class));
  }

  private static FilePredicate mainWithAbsolutePath(FileSystem fs, File file) {
    return fs.predicates().and(fs.predicates().hasType(Type.MAIN), fs.predicates().hasAbsolutePath(file.getAbsolutePath()));
  }

  private static IssueBuilder mockIssueBuilder() {
    IssueBuilder issueBuilder = mock(IssueBuilder.class);
    when(issueBuilder.ruleKey(Mockito.any(RuleKey.class))).thenReturn(issueBuilder);
    when(issueBuilder.line(Mockito.anyInt())).thenReturn(issueBuilder);
    when(issueBuilder.message(Mockito.anyString())).thenReturn(issueBuilder);
    return issueBuilder;
  }

  private static List<ActiveRule> mockActiveRules(String... activeConfigRuleKeys) {
    ImmutableList.Builder<ActiveRule> builder = ImmutableList.builder();
    for (String activeConfigRuleKey : activeConfigRuleKeys) {
      ActiveRule activeRule = mock(ActiveRule.class);
      if ("CustomRuleTemplate".equals(activeConfigRuleKey)) {
        when(activeRule.getRuleKey()).thenReturn(activeConfigRuleKey);
      } else if (activeConfigRuleKey.startsWith("CR")) {
        when(activeRule.getRuleKey()).thenReturn("CustomRuleTemplate_42");
        when(activeRule.getParameter("CheckId")).thenReturn(activeConfigRuleKey);
      } else if (activeConfigRuleKey.startsWith("CA")) {
        when(activeRule.getConfigKey()).thenReturn(activeConfigRuleKey);
        when(activeRule.getRuleKey()).thenReturn("_" + activeConfigRuleKey);
      } else {
        throw new IllegalArgumentException("Unsupported active rule config key: " + activeConfigRuleKey);
      }
      builder.add(activeRule);
    }
    return builder.build();
  }

}
