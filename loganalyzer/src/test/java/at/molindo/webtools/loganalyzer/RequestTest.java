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

package at.molindo.webtools.loganalyzer;

import junit.framework.TestCase;

public class RequestTest extends TestCase {
	public void testParseRequestLine() {
		final String format = "201.51.151.118 - - [26/Aug/2011:21:00:04 +0000] \"GET %s HTTP/1.1\" 200 38 \"-\" \"Mozilla/4.0 (compatible)\"";

		Request r = new Request();

		r.populate(String.format(format, "/"));
		assertEquals("/", r.getRequestUri());
		assertEquals("/", r.getPath());
		assertEquals("", r.getQuery());
		assertEquals("", r.getFragment());
		assertEquals("/", r.getPathDirectory());
		assertEquals("", r.getPathFile());
		assertEquals("", r.getPathSuffix());

		r.populate(String.format(format, "/foo"));
		assertEquals("/foo", r.getRequestUri());
		assertEquals("/foo", r.getPath());
		assertEquals("", r.getQuery());
		assertEquals("", r.getFragment());
		assertEquals("/", r.getPathDirectory());
		assertEquals("foo", r.getPathFile());
		assertEquals("", r.getPathSuffix());

		r.populate(String.format(format, "/foo.txt"));
		assertEquals("/foo.txt", r.getRequestUri());
		assertEquals("/foo.txt", r.getPath());
		assertEquals("", r.getQuery());
		assertEquals("", r.getFragment());
		assertEquals("/", r.getPathDirectory());
		assertEquals("foo.txt", r.getPathFile());
		assertEquals("txt", r.getPathSuffix());

		r.populate(String.format(format, "/bar/foo.txt"));
		assertEquals("/bar/foo.txt", r.getRequestUri());
		assertEquals("/bar/foo.txt", r.getPath());
		assertEquals("", r.getQuery());
		assertEquals("", r.getFragment());
		assertEquals("/bar", r.getPathDirectory());
		assertEquals("foo.txt", r.getPathFile());
		assertEquals("txt", r.getPathSuffix());

		r.populate(String.format(format, "/bar/foo.txt?baz=1"));
		assertEquals("/bar/foo.txt?baz=1", r.getRequestUri());
		assertEquals("/bar/foo.txt", r.getPath());
		assertEquals("baz=1", r.getQuery());
		assertEquals("", r.getFragment());
		assertEquals("/bar", r.getPathDirectory());
		assertEquals("foo.txt", r.getPathFile());
		assertEquals("txt", r.getPathSuffix());

		r.populate(String.format(format, "/bar/foo.txt?baz=1#qux"));
		assertEquals("/bar/foo.txt?baz=1#qux", r.getRequestUri());
		assertEquals("/bar/foo.txt", r.getPath());
		assertEquals("baz=1", r.getQuery());
		assertEquals("qux", r.getFragment());
		assertEquals("/bar", r.getPathDirectory());
		assertEquals("foo.txt", r.getPathFile());
		assertEquals("txt", r.getPathSuffix());

		r.populate(String.format(format, "/bar/foo.txt?#"));
		assertEquals("/bar/foo.txt?#", r.getRequestUri());
		assertEquals("/bar/foo.txt", r.getPath());
		assertEquals("", r.getQuery());
		assertEquals("", r.getFragment());
		assertEquals("/bar", r.getPathDirectory());
		assertEquals("foo.txt", r.getPathFile());
		assertEquals("txt", r.getPathSuffix());
	}
}
