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

import java.util.Arrays;
import java.util.HashSet;

import at.molindo.webtools.loganalyzer.Request;

public class StatusFilter extends AbstractFilter {

	private HashSet<Integer> _statuses = new HashSet<Integer>();

	public StatusFilter(int... statuses) {
		super("Requests with statuses: " + Arrays.asList(statuses));

		if (statuses.length == 0) {
			throw new IllegalArgumentException(
					"at least one status is required");
		}
		for (int status : statuses) {
			_statuses.add(status);
		}
	}

	@Override
	public boolean filter(Request request) {
		return !_statuses.contains(request.getStatus());
	}
}