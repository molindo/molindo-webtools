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
package at.molindo.webtools.loganalyzer.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.molindo.webtools.loganalyzer.Request;
import at.molindo.webtools.loganalyzer.collector.AbstractCollector;
import at.molindo.webtools.loganalyzer.filter.AbstractFilter;

public class DefaultHandler extends AbstractLogHandler {
	private long _requests = 0;
	private long _totalLength = 0;
	private long _noLength = 0;
	private HashMap<Integer, DefaultHandler.PerStatusInfo> _statusCounts = new HashMap<Integer, DefaultHandler.PerStatusInfo>();

	private List<AbstractFilter> _filters = new ArrayList<AbstractFilter>();
	private List<AbstractCollector> _collectors = new ArrayList<AbstractCollector>();

	public DefaultHandler(String name) {
		super(name);
	}

	public DefaultHandler addFilter(AbstractFilter filter) {
		_filters.add(filter);
		return this;
	}

	public DefaultHandler addCollector(AbstractCollector collector) {
		_collectors.add(collector);
		return this;
	}

	@Override
	public void handle(Request request) {
		for (AbstractFilter f : _filters) {
			if (f.filter(request)) {
				return;
			}
		}

		int length = request.getLength();
		int status = request.getStatus();

		_requests++;
		if (length > 0) {
			_totalLength += length;
		} else {
			_noLength++;
		}

		DefaultHandler.PerStatusInfo info = _statusCounts.get(status);
		if (info == null) {
			info = new PerStatusInfo();
			_statusCounts.put(status, info);
		}
		info.increment(length);

		for (AbstractCollector collector : _collectors) {
			collector.collect(request);
		}
	}

	@Override
	public void report() {
		if (_filters.size() > 0) {
			for (AbstractFilter f : _filters) {
				System.out.println();
				System.out.println("Filter: " + f.getName());
			}
			System.out.println();
		}

		System.out.println("requests:        " + _requests);
		System.out.println("no length:       " + _noLength + " ("
				+ (100f / _requests * _noLength) + "%)");
		System.out.println("total length:    "
				+ (_totalLength / (1024 * 1024)) + " MB");
		for (Map.Entry<Integer, DefaultHandler.PerStatusInfo> e : _statusCounts.entrySet()) {
			System.out
					.println(e.getKey() + ":             " + e.getValue());
		}

		if (_collectors.size() > 0) {
			for (AbstractCollector c : _collectors) {
				System.out.println();
				System.out.println("Collector: " + c.getName());
				c.report();
			}
		}
	}

	private static class PerStatusInfo {
		private long _count;
		private long _length;
		private long _noLength;

		@Override
		public String toString() {
			return "count=" + _count + ", length=" + _length
					+ ", noLength=" + _noLength;
		}

		public void increment(int length) {
			_count++;
			if (length == 0) {
				_noLength++;
			} else {
				_length += length;
			}
		}
	}
}