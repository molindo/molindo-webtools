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

public class RequestCollector extends AbstractCollector {
	private static final int MAX_REPORTED = 64;

	private final HashSet<String> _requests = new HashSet<String>();

	public RequestCollector() {
		super("Requests");
	}

	@Override
	public void report() {
		Collection<String> r;
		if (_requests.size() > MAX_REPORTED) {
			r = new ArrayList<String>(_requests).subList(0, MAX_REPORTED);
		} else {
			r = _requests;
		}

		System.out.println("requests count:  " + _requests.size());
		if (r.size() > 0) {
			System.out.println("requests:        " + r + (_requests.size() > MAX_REPORTED ? " ..." : ""));
		}
	}

	@Override
	public void collect(Request request) {
		_requests.add(request.getRequest());
	}
}