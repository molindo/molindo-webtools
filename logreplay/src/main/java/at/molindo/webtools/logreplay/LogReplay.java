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

package at.molindo.webtools.logreplay;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import at.molindo.utils.io.Compression;
import at.molindo.webtools.crawler.Crawler;
import at.molindo.webtools.crawler.CrawlerReferrer;
import at.molindo.webtools.crawler.CrawlerResult;
import at.molindo.webtools.crawler.CrawlerTask;
import at.molindo.webtools.crawler.ICrawlerHistory;
import at.molindo.webtools.crawler.filter.ContainsStringFilter;
import at.molindo.webtools.crawler.filter.PrefixFilter;
import at.molindo.webtools.crawler.filter.SuffixFilter;
import at.molindo.webtools.crawler.observer.SlowRequestObserver;
import at.molindo.webtools.loganalyzer.LogAnalyzer;
import at.molindo.webtools.loganalyzer.Request;
import at.molindo.webtools.loganalyzer.handler.AbstractLogHandler;

public class LogReplay {

	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("usage: Logreplay [logfile] [host]");
			System.exit(255);
		}

		final File file = "-".equals(args[0]) ? null : new File(args[0]);
		final URL host = new URL(args[1]);
		final String baseUrl = host.getProtocol() + "://" + host.getHost()
				+ (host.getPort() >= 0 ? ":" + host.getPort() : "");

		final int threads = Integer.parseInt(System.getProperty("logreplay.threads", "4"));
		final boolean tidy = Boolean.parseBoolean(System.getProperty("logreplay.tidy", Boolean.FALSE.toString()));
		final Compression compression = Compression.valueOf(System.getProperty("logreplay.compression",
				Compression.AUTO.name()));

		LogAnalyzer a;
		if (file == null) {
			a = LogAnalyzer.stdin(compression);
		} else if (file.isDirectory()) {
			a = LogAnalyzer.directory(file, compression);
		} else {
			a = LogAnalyzer.file(file, compression);
		}

		final Crawler crawler = new Crawler(host.toString(), host.toString(), threads, Integer.MAX_VALUE, tidy) {

			@Override
			protected CrawlerTask newCrawlerTask(final String url, final CrawlerReferrer referrer, final boolean tidy) {
				return new CrawlerTask(this, url, referrer, tidy) {
					@Override
					protected void parseResult(final String string) throws SAXException, IOException {
						// ignore
					}
				};
			}

			@Override
			protected ICrawlerHistory newCrawlerHistory() {
				return new ICrawlerHistory() {
					@Override
					public void report(final CrawlerResult result) {
					}

					@Override
					public boolean queue(final String url, final CrawlerReferrer referrer) {
						return true;
					}

					@Override
					public Map<String, CrawlerResult> getVisitedURLs() {
						return java.util.Collections.emptyMap();
					}
				};
			}

			@Override
			protected BlockingQueue<Runnable> newBlockingQueue() {
				return new LinkedBlockingQueue<Runnable>(10) {

					private static final long serialVersionUID = 1L;

					@Override
					public boolean offer(final Runnable e) {
						try {
							put(e);
							return true;
						} catch (final InterruptedException e1) {
							e1.printStackTrace();
							return false;
						}
					}

				};
			}
		};
		crawler.getFilters().add(new SuffixFilter(".jpg"));
		crawler.getFilters().add(new SuffixFilter(".jpeg"));
		crawler.getFilters().add(new SuffixFilter(".gif"));
		crawler.getFilters().add(new SuffixFilter(".png"));
		crawler.getFilters().add(new SuffixFilter(".ico"));
		crawler.getFilters().add(new SuffixFilter(".xml"));
		crawler.getFilters().add(new PrefixFilter(crawler, "/iframe"));
		crawler.getFilters().add(new PrefixFilter(crawler, "/fanshop"));
		crawler.getFilters().add(new ContainsStringFilter("?wicket:interface="));

		crawler.addObserver(new SlowRequestObserver(400));

		a.addHandler(new AbstractLogHandler("replay") {

			private final Pattern _request = Pattern.compile("^([A-Z]+) (.*) (HTTP/[01]\\.[019])$");

			@Override
			public void report() {
			}

			@Override
			public void handle(final Request request) {
				if (request.getStatus() != 200) {
					return;
				}

				String r = extractRequest(unquote(request.getRequest()));
				if (r == null) {
					return;
				}
				if (!r.startsWith("/")) {
					try {
						r = new URL(r).getFile();
					} catch (final MalformedURLException e) {
						return;
					}
				}

				try {
					final String url = new URL(baseUrl + r).toString();
					crawler.queue(url, new CrawlerReferrer(unquote(request.getReferer()), ""));
				} catch (final MalformedURLException e) {
					System.err.println(e);
				}
			}

			private String extractRequest(final String request) {
				final Matcher m = _request.matcher(request);

				if (m.matches() && "GET".equals(m.group(1)) && !"-".equals(m.group(2))) {
					return m.group(2);
				} else {
					return null;
				}

			}

			private String unquote(final String str) {
				return str.length() > 1 && str.endsWith("\"") && str.startsWith("\"") ? str.substring(1,
						str.length() - 1) : str;
			}
		});
		a.analyze();
	}
}
