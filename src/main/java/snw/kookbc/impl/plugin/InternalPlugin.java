/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.plugin;

import java.io.File;
import java.io.InputStream;

import org.slf4j.Logger;

import snw.jkook.Core;
import snw.jkook.config.file.FileConfiguration;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginDescription;

// A plugin implementation as a placeholder to call methods that require plugin instance.
// DO NOT USE THIS OUTSIDE snw.kookbc PACKAGE.
public final class InternalPlugin implements Plugin {
    public static final InternalPlugin INSTANCE = new InternalPlugin();

    private InternalPlugin() {
    }

    @Override
    public FileConfiguration getConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public File getDataFolder() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PluginDescription getDescription() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public File getFile() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Logger getLogger() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public InputStream getResource(String arg0) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isEnabled() {
        return true; // always true
    }

    @Override
    public void onDisable() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void onEnable() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void onLoad() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void reloadConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void saveConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void saveDefaultConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void saveResource(String arg0, boolean arg1, boolean arg2) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setEnabled(boolean arg0) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
