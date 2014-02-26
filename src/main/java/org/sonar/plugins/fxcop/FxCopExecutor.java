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

import org.sonar.api.BatchExtension;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class FxCopExecutor implements BatchExtension {

  private static final int FXCOPCMD_TIMEOUT_MINUTES = 30;

  public void execute(String executable, String assemblies, File rulesetFile, File reportFile) {
    CommandExecutor.create().execute(
      Command.create(executable)
        .addArgument("/file:" + assemblies)
        .addArgument("/ruleset:=" + rulesetFile.getAbsolutePath())
        .addArgument("/out:" + reportFile.getAbsolutePath())
        .addArgument("/outxsl:none")
        .addArgument("/forceoutput"),
      TimeUnit.MINUTES.toMillis(FXCOPCMD_TIMEOUT_MINUTES));
  }

}
