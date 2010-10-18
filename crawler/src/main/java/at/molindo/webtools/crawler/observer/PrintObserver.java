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
package at.molindo.webtools.crawler.observer;

import java.util.Observable;
import java.util.Observer;

import at.molindo.webtools.crawler.Crawler;
import at.molindo.webtools.crawler.CrawlerResult;

public final class PrintObserver implements Observer {
	private final boolean _printSuccess;

	public PrintObserver(final boolean printSuccess) {
		_printSuccess = printSuccess;
	}

	@Override
	public void update(final Observable o, final Object arg) {
		final Crawler s = (Crawler) o;

		if (arg instanceof CrawlerResult) {
			final CrawlerResult sr = (CrawlerResult) arg;

			final StringBuilder buf = new StringBuilder();
			buf.append("#").append(s.getRetrievedCount());
			buf.append(" ").append(sr.getUrl());
			buf.append(" ").append(sr.getStatus());
			buf.append(" ").append(sr.getTime()).append("ms");
			if (sr.getErrorMessage() != null) {
				buf.append(" [").append(sr.getErrorMessage()).append("]");
			}

			buf.append(" referrers: ").append(sr.getReferrers());

			if (sr.getStatus() == 200 || sr.getErrorMessage() != null) {
				if (_printSuccess) {
					System.out.println(buf.toString());
				}
			} else if (sr.getStatus() / 100 == 3) {
				// redirect
			} else {
				System.err.println(buf.toString());
			}
		} else if (arg == Crawler.FINISH) {
			System.out.println("finish");
		}
	}
}
