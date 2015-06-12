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
package at.molindo.webtools.loganalyzer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import at.molindo.utils.data.StringUtils;

public final class Request {

	private static final int FIELD_IP = 0;
	private static final int FIELD_DATE = 3;
	private static final int FIELD_REQUEST = 4;
	private static final int FIELD_STATUS = 5;
	private static final int FIELD_LENGTH = 6;
	private static final int FIELD_REFERER = 7;
	private static final int FIELD_AGENT = 8;

	// e.g. [24/Feb/2009:13:17:50 +0000]
	private static DateFormat FORMAT = new SimpleDateFormat("[d/MMM/yyyy:HH:mm:ss Z]");
	private static Date ERROR = new Date(0);

	int _field;
	private String _previous;

	private String _line;
	private int _status;
	private int _length;
	private final String[] _values = new String[11];
	private final LazyDate _date = new LazyDate();
	private final RequestLine _requestLine = new RequestLine();

	public Request() {
	}

	private static int getIntField(String string) {
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
			return -1;
		}
	}

	/**
	 * parses a log entry line in default combined format (see <a
	 * href="http://wiki.nginx.org/NginxHttpLogModule#log_format"
	 * >HttpLogModule#log_format</a>)
	 * 
	 * @param line
	 *            single log entry line in combined format
	 * @return true if entry matched expected format
	 */
	public boolean populate(String line) {
		_line = line;

		_date.clear();
		_requestLine.clear();

		_previous = null;
		_field = 0;

		char[] chars = new char[line.length()];
		line.getChars(0, line.length(), chars, 0);

		int last = -1;
		FieldState state = FieldState.NONE;

		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			switch (state) {
			case NONE:
				switch (c) {
				case ' ':
					// set("");
					last = i;
					break;
				case '"':
					// new quoted field;
					state = FieldState.QUOTES;
					break;
				case '[':
					state = FieldState.BRACKETS;
					break;
				default:
					state = FieldState.NORMAL;
				}
				break;
			case NORMAL:
				if (c == ' ') {
					// end of field
					set(line.substring(last + 1, i));
					last = i;
					state = FieldState.NONE;
				}
				break;
			case BRACKETS:
				if (c == ']' && (chars.length == i + 1 || chars[i + 1] == ' ')) {
					state = FieldState.NORMAL;
				}
				break;
			case QUOTES:
				if (c == '"' && (chars.length == i + 1 || chars[i + 1] == ' ')) {
					state = FieldState.NORMAL;
				}
				break;
			default:
				throw new IllegalStateException("" + state);
			}
		}
		if (last + 1 < line.length()) {
			set(line.substring(last + 1));
		}

		return _field == 9 && _status > 0 && _length >= 0;
	}

	private void set(String string) {
		if (string.endsWith("\"") && !string.startsWith("\"")) {
			// try to fix potential parse error
			_field--;
			_previous = string = _previous + " " + string;
		} else {
			_previous = string;
		}

		if (_field < _values.length) {
			_values[_field] = string;
			switch (_field) {
			case FIELD_STATUS:
				_status = getIntField(string);
				break;
			case FIELD_LENGTH:
				_length = getIntField(string);
				break;
			}
		}
		_field++;
	}

	public String getLine() {
		return _line;
	}

	public int getLength() {
		return _length;
	}

	public int getStatus() {
		return _status;
	}

	public String getAgent() {
		return _values[FIELD_AGENT];
	}

	public String getIp() {
		return _values[FIELD_IP];
	}

	public String getDateString() {
		return _values[FIELD_DATE];
	}

	public Date getDate() {
		return _date.getDate();
	}

	public String getRequestLine() {
		return _values[FIELD_REQUEST];
	}

	public String getReferer() {
		return _values[FIELD_REFERER];
	}

	public String getMethod() {
		return _requestLine.getMethod();
	}

	public String getRequestUri() {
		return _requestLine.getRequestUri();
	}

	public String getProtocol() {
		return _requestLine.getProtocol();
	}

	public String getPath() {
		return _requestLine.getPath();
	}

	public String getQuery() {
		return _requestLine.getQuery();
	}

	public String getFragment() {
		return _requestLine.getFragment();
	}

	public String getPathDirectory() {
		return _requestLine.getPathDirectory();
	}

	public String getPathFile() {
		return _requestLine.getPathFile();
	}

	public String getPathSuffix() {
		return _requestLine.getPathSuffix();
	}

	enum FieldState {
		NONE, NORMAL, QUOTES, BRACKETS
	};

	private class LazyDate {

		private Date _date;

		private LazyDate() {
		}

		private void clear() {
			_date = null;
		}

		private Date getDate() {
			if (_date == null) {
				_date = load();
			}
			return _date == ERROR ? null : _date;
		}

		private Date load() {
			try {
				return FORMAT.parse(getDateString());
			} catch (ParseException e) {
				return ERROR;
			}
		}
	}

	private class RequestLine {

		// load1
		private String _method;
		private String _requestUri;
		private String _protocol;

		// load2
		private String _path;
		private String _query;
		private String _fragment;

		// load3
		private String _pathDirectory;
		private String _pathFile;
		private String _pathSuffix;

		private void clear() {
			_method = null;
			_requestUri = null;
			_protocol = null;

			_path = null;
			_query = null;
			_fragment = null;

			_pathDirectory = null;
			_pathFile = null;
			_pathSuffix = null;
		}

		private String getMethod() {
			if (_method == null) {
				load1();
			}
			return _method;
		}

		private String getRequestUri() {
			if (_requestUri == null) {
				load1();
			}
			return _requestUri;
		}

		private String getProtocol() {
			if (_protocol == null) {
				load1();
			}
			return _protocol;
		}

		private void load1() {
			String string = getRequestLine();
			int first = string.indexOf(' ');
			int last = string.lastIndexOf(' ');
			if (first > 0 && last > first) {
				_method = string.substring(string.startsWith("\"") ? 1 : 0, first);
				_requestUri = string.substring(first + 1, last);
				_protocol = string.substring(last + 1, string.endsWith("\"") ? string.length() - 1 : string.length());
			} else {
				System.err.println("unexpected request line: " + string);
				_method = _requestUri = _protocol = "";
			}
		}

		private String getPath() {
			if (_path == null) {
				load2();
			}
			return _path;
		}

		private String getQuery() {
			if (_query == null) {
				load2();
			}
			return _query;
		}

		private String getFragment() {
			if (_fragment == null) {
				load2();
			}
			return _fragment;
		}

		private void load2() {
			String requestUri = getRequestUri(); // invokes load1()

			int frag = requestUri.indexOf('#');
			if (frag > 0) {
				_fragment = requestUri.substring(frag + 1);
				requestUri = requestUri.substring(0, frag);
			} else {
				_fragment = "";
			}

			int query = requestUri.indexOf('?');
			if (query > 0) {
				_query = requestUri.substring(query + 1);
				requestUri = requestUri.substring(0, query);
			} else {
				_query = "";
			}

			_path = requestUri;
		}

		private String getPathDirectory() {
			if (_pathDirectory == null) {
				load3();
			}
			return _pathDirectory;
		}

		private String getPathFile() {
			if (_pathFile == null) {
				load3();
			}
			return _pathFile;
		}

		private String getPathSuffix() {
			if (_pathSuffix == null) {
				load3();
			}
			return _pathSuffix;
		}

		private void load3() {
			String path = getPath(); // invokes load2() and load1()
			String directory = StringUtils.beforeLast(path, "/");
			if (directory.equals("")) {
				// make root directory "/"
				_pathDirectory = "/";
			} else {
				_pathDirectory = directory;
			}
			_pathFile = path.substring(directory.length() + 1);
			_pathSuffix = StringUtils.afterLast(_pathFile, ".");
		}

	}
}