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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DTDMemoryCache implements EntityResolver {
	private final Map<Object, byte[]> cache = new HashMap<Object, byte[]>();

	// Remember to handle synchronization issues !!
	// A dumb but easy implementation would synchronize the whole
	// resolveEntity method
	@Override
	public synchronized InputSource resolveEntity(final String publicIdP, String systemIdP) throws SAXException {
		// The PUBLIC id is the key to our cache
		byte[] resultP = cache.get(publicIdP);

		if (resultP == null) {
			final String workingDir = "file://" + System.getProperty("user.dir") + "/";
			if (systemIdP.startsWith(workingDir)) {
				systemIdP = systemIdP.replace(workingDir, "http://www.w3.org/TR/xhtml1/DTD/");
			}

			try {
				// The SYSTEM id is the URL used to fetch the entity
				final URL urlP = new URL(systemIdP);
				final InputStream isP = urlP.openConnection().getInputStream();
				final ByteArrayOutputStream baosP = new ByteArrayOutputStream();

				// We copy the input stream into the output stream
				// Fast buffer implementation
				// I could have used BufferInputStream and OutputStream
				// But it's much slower
				int readP;
				final byte[] bufferP = new byte[1024];
				while ((readP = isP.read(bufferP)) > -1) {
					baosP.write(bufferP, 0, readP);
				}

				resultP = baosP.toByteArray();

				// We store the result in the cache.
				cache.put(publicIdP, resultP);
			} catch (final Exception eP) {
				throw new SAXException(eP);
			}
		} else {
			// System.err.println("found "+publicIdP+ " in cache");
		}

		return new InputSource(new ByteArrayInputStream(resultP));
	}
}
