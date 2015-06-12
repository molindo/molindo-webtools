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
package at.molindo.webtools.crawler;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrawlerHistory implements ICrawlerHistory {

	private static final CrawlerResult NULL = new CrawlerResult();

	private final ConcurrentHashMap<String, CrawlerResult> _visitedURLs = new ConcurrentHashMap<String, CrawlerResult>();

	@Override
	public boolean queue(final String url, final CrawlerReferrer referrer) {
		final CrawlerResult sr = _visitedURLs.putIfAbsent(url, NULL);
		if (sr != null) {
			sr.getReferrers().add(referrer);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Map<String, CrawlerResult> getVisitedURLs() {
		return Collections.unmodifiableMap(_visitedURLs);
	}

	@Override
	public void report(final CrawlerResult result) {
		final CrawlerResult sr = _visitedURLs.put(result.getUrl(), result);
		if (sr == null || sr != NULL) {
			System.err.println("unexpected report for url " + result.getUrl());
		}
	}
}
