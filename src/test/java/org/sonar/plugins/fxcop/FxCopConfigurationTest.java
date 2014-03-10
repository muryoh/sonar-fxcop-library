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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FxCopConfigurationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test() {
    FxCopConfiguration fxCopConf = new FxCopConfiguration("cs", "cs-fxcop", "fooAssemblyKey", "fooFxCopCmdPathKey");
    assertThat(fxCopConf.languageKey()).isEqualTo("cs");
    assertThat(fxCopConf.repositoryKey()).isEqualTo("cs-fxcop");
    assertThat(fxCopConf.assemblyPropertyKey()).isEqualTo("fooAssemblyKey");
    assertThat(fxCopConf.fxCopCmdPropertyKey()).isEqualTo("fooFxCopCmdPathKey");

    fxCopConf = new FxCopConfiguration("vbnet", "vbnet-fxcop", "barAssemblyKey", "barFxCopCmdPathKey");
    assertThat(fxCopConf.languageKey()).isEqualTo("vbnet");
    assertThat(fxCopConf.repositoryKey()).isEqualTo("vbnet-fxcop");
    assertThat(fxCopConf.assemblyPropertyKey()).isEqualTo("barAssemblyKey");
    assertThat(fxCopConf.fxCopCmdPropertyKey()).isEqualTo("barFxCopCmdPathKey");
  }

  @Test
  public void check_properties() {
    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(true);
    when(settings.hasKey("fooFxCopCmdPathKey")).thenReturn(true);

    new FxCopConfiguration("", "", "fooAssemblyKey", "fooFxCopCmdPathKey").checkProperties(settings);
  }

  @Test
  public void check_properties_assembly_property() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The property \"fooAssemblyKey\" must be set.");

    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(false);

    new FxCopConfiguration("", "", "fooAssemblyKey", "").checkProperties(settings);
  }

  @Test
  public void check_properties_fxcopcmd_property() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The property \"fooFxCopCmdPathKey\" must be set.");

    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(true);
    when(settings.hasKey("fooFxCopCmdPathKey")).thenReturn(false);

    new FxCopConfiguration("", "", "fooAssemblyKey", "fooFxCopCmdPathKey").checkProperties(settings);
  }

}
