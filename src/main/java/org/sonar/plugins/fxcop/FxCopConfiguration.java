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

import org.sonar.api.config.Settings;

public class FxCopConfiguration {

  private final String languageKey;
  private final String repositoryKey;
  private final String assemblyPropertyKey;
  private final String fxCopCmdPropertyKey;

  public FxCopConfiguration(String languageKey, String repositoryKey, String assemblyPropertyKey, String fxCopCmdPropertyKey) {
    this.languageKey = languageKey;
    this.repositoryKey = repositoryKey;
    this.assemblyPropertyKey = assemblyPropertyKey;
    this.fxCopCmdPropertyKey = fxCopCmdPropertyKey;
  }

  public String languageKey() {
    return languageKey;
  }

  public String repositoryKey() {
    return repositoryKey;
  }

  public String assemblyPropertyKey() {
    return assemblyPropertyKey;
  }

  public String fxCopCmdPropertyKey() {
    return fxCopCmdPropertyKey;
  }

  public void checkProperties(Settings settings) {
    checkProperty(settings, assemblyPropertyKey);
    checkProperty(settings, fxCopCmdPropertyKey);
  }

  private static void checkProperty(Settings settings, String property) {
    if (!settings.hasKey(property)) {
      throw new IllegalStateException("The property \"" + property + "\" must be set.");
    }
  }

}
