/**
 * Copyright 2010 Molindo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.molindo.webtools.loganalyzer.handler;

import at.molindo.webtools.loganalyzer.Request;

public abstract class AbstractLogHandler {
	private String _name;

	public AbstractLogHandler(String name) {
		setName(name);
	}

	public abstract void report();

	public abstract void handle(Request request);

	public String getName() {
		return _name;
	}

	public AbstractLogHandler setName(String name) {
		_name = name;
		return this;
	}

	public void onAfterFile() {
	}

	public void onBeforeFile() {
	}

	public void onAfterAnalyze() {
	}

	public void onBeforeAnalyze() {
	}
}