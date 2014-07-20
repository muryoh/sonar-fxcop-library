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

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class FxCopExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(FxCopExecutor.class);

  private static final int EXIT_CODE_SUCCESS = 0;
  private static final int EXIT_CODE_SUCCESS_SHOULD_BREAK_BUILD = 1024;
  
  public void execute(String executable, String assemblies, File rulesetFile,
      File reportFile, int timeout, String assemblyDependencyDirectories,
      boolean aspnet) {
    int exitCode = CommandExecutor.create().execute(
        createCommand(executable, assemblies, rulesetFile, reportFile, assemblyDependencyDirectories, aspnet),
        TimeUnit.MINUTES.toMillis(timeout));
    Preconditions.checkState(exitCode == EXIT_CODE_SUCCESS || exitCode == EXIT_CODE_SUCCESS_SHOULD_BREAK_BUILD,
        "The execution of \"" + executable + "\" failed and returned "
            + exitCode + " as exit code.");
    LOG.info("FxCopCmd.exe ended with the exit code: " + exitCode);

  }
  
  private Command createCommand(String executable, String assemblies, File rulesetFile, File reportFile, String assemblyDependencyDirectories, boolean aspnet) {
	  Command command = Command.create(getExecutable(executable))

      .addArgument("/ruleset:=" + rulesetFile.getAbsolutePath())
      .addArgument("/out:" + reportFile.getAbsolutePath())
      .addArgument("/outxsl:none")
      .addArgument("/forceoutput")
      .addArgument("/searchgac");

    if (aspnet) {
      command.addArgument("/aspnet");
    }

    for (String assembly : assemblies.split(",")) {
      command.addArgument("/file:" + assembly);
    }

    if (assemblyDependencyDirectories != null
        && assemblyDependencyDirectories.length() > 0) {
      String[] directories = assemblyDependencyDirectories.split(",");
      for (String directory : directories) {
        command.addArgument("/directory:" + directory);
      }
    }
    return command;

  }

  /**
   * Handles deprecated property: "installDirectory", which gives the path to the directory only.
   */
  private static String getExecutable(String propertyValue) {
    String execName = "FxCopCmd.exe";


    if (!propertyValue.endsWith(execName)) {
      return (new File(propertyValue, execName)).getAbsolutePath();
    } else {
      return propertyValue;
    }
  }
  
}
