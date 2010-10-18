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


public class LogAnalyzerTest extends TestCase {
	public void testParser() {
		Request r = new Request();
		
		// unexpected request line
		boolean success = r.populate("80.74.156.205 -  -  [24/Feb/2009:21:00:10 +0000] \"GET /songtext/primus/the-chastising-of-renegade-53d0d3f5.html\" HTTP/1.0\" 404 17983 \"-\" \"-\" ");
		assertTrue(success);
		
		// jetty
		success = r.populate("62.225.61.178 -  -  [24/Feb/2009:10:08:04 +0000] \"GET /html/STCSearch.xml HTTP/1.1\" 302 0 \"-\" \"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.2; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30)");
		assertTrue(success);
		
		// nginx
		assertTrue(r.populate("127.0.0.1 - - [12/Jan/2010:15:16:01 +0100] \"GET /img/feedback-tab.png HTTP/1.1\" 304 0 \"http://www.local.setlist.fm/\" \"Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/532.8 (KHTML, like Gecko) Chrome/4.0.288.1 Safari/532.8\""));
	}
}
