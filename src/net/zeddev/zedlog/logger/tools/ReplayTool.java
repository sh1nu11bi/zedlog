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

package net.zeddev.zedlog.logger.tools;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import net.zeddev.litelogger.Logger;
import net.zeddev.zedlog.logger.LogEntry;
import net.zeddev.zedlog.logger.LogEvent;
import net.zeddev.zedlog.logger.impl.CompositeDataLogger;
import net.zeddev.zedlog.logger.impl.KeyEvent;
import net.zeddev.zedlog.logger.impl.MouseClickLogger;
import net.zeddev.zedlog.logger.impl.MouseDraggedLogger;
import net.zeddev.zedlog.logger.impl.MouseMovementLogger;
import net.zeddev.zedlog.logger.impl.MouseWheelLogger;

/**
 * Replays/simulates logged <code>LogEvent</code>s.
 * Uses <code>java.awt.Robot</code> to simulate mouse moves, clicks and key
 * presses.
 *
 * @author Zachary Scott <zscott.dev@gmail.com>
 */
public final class ReplayTool implements Runnable {

	private final Logger logger = Logger.getLogger(this);

	// the log entries to be replayed
	private final List<LogEntry> logEntries;

	// the tools observers
	private final List<ReplayToolObserver> observers = new ArrayList<>();

	private boolean running = false;

	/**
	 * Creates a new <code>ReplayTool</code> for the given <code>LogEntry</code> set.
	 *
	 * @param logEntries The <code>LogEntry</code> list in which to replay.
	 */
	public ReplayTool(final List<LogEntry> logEntries) {
		this.logEntries = logEntries;
			// TODO sort chronologically
	}

	/**
	 * Creates a new <code>ReplayTool</code> for the given <code>CompositeDataLogger</code>.
	 *
	 * @param loggers The <code>CompositeDataLogger</code> in which to replay.
	 */
	public ReplayTool(final CompositeDataLogger loggers) {
		this(loggers.logEntries());
	}

	/**
	 * Adds a <code>ReplayToolObserver</code>.
	 *
	 * @param observer The observer to be added to the list.
	 */
	public void addObserver(final ReplayToolObserver observer) {
		assert(observer != null);
		observers.add(observer);
	}

	/**
	 * Removes the given <code>ReplayToolObserver</code>.
	 *
	 * @param observer The observer to be removed
	 */
	public void removeObserver(final ReplayToolObserver observer) {
		assert(observer != null);
		observers.remove(observer);
	}

	// simulates a KeyEvent
	private void simKeyEvent(Robot robot, KeyEvent event) {

		int keyCode = event.getKeyCode();

		if (keyCode > 0) {
			if (event.getEventType() == KeyEvent.Type.PRESSED) {
				robot.keyPress(keyCode);
			} else if (event.getEventType() == KeyEvent.Type.RELEASED) {
				robot.keyRelease(keyCode);
			} else if (event.getEventType() == KeyEvent.Type.TYPED) {
				robot.keyPress(keyCode);
				robot.keyRelease(keyCode);
			}
		}

	}

	// simulates a mouse movement
	private void simMouseMoveEvent(Robot robot, MouseMovementLogger.MouseMovedEvent event) {

		robot.mouseMove(event.getX(), event.getY());

	}

	// converts between JNativeHook mouse button codes and AWT's
	private int convertMouseButtonCode(int orig) {

		if (orig == 1) {
			return InputEvent.BUTTON1_MASK;
		} else if (orig == 2) {
			return InputEvent.BUTTON2_MASK;
		} else if (orig == 3) {
			return InputEvent.BUTTON3_MASK;
		} else {
			// XXX default to button 1 as to avoid causing exceptions
			return InputEvent.BUTTON1_MASK;
		}

	}

	// simulates a mouse drag
	private void simMouseDragEvent(Robot robot, MouseDraggedLogger.MouseDraggedEvent event) {

		int button = convertMouseButtonCode(event.getButtonCode());

		robot.mousePress(button);
		robot.mouseMove(event.getX(), event.getY());
		robot.mouseRelease(button);

	}

	// simulate a mouse click
	private void simMouseClickEvent(Robot robot, MouseClickLogger.MouseClickedEvent event) {

		int button = convertMouseButtonCode(event.getButtonCode());

		// simulate a click
		robot.mousePress(button);
		robot.delay(10);
		robot.mouseRelease(button);

	}

	// simulates a mouse wheel movement
	private void simMouseWheelEvent(Robot robot, MouseWheelLogger.MouseWheelMovedEvent event) {

		robot.mouseWheel(event.getRotation());

	}

	// simulates a LogEvent
	private void simEvent(Robot robot, LogEvent event) {

		// simulate the event
		if (event != null) {
			if (event instanceof KeyEvent) {
				simKeyEvent(robot, (KeyEvent) event);
			} else if (event instanceof MouseMovementLogger.MouseMovedEvent) {
				simMouseMoveEvent(robot, (MouseMovementLogger.MouseMovedEvent) event);
			} else if (event instanceof MouseDraggedLogger.MouseDraggedEvent) {
				simMouseDragEvent(robot, (MouseDraggedLogger.MouseDraggedEvent) event);
			} else if (event instanceof MouseClickLogger.MouseClickedEvent) {
				simMouseClickEvent(robot, (MouseClickLogger.MouseClickedEvent) event);
			} else if (event instanceof MouseWheelLogger.MouseWheelMovedEvent) {
				simMouseWheelEvent(robot, (MouseWheelLogger.MouseWheelMovedEvent) event);
			} else {
				// IGNORE - unknown event type
			}
		}

		// notify observers of simulated event
		for (ReplayToolObserver observer : observers)
			observer.replayedEvent(event);

	}

	/**
	 * Replays the events, timed the same as the original events.
	 *
	 */
	@Override
	public void run() {

		logger.info("Replay tool simulation started.");

		// the robot used to simulate keyboard and mouse events
		Robot robot;
		try {
			robot = new Robot();
		} catch (AWTException ex) {
			logger.error("Failed to initialise input control!", ex);
			return;
		}

		running = true;

		// simulate each logged event
		for (int i = 0; running && i < logEntries.size(); i++) {
			// NOTE it is assumed that the log entries are in chronological order

			long startTime = System.currentTimeMillis();

			simEvent(robot, logEntries.get(i).getEvent());

			long timeTaken = System.currentTimeMillis() - startTime;

			// time the next event correctly
			if ((i + 1) < logEntries.size()) {

				long delayTime = logEntries.get(i + 1).getTimestamp() - logEntries.get(i).getTimestamp();
				delayTime -= timeTaken; // compensate for time taken simulating event

				if (delayTime > 0)
					robot.delay((int) delayTime);

			}

		}

		// notify of finish
		for (ReplayToolObserver observer : observers)
			observer.replayFinished();

		logger.info("Replay tool simulation finished.");

	}

	/**
	 * Stops replaying.
	 * Thread safe.
	 *
	 */
	public void stop() {
		running = false;
		logger.info("Replay tool simulation stopped.");
	}

}
