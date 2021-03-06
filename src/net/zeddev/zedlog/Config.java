package net.zeddev.zedlog;
/* Copyright (C) 2013  Zachary Scott <zscott.dev@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// NOTE Must NOT depend on any external packages as is used within the installer

/**
 * Configuration for ZedLog.
 *
 * @author Zachary Scott <zscott.dev@gmail.com>
 */
public enum Config {

	/** Singleton instance. */
	INSTANCE;

	/** Major version number. */
	public final int VERSION_MAJOR = 0;

	/** Minor version number.  */
	public final int VERSION_MINOR = 2;

	/** Version build (minor-minor version).  */
	public final int VERSION_BUILD = 4;
	
	/** The phase of version development (alpha, beta, rc etc.). */
	public final String VERSION_PHASE = "beta";

	/** The version as a string. */
	public final String VERSION = 
			"v" + VERSION_MAJOR + "." + VERSION_MINOR + VERSION_PHASE + VERSION_BUILD;

	/** Official program name. */
	public final String NAME = "ZedLog";

	/** The full program name including version. */
	public final String FULL_NAME = NAME + " " + VERSION;

	/** A short description about the program. */
	public final String DESCRIPTION =
		"A robust cross-platform input logging tool (AKA a key logger).";

	/** The GUI icon directory. */
	public static final String ICON_DIR = "/net/zeddev/zedlog/gui/icons";

	/** The documentation resource directory. */
	public static final String DOCDIR = "/net/zeddev/zedlog/doc";

	/** The help html file name (relative to {@code Config.HELPDIR}).
	 * i.e. Needs to be loaded using {@code Class.getResource()}.
	 */
	public static final String HELPDOC = "helpdoc.html";

	/** The OS in which the program is running. */
	public final String OS = System.getProperty("os.name").trim().toLowerCase();

	private Config() {
	}
	
	/** The current users name. */
	public String userName() {
		return System.getProperty("user.name").toLowerCase();
	}
	
	/** Whether running under *nix (i.e. Unix, Linux, etc.). */
	public boolean isUnix() {
		return OS.matches(".*(nix|nux|aix)");
	}
	
	/** Whether running under Windows. */
	public boolean isWindows() {
		return OS.matches(".*win.*");
	}
	
	/** Whether running under Mac OSX. */
	public boolean isOSX() {
		return OS.matches("os x");
	}
	
	/** Whether running under Mac. */
	public boolean isMac() {
		// NOTE Not supported
		return OS.matches("mac");
	}
	
	/** Whether running under Solaris. */
	public boolean isSolaris() {
		// NOTE Not supported!!
		return OS.matches("solaris|sunos");
	}

}
