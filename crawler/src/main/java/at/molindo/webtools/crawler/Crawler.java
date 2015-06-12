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

import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParserFactory;

import at.molindo.webtools.crawler.filter.ICrawlerFilter;
import at.molindo.webtools.crawler.filter.PrefixFilter;
import at.molindo.webtools.crawler.observer.ExitObserver;
import at.molindo.webtools.crawler.observer.PrintObserver;

import com.sun.org.apache.xerces.internal.parsers.XIncludeAwareParserConfiguration;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParserConfiguration;

public class Crawler extends Observable {

	public static final Object FINISH = new Object();

	private ThreadPoolExecutor _executor;
	final String _host;
	private final String _username;
	private final String _password;
	private int _max;
	private ICrawlerHistory _history;
	private String _start;
	private int _dispatchedCount;
	private int _retrievedCount;
	private boolean _tidy;
	private SAXParserFactory _parserFactory;
	private DTDMemoryCache _dtdMemoryCache;

	private final List<ICrawlerFilter> _filters = new CopyOnWriteArrayList<ICrawlerFilter>();

	public Crawler(final String host, final String start, final int threads, final int max, final boolean tidy) {
		this(host, null, null, start, threads, max, tidy);
	}

	public Crawler(final String host, final String username, final String password, final String start,
			final int threads, final int max, final boolean tidy) {
		_host = host.endsWith("/") ? host : host + "/";
		_start = start.startsWith(_host) ? start : _host + (start.startsWith("/") ? start.substring(1) : start);
		_tidy = tidy;
		_username = username;
		_password = password;

		final XMLParserConfiguration config = new XIncludeAwareParserConfiguration();
		config.setProperty("http://apache.org/xml/properties/internal/grammar-pool", new XMLGrammarPoolImpl());

		_parserFactory = SAXParserFactory.newInstance();
		_dtdMemoryCache = new DTDMemoryCache();

		_executor = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, newBlockingQueue(),
				new ThreadFactory() {

			@Override
			public Thread newThread(final Runnable r) {
				return new CrawlerThread(Crawler.this, r);
			}

		});
		_executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {

			@Override
			public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
				if (r instanceof CrawlerTask) {
					// CrawlerTask task = (CrawlerTask) r;
					// System.err.println("rejected execution: " +
					// task.getUrlString());
				}
			}
		});

		_max = max > 0 ? max : Integer.MAX_VALUE;

		_history = newCrawlerHistory();

		queue(_start, null);

	}

	protected BlockingQueue<Runnable> newBlockingQueue() {
		return new LinkedBlockingQueue<Runnable>(10000);
	}

	protected ICrawlerHistory newCrawlerHistory() {
		return new CrawlerHistory();
	}

	public void queue(String url, final CrawlerReferrer referrer) {
		if (_dispatchedCount < _max) {
			url = prepareUrl(url);
			if (url == null) {
				return;
			}

			if (_history.queue(url, referrer)) {
				final CrawlerTask task = newCrawlerTask(url, referrer, _tidy);

				for (final ICrawlerFilter filter : _filters) {
					if (filter.filter(task)) {
						return;
					}
				}

				_executor.execute(task);
				_dispatchedCount++;
			}

			if (_dispatchedCount == _max) {
				// reached max
				System.out.println("reached dispatch max");
				_executor.shutdown();
			}
		}
	}

	protected CrawlerTask newCrawlerTask(final String url, final CrawlerReferrer referrer, final boolean tidy) {
		return new CrawlerTask(this, url, referrer, tidy);
	}

	private String prepareUrl(final String url) {
		int jsessionidIndex;
		if ((jsessionidIndex = url.indexOf(";jsessionid=")) >= 0) {
			final String path = url.substring(0, jsessionidIndex);
			final int queryStringIndex = url.indexOf("?");
			if (queryStringIndex >= 0) {
				return path + url.substring(queryStringIndex);
			} else {
				return path;
			}
		}
		return url;
	}

	protected void report(final CrawlerResult result) {
		_history.report(result);

		_retrievedCount++;
		setChanged();
		notifyObservers(result);

		if (_dispatchedCount == _retrievedCount) {
			setChanged();
			notifyObservers(FINISH);
		}
	}

	public Map<String, CrawlerResult> getVisitedURLs() {
		return _history.getVisitedURLs();
	}

	public int getDispatchedCount() {
		return _dispatchedCount;
	}

	public int getRetrievedCount() {
		return _retrievedCount;
	}

	public List<ICrawlerFilter> getFilters() {
		return _filters;
	}

	public String getHost() {
		return _host;
	}

	public SAXParserFactory getParserFactory() {
		return _parserFactory;
	}

	public DTDMemoryCache getDtdMemoryCache() {
		return _dtdMemoryCache;
	}

	public static void main(final String[] args) throws InterruptedException {
		System.out.println("starting crawler");

		final String host = "http://localhost:8080/";
		final String start = host + "";
		final int threads = 4;
		final int max = 0;
		final boolean tidy = false;

		final Crawler s = new Crawler(host, start, threads, max, tidy);

		s.getFilters().add(new PrefixFilter(s, "?wicket:interface="));

		s.addObserver(new PrintObserver(true));
		s.addObserver(new ExitObserver());
	}

	public String getUsername() {
		return _username;
	}

	public String getPassword() {
		return _password;
	}
}
