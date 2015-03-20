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
package at.molindo.webtools.loganalyzer.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import at.molindo.webtools.loganalyzer.Request;

public class AgentCollector extends AbstractCollector {
	private static final int MAX_REPORTED = 64;

	public AgentCollector() {
		super("Agents");
	}

	private final HashSet<String> _agents = new HashSet<String>();

	@Override
	public void report() {
		Collection<String> agents;
		if (_agents.size() > MAX_REPORTED) {
			agents = new ArrayList<String>(_agents).subList(0, MAX_REPORTED);
		} else {
			agents = _agents;
		}

		System.out.println("agents count:    " + _agents.size());
		if (agents.size() > 0) {
			System.out.println("agents:          " + agents + (_agents.size() > MAX_REPORTED ? " ..." : ""));
		}
	}

	@Override
	public void collect(Request request) {
		_agents.add(request.getAgent());
	}
}