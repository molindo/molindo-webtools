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

public final class Request {

	private static final int FIELD_IP = 0;
	private static final int FIELD_DATE = 3;
	private static final int FIELD_REQUEST = 4;
	private static final int FIELD_STATUS = 5;
	private static final int FIELD_LENGTH = 6;
	private static final int FIELD_REFERER = 7;
	private static final int FIELD_AGENT = 8;

	int _field;
	private String _previous;

	private String _line;
	private LazyDate _date;
	private int _status;
	private int _length;
	private String[] _values = new String[11];
	private String _method;
	private String _path;
	private String _protocol;

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

	public boolean populate(String line) {
		_line = line;
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

		return (_field == 9) && _status > 0 && _length >= 0;
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
			switch (_field) {
			case FIELD_DATE:
				_date = new LazyDate(string);
				break;
			case FIELD_STATUS:
				_status = getIntField(string);
				break;
			case FIELD_LENGTH:
				_length = getIntField(string);
				break;
			case FIELD_REQUEST:
				int first = string.indexOf(' ');
				int last = string.lastIndexOf(' ');
				if (first > 0 && last > first) {
					_method = string.substring(string.startsWith("\"") ? 1 : 0, first);
					_path = string.substring(first + 1, last);
					_protocol = string.substring(last + 1,
							string.endsWith("\"") ? string.length() - 1 : string.length());
				} else {
					System.err.println("unexpected request line: " + string);
				}
				_values[_field] = string;
				break;
			default:
				_values[_field] = string;
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

	public Date getDate() {
		return _date.getDate();
	}

	public String getRequest() {
		return _values[FIELD_REQUEST];
	}

	public String getReferer() {
		return _values[FIELD_REFERER];
	}

	public String getMethod() {
		return _method;
	}

	public String getPath() {
		return _path;
	}

	public String getProtocol() {
		return _protocol;
	}

	enum FieldState {
		NONE, NORMAL, QUOTES, BRACKETS
	};

	private static class LazyDate {
		// e.g. [24/Feb/2009:13:17:50 +0000]
		private static DateFormat FORMAT = new SimpleDateFormat("[d/MMM/yyyy:HH:mm:ss Z]");
		private static Date ERROR = new Date(0);

		private String _dateStr;
		private Date _date;

		public LazyDate(String date) {
			_dateStr = date;
		}

		public Date getDate() {
			if (_date == null) {
				_date = load();
			}
			return _date == ERROR ? null : _date;
		}

		private Date load() {
			try {
				return FORMAT.parse(_dateStr);
			} catch (ParseException e) {
				return ERROR;
			}
		}
	}

}