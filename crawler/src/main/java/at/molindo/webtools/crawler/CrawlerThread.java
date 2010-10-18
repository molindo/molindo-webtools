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
package at.molindo.webtools.crawler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

public final class CrawlerThread extends Thread {
	private final Crawler _crawler;
	private SAXParser _parser;
	private HttpClient _client;

	CrawlerThread(final Crawler crawler, final Runnable r) {
		super(r);
		_crawler = crawler;
		try {
			_parser = _crawler.getParserFactory().newSAXParser();
			_client = new DefaultHttpClient();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException("failed to create thread", e);
		} catch (final SAXException e) {
			throw new RuntimeException("failed to create thread", e);
		}
	}

	public SAXParser getParser() {
		return _parser;
	}

	public HttpClient getClient() {
		return _client;
	}
}
