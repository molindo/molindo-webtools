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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.w3c.tidy.Tidy;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import at.molindo.utils.io.StreamUtils;

public class CrawlerTask implements Runnable {

	private final Crawler _crawler;
	private final String _urlString;
	private final CrawlerReferrer _referrer;
	private boolean _tidy = true;

	public CrawlerTask(final Crawler crawler, final String url, final CrawlerReferrer referrer, final boolean tidy) {
		_crawler = crawler;
		_urlString = url;
		_referrer = referrer;
		_tidy = tidy;
	}

	public String getUrlString() {
		return _urlString;
	}

	public CrawlerReferrer getReferrer() {
		return _referrer;
	}

	@Override
	public void run() {
		if (Thread.currentThread() instanceof CrawlerThread == false) {
			throw new Error("not a cralwer thread");
		}

		final CrawlerResult sr = new CrawlerResult();
		sr.setUrl(_urlString);
		sr.getReferrers().add(_referrer);

		final HttpGet get = new HttpGet(_urlString);
		// get.setFollowRedirects(false);

		try {
			final long start = System.currentTimeMillis();

			final HttpResponse response = ((CrawlerThread) Thread.currentThread()).getClient().execute(get);

			sr.setStatus(response.getStatusLine().getStatusCode());
			sr.setTime((int) (System.currentTimeMillis() - start));

			final Header[] contentTypeHeader = response.getHeaders("Content-Type");
			sr.setContentType(contentTypeHeader == null || contentTypeHeader.length == 0 ? null : contentTypeHeader[0]
					.getValue());

			final String encoding = response.getEntity().getContentEncoding() == null ? null : response.getEntity()
					.getContentEncoding().getValue();

			final Object content = consumeContent(response.getEntity().getContent(), sr.getContentType(), response
					.getEntity().getContentLength(), encoding);

			if (sr.getStatus() / 100 == 3) {
				String redirectLocation;
				final Header[] locationHeader = response.getHeaders("location");
				if (locationHeader != null && locationHeader.length > 0) {
					redirectLocation = locationHeader[0].getValue();
					if (redirectLocation.startsWith("/")) {
						redirectLocation = _crawler._host + redirectLocation.substring(1);
					}
					_crawler.queue(redirectLocation, new CrawlerReferrer(_urlString, response.getStatusLine()
							.getReasonPhrase() + ": " + _referrer));
				} else {
					System.err.println("redirect without location from " + _urlString);
				}
			} else if (sr.getStatus() == HttpStatus.SC_OK) {
				if (content instanceof String) {
					sr.setText((String) content);

					if (sr.getContentType().startsWith("text/html")) {
						parseResult(sr.getText());
					}
				}
			}
		} catch (final MalformedURLException e) {
			sr.setErrorMessage(e.getMessage());
			// e.printStackTrace();
		} catch (final IOException e) {
			sr.setErrorMessage(e.getMessage());
			e.printStackTrace();
		} catch (final SAXException e) {
			sr.setErrorMessage(e.getMessage());
			// e.printStackTrace();
		} catch (final Throwable t) {
			t.printStackTrace();
		} finally {
			_crawler.report(sr);
			// response.releaseConnection();
		}

	}

	private Object consumeContent(final InputStream content, String contentType, final long contentLength,
			final String encoding) throws IOException {
		if (contentType == null) {
			contentType = "";
		}

		try {
			if (contentType.startsWith("text/")) {
				final BufferedReader r = new BufferedReader(new InputStreamReader(content, encoding == null ? "utf-8"
						: encoding));

				String line;
				final StringBuilder buf = new StringBuilder();

				while ((line = r.readLine()) != null) {
					buf.append(line).append("\n");
				}
				if (buf.length() > 0) {
					buf.setLength(buf.length() - 1);
				}
				return buf.toString();
			} else {
				final ByteArrayOutputStream out = new ByteArrayOutputStream(contentLength > 0
						&& contentLength <= Integer.MAX_VALUE ? (int) contentLength : 4096);
				StreamUtils.copy(content, out, 4096);
				final byte[] bytes = out.toByteArray();
				out.flush();
				out.close();
				return bytes;
			}
		} finally {
			try {
				content.close();
			} catch (final IOException e) {
				// ignore
			}
		}
	}

	protected void parseResult(final String string) throws SAXException, IOException {

		InputSource inputSource;
		if (_tidy) {
			final Tidy tidy = new Tidy();
			tidy.setXHTML(true);
			tidy.setErrfile("/dev/null");
			final ByteArrayInputStream in = new ByteArrayInputStream(string.getBytes());
			final ByteArrayOutputStream out = new ByteArrayOutputStream();

			tidy.parse(in, out);

			inputSource = new InputSource(new ByteArrayInputStream(out.toByteArray()));
		} else {
			inputSource = new InputSource(new StringReader(string));
		}

		((CrawlerThread) Thread.currentThread()).getParser().parse(inputSource, new DefaultHandler() {

			@Override
			public void startElement(final String uri, final String localName, final String name,
					final Attributes attributes) throws SAXException {

				if ("a".equals(name)) {
					String href = attributes.getValue("href");
					if (href != null) {
						final int anchorIndex = href.lastIndexOf("#");
						if (anchorIndex > 0) {
							href = href.substring(0, anchorIndex);
						} else if (anchorIndex == 0) {
							// anchor on same page: ignore
							return;
						}

						if (href != null) {
							final CrawlerReferrer referrer = new CrawlerReferrer(_urlString, href);
							if (!href.startsWith("http://")) {
								if (href.startsWith("/")) {
									_crawler.queue(_crawler._host + href.substring(1), referrer);
								} else if (!href.startsWith("javascript:") && !href.startsWith("ftp:")
										&& !href.startsWith("mailto:")) {
									String relativeTo = _urlString.substring(0, _urlString.lastIndexOf("/"));
									boolean one = false, two = false;
									while ((two = href.startsWith("../")) || (one = href.startsWith("./"))) {
										if (two) {
											href = href.substring(3);
											relativeTo = relativeTo.substring(0, relativeTo.lastIndexOf("/"));
										} else if (one) {
											href = href.substring(2);
										}
									}

									_crawler.queue(relativeTo + "/" + href, referrer);
								}
							} else if (href.startsWith(_crawler._host)) {
								_crawler.queue(href, referrer);
							}
						}
					}
				}
			}

			@Override
			public InputSource resolveEntity(final String publicId, String systemId) throws IOException, SAXException {
				if ("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd".equals(systemId)) {
					systemId = getClass().getClassLoader().getResource("xhtml1-transitional.dtd").toString();
				}

				return _crawler.getDtdMemoryCache().resolveEntity(publicId, systemId);
			}
		});
	}
}
