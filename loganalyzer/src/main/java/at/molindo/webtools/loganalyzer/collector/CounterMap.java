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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class CounterMap<T> implements Iterable<Map.Entry<T, Integer>> {
	private final Map<T, Integer> _counters = new HashMap<T, Integer>();

	public void increment(T key) {
		Integer c = _counters.put(key, 1);
		if (c != null) {
			_counters.put(key, c + 1);
		}
	}

	@Override
	public Iterator<Entry<T, Integer>> iterator() {
		return _counters.entrySet().iterator();
	}

	@Override
	public String toString() {
		return _counters.toString();
	}

	public SortedMap<Integer, T> toSortedMap() {
		SortedMap<Integer, T> map = new TreeMap<Integer, T>(Collections.reverseOrder());

		for (Entry<T, Integer> e : _counters.entrySet()) {
			map.put(e.getValue(), e.getKey());
		}

		return map;
	}

}
