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

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

public final class CrawlerResult implements Serializable {
	private static final long serialVersionUID = 1L;

	private String _url;
	private Date _date = new Date();
	private int _time;
	private int _status;
	private String _errorMessage;
	private String _contentType;
	private String _text;
	private SortedSet<CrawlerReferrer> _referrers = Collections.synchronizedSortedSet(new TreeSet<CrawlerReferrer>());

	public String getUrl() {
		return _url;
	}

	public void setUrl(final String url) {
		_url = url;
	}

	public Date getDate() {
		return _date;
	}

	public void setDate(final Date date) {
		_date = date;
	}

	public void setStatus(final int status) {
		_status = status;
	}

	public int getStatus() {
		return _status;
	}

	public void setErrorMessage(final String message) {
		_errorMessage = message;
	}

	public String getErrorMessage() {
		return _errorMessage;
	}

	public String getText() {
		return _text;
	}

	public void setText(final String text) {
		_text = text;
	}

	public void setTime(final int time) {
		_time = time;
	}

	public int getTime() {
		return _time;
	}

	public String getContentType() {
		return _contentType;
	}

	public void setContentType(final String contentType) {
		_contentType = contentType;
	}

	public void setReferrers(final SortedSet<CrawlerReferrer> referrer) {
		_referrers = referrer;
	}

	public SortedSet<CrawlerReferrer> getReferrers() {
		return _referrers;
	}

}
