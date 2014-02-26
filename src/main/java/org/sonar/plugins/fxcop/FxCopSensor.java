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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FxCopSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FxCopSensor.class);

  private final Settings settings;
  private final RulesProfile profile;
  private final ModuleFileSystem fileSystem;
  private final ResourcePerspectives perspectives;

  public FxCopSensor(Settings settings, RulesProfile profile, ModuleFileSystem fileSystem, ResourcePerspectives perspectives) {
    this.settings = settings;
    this.profile = profile;
    this.fileSystem = fileSystem;
    this.perspectives = perspectives;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    boolean shouldExecute;

    if (!settings.hasKey(HardcodedCrap.ASSEMBLIES_PROPERTY_KEY)) {
      shouldExecute = false;
    } else if (profile.getActiveRulesByRepository(HardcodedCrap.REPOSITORY_KEY).isEmpty()) {
      LOG.info("All FxCop rules are disabled, skipping its execution.");
      shouldExecute = false;
    } else {
      shouldExecute = true;
    }

    return shouldExecute;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    File rulesetFile = new File(fileSystem.workingDir(), "fxcop-sonarqube.ruleset");
    new FxCopRulesetWriter().write(enabledRuleKeys(), rulesetFile);

    File reportFile = new File(fileSystem.workingDir(), "fxcop-report.xml");

    Command command = Command.create(HardcodedCrap.FXCOPCMD_PATH)
      .addArgument("/file:" + settings.getString(HardcodedCrap.ASSEMBLIES_PROPERTY_KEY))
      .addArgument("/ruleset:=" + rulesetFile.getAbsolutePath())
      .addArgument("/out:" + reportFile.getAbsolutePath())
      .addArgument("/outxsl:none")
      .addArgument("/forceoutput");
    CommandExecutor.create().execute(command, TimeUnit.MINUTES.toMillis(HardcodedCrap.FXCOPCMD_TIMEOUT_MINUTES));

    for (FxCopIssue issue : new FxCopReportParser().parse(reportFile)) {
      if (issue.path() == null || issue.file() == null || issue.line() == null) {
        LOG.info("Skipping the FxCop issue at line " + issue.reportLine() + " which has no associated file.");
        continue;
      }

      File file = new File(new File(issue.path()), issue.file());
      org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.fromIOFile(file, project);
      if (sonarFile == null) {
        LOG.info("Skipping the FxCop issue at line " + issue.reportLine() + " whose file \"" + file.getAbsolutePath() + "\" is not in SonarQube.");
        continue;
      }

      Issuable issuable = perspectives.as(Issuable.class, sonarFile);
      issuable.addIssue(
        issuable.newIssueBuilder()
          .ruleKey(RuleKey.of(HardcodedCrap.REPOSITORY_KEY, HardcodedCrap.SINGLE_RULE_KEY))
          .line(issue.line())
          .message(issue.message())
          .build());
    }
  }

  private List<String> enabledRuleKeys() {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (ActiveRule activeRule : profile.getActiveRulesByRepository(HardcodedCrap.REPOSITORY_KEY)) {
      builder.add(activeRule.getRuleKey());
    }
    return builder.build();
  }

}
