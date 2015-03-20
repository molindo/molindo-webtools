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
package at.molindo.webtools.loganalyzer.filter;

import java.util.Arrays;

import at.molindo.webtools.loganalyzer.Request;

public class RequestPrefixFilter extends AbstractFilter {

	private final String[] _prefixes;

	public RequestPrefixFilter(String... prefixes) {
		super("Requests starting with '" + Arrays.toString(prefixes) + "'");
		if (prefixes == null) {
			throw new NullPointerException("prefix");
		}
		if (prefixes.length == 0) {
			throw new IllegalArgumentException("at least one prefix required");
		}
		_prefixes = prefixes;
	}

	@Override
	public boolean filter(Request request) {
		// TODO performance: Radix Tree anyone?
		String path = request.getPath();
		if (path == null || "".equals(path)) {
			return true;
		}
		for (String prefix : _prefixes) {
			if (path.startsWith(prefix)) {
				return false;
			}
		}
		return true;
	}

}
