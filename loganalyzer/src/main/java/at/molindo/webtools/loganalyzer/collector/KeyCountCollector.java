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

import java.util.Map;

import at.molindo.webtools.loganalyzer.Request;

public abstract class KeyCountCollector<T> extends AbstractCollector {

	private final CounterMap<T> _counters = new CounterMap<T>();
	private int _total = 0;
	private int _reportMax = 25;

	public KeyCountCollector(String name) {
		super(name);
	}

	@Override
	public final void collect(Request request) {
		_total++;
		T key = getKey(request);
		if (key != null) {
			_counters.increment(key);
		}
	}

	@Override
	public final void report() {
		int c = 0;
		for (Map.Entry<Integer, T> e : _counters.toSortedMap().entrySet()) {
			System.out.println(format(e.getValue(), e.getKey(), _total));

			if (++c >= _reportMax) {
				break;
			}
		}
	}

	static String format(Object key, int count, int total) {
		return String.format("%s: %d (%.4f%%)", key, count, 100.0 / total * count);
	}

	public int getReportMax() {
		return _reportMax;
	}

	public KeyCountCollector<T> setReportMax(int reportMax) {
		_reportMax = reportMax;
		return this;
	}

	protected abstract T getKey(Request request);

}
