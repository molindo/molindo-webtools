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

/**
 * 
 */
package at.molindo.webtools.loganalyzer.filter;

import at.molindo.webtools.loganalyzer.Request;


public class AgentFilter extends AbstractFilter {
	private String _criteria;
	private boolean _ignoreCase;

	public AgentFilter(String criteria, boolean ignoreCase) {
		super("Agents containing '" + criteria + "'"
				+ (ignoreCase ? " (case ignored)" : ""));

		if (criteria == null || "".equals(criteria)) {
			throw new IllegalArgumentException("criteria must not be empty");
		}
		_ignoreCase = ignoreCase;
		_criteria = (_ignoreCase) ? criteria.toLowerCase() : criteria;
	}

	@Override
	public boolean filter(Request request) {
		String agent = request.getAgent();
		if (_ignoreCase)
			agent = agent.toLowerCase();
		return !agent.contains(_criteria);
	}
}