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

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FxCopConfigurationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test() {
    FxCopConfiguration fxCopConf = new FxCopConfiguration("cs", "cs-fxcop", "fooAssemblyKey", "fooFxCopCmdPathKey", "fooTimeoutKey", "fooAspnetKey", "fooDirectoriesKey",
      "fooReferencesKey", "fooAssemblyCompareMode");
    assertThat(fxCopConf.languageKey()).isEqualTo("cs");
    assertThat(fxCopConf.repositoryKey()).isEqualTo("cs-fxcop");
    assertThat(fxCopConf.assemblyPropertyKey()).isEqualTo("fooAssemblyKey");
    assertThat(fxCopConf.fxCopCmdPropertyKey()).isEqualTo("fooFxCopCmdPathKey");
    assertThat(fxCopConf.timeoutPropertyKey()).isEqualTo("fooTimeoutKey");
    assertThat(fxCopConf.aspnetPropertyKey()).isEqualTo("fooAspnetKey");
    assertThat(fxCopConf.directoriesPropertyKey()).isEqualTo("fooDirectoriesKey");
    assertThat(fxCopConf.referencesPropertyKey()).isEqualTo("fooReferencesKey");
    assertThat(fxCopConf.assemblyCompareModePropertyKey()).isEqualTo("fooAssemblyCompareMode");

    fxCopConf = new FxCopConfiguration("vbnet", "vbnet-fxcop", "barAssemblyKey", "barFxCopCmdPathKey", "barTimeoutKey", "barAspnetKey", "barDirectoriesKey", "barReferencesKey", "barAssemblyCompareMode");
    assertThat(fxCopConf.languageKey()).isEqualTo("vbnet");
    assertThat(fxCopConf.repositoryKey()).isEqualTo("vbnet-fxcop");
    assertThat(fxCopConf.assemblyPropertyKey()).isEqualTo("barAssemblyKey");
    assertThat(fxCopConf.fxCopCmdPropertyKey()).isEqualTo("barFxCopCmdPathKey");
    assertThat(fxCopConf.timeoutPropertyKey()).isEqualTo("barTimeoutKey");
    assertThat(fxCopConf.aspnetPropertyKey()).isEqualTo("barAspnetKey");
    assertThat(fxCopConf.directoriesPropertyKey()).isEqualTo("barDirectoriesKey");
    assertThat(fxCopConf.referencesPropertyKey()).isEqualTo("barReferencesKey");
    assertThat(fxCopConf.assemblyCompareModePropertyKey()).isEqualTo("barAssemblyCompareMode");
  }

  @Test
  public void check_properties() {
    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(true);
    when(settings.getString("fooAssemblyKey")).thenReturn(new File("src/test/resources/FxCopConfigurationTest/MyLibrary.dll").getAbsolutePath());
    when(settings.hasKey("fooFxCopCmdPathKey")).thenReturn(true);
    when(settings.getString("fooFxCopCmdPathKey")).thenReturn(new File("src/test/resources/FxCopConfigurationTest/FxCopCmd.exe").getAbsolutePath());

    new FxCopConfiguration("", "", "fooAssemblyKey", "fooFxCopCmdPathKey", "", "", "", "", "").checkProperties(settings);
  }

  @Test
  public void check_properties_without_assembly_extension() {
    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(true);
    when(settings.getString("fooAssemblyKey")).thenReturn(new File("src/test/resources/FxCopConfigurationTest/MyLibrary").getAbsolutePath());
    when(settings.hasKey("fooFxCopCmdPathKey")).thenReturn(true);
    when(settings.getString("fooFxCopCmdPathKey")).thenReturn(new File("src/test/resources/FxCopConfigurationTest/FxCopCmd.exe").getAbsolutePath());

    new FxCopConfiguration("", "", "fooAssemblyKey", "fooFxCopCmdPathKey", "", "", "", "", "").checkProperties(settings);
  }

  @Test
  public void check_properties_assembly_property_not_set() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("The property \"fooAssemblyKey\" must be set and the project must have been built to execute FxCop rules.");
    thrown.expectMessage("http://docs.codehaus.org/x/TAA1Dg");
    thrown.expectMessage("sonar.visualstudio.skipIfNotBuilt");

    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(false);

    new FxCopConfiguration("", "", "fooAssemblyKey", "", "", "", "", "", "").checkProperties(settings);
  }

  @Test
  public void check_properties_assembly_property_not_not_found() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot find the assembly");
    thrown.expectMessage(new File("src/test/resources/FxCopConfigurationTest/MyLibraryNotFound.dll").getAbsolutePath());
    thrown.expectMessage("\"fooAssemblyKey\"");

    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(true);
    when(settings.getString("fooAssemblyKey")).thenReturn(new File("src/test/resources/FxCopConfigurationTest/MyLibraryNotFound.dll").getAbsolutePath());

    new FxCopConfiguration("", "", "fooAssemblyKey", "", "", "", "", "", "").checkProperties(settings);
  }

  @Test
  public void check_properties_assembly_property_pdb_not_found() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot find the .pdb file");
    thrown.expectMessage(new File("src/test/resources/FxCopConfigurationTest/MyLibraryWithoutPdb.pdb").getAbsolutePath());
    thrown.expectMessage("\"fooAssemblyKey\"");

    Settings settings = mock(Settings.class);
    when(settings.hasKey("fooAssemblyKey")).thenReturn(true);
    when(settings.getString("fooAssemblyKey")).thenReturn(new File("src/test/resources/FxCopConfigurationTest/MyLibraryWithoutPdb.dll").getAbsolutePath());

    new FxCopConfiguration("", "", "fooAssemblyKey", "", "", "", "", "", "").checkProperties(settings);
  }

  @Test
  public void check_properties_fxcopcmd_property_deprecated() {
    Settings settings = new Settings();
    settings.setProperty("fooAssemblyKey", "src/test/resources/FxCopConfigurationTest/MyLibrary.dll");
    settings.setProperty("sonar.fxcop.installDirectory", new File("src/test/resources/FxCopConfigurationTest/FxCopCmd.exe").getAbsolutePath());

    FxCopConfiguration fxCopConf = new FxCopConfiguration("", "", "fooAssemblyKey", "fooFxCopCmdPathKey", "", "", "", "", "");
    fxCopConf.checkProperties(settings);

    assertThat(settings.getString(fxCopConf.fxCopCmdPropertyKey())).isEqualTo(new File("src/test/resources/FxCopConfigurationTest/FxCopCmd.exe").getAbsolutePath());
  }

  @Test
  public void check_properties_fxcopcmd_property_not_found() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot find the FxCopCmd executable");
    thrown.expectMessage(new File("src/test/resources/FxCopConfigurationTest/FxCopCmdNotFound.exe").getAbsolutePath());
    thrown.expectMessage("\"fooFxCopCmdPathKey\"");

    Settings settings = new Settings();
    settings.setProperty("fooAssemblyKey", "src/test/resources/FxCopConfigurationTest/MyLibrary.dll");
    settings.setProperty("fooFxCopCmdPathKey", new File("src/test/resources/FxCopConfigurationTest/FxCopCmdNotFound.exe").getAbsolutePath());

    new FxCopConfiguration("", "", "fooAssemblyKey", "fooFxCopCmdPathKey", "", "", "", "", "").checkProperties(settings);
  }

  @Test
  public void check_properties_timeout_property_deprecated() {
    Settings settings = new Settings();
    settings.setProperty("fooAssemblyKey", "src/test/resources/FxCopConfigurationTest/MyLibrary.dll");
    settings.setProperty("fooFxCopCmdPathKey", new File("src/test/resources/FxCopConfigurationTest/FxCopCmd.exe").getAbsolutePath());
    settings.setProperty("sonar.fxcop.timeoutMinutes", "42");

    FxCopConfiguration fxCopConf = new FxCopConfiguration("", "", "fooAssemblyKey", "fooFxCopCmdPathKey", "fooTimeoutKey", "", "", "", "");
    fxCopConf.checkProperties(settings);

    assertThat(settings.getString(fxCopConf.timeoutPropertyKey())).isEqualTo("42");
  }

}
