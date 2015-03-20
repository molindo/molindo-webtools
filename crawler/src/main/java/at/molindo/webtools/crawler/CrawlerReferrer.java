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

import java.io.Serializable;

public final class CrawlerReferrer implements Serializable, Comparable<CrawlerReferrer> {
	private static final long serialVersionUID = 1L;

	private final String _referrerUrl;
	private final String _href;

	public CrawlerReferrer(final String referrerUrl, final String href) {
		_referrerUrl = referrerUrl;
		_href = href;
	}

	public String getReferrerUrl() {
		return _referrerUrl;
	}

	public String getHref() {
		return _href;
	}

	@Override
	public int compareTo(final CrawlerReferrer o) {
		if (o == null) {
			return -1;
		}
		final int val = getReferrerUrl().compareTo(o.getReferrerUrl());
		if (val != 0) {
			return val;
		}
		return getHref().compareTo(o.getHref());
	}

	@Override
	public String toString() {
		return _referrerUrl + "|" + _href;
	}
}
