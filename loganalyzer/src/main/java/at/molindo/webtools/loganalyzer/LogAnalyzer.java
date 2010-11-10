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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import at.molindo.utils.io.Compression;
import at.molindo.utils.io.FileUtils;
import at.molindo.utils.io.StreamUtils;
import at.molindo.webtools.loganalyzer.collector.AgentCollector;
import at.molindo.webtools.loganalyzer.handler.AbstractLogHandler;
import at.molindo.webtools.loganalyzer.handler.DefaultHandler;

public class LogAnalyzer {

	private static final String FLAG_GZIP = "--gzip";
	private static final String FLAG_BZIP2 = "--bzip2";
	private static final String FLAG_DIRECTORY = "--directory";

	private final List<AbstractLogHandler> _logHandlers = new ArrayList<AbstractLogHandler>();

	private InputStream[] _ins;

	long _start = System.currentTimeMillis();
	int _lineCount = 0;
	int _skippedCount = 0;

	public LogAnalyzer() {
	}

	public LogAnalyzer addHandler(final AbstractLogHandler handler) {
		_logHandlers.add(handler);
		return this;
	}

	public LogAnalyzer setInputStream(final InputStream in) {
		return setInputStreams(in);
	}

	public LogAnalyzer setInputStreams(final InputStream... ins) {
		_ins = ins;
		return this;
	}

	public LogAnalyzer analyze() throws IOException {
		if (_logHandlers.size() == 0) {
			throw new IllegalStateException("no log handlers");
		}

		if (_ins == null || _ins.length == 0) {
			throw new IllegalStateException("no input stream to read");
		}

		for (final AbstractLogHandler h : _logHandlers) {
			h.onBeforeAnalyze();
		}
		for (final InputStream in : _ins) {
			for (final AbstractLogHandler h : _logHandlers) {
				h.onBeforeFile();
			}
			try {
				final BufferedReader r = new BufferedReader(new InputStreamReader(in));
				String line;
				final Request request = new Request(); // reuse

				while ((line = r.readLine()) != null) {
					_lineCount++;

					if (!request.populate(line)) {
						_skippedCount++;
						System.err.println("skipping illegal line: '" + line + "'");
						continue;
					}

					for (final AbstractLogHandler h : _logHandlers) {
						h.handle(request);
					}
				}

				r.close();
			} finally {
				for (final AbstractLogHandler h : _logHandlers) {
					h.onAfterFile();
				}
			}
		}
		for (final AbstractLogHandler h : _logHandlers) {
			h.onAfterAnalyze();
		}
		return this;
	}

	public LogAnalyzer report() {
		final float seconds = (System.currentTimeMillis() - _start) / 1000f;

		System.out.println("lines:           " + _lineCount);
		System.out.println("skipped:         " + _skippedCount + " (" + 100f / _lineCount * _skippedCount + "%)");
		System.out.println("time to analyze: " + seconds + " s");
		System.out.println("analysis speed:  " + _lineCount / seconds + " lines/s");

		for (final AbstractLogHandler h : _logHandlers) {
			System.out.println();
			System.out.println("Handler: " + h.getName());
			h.report();
		}
		return this;
	}

	public static LogAnalyzer directory(final File directory) throws FileNotFoundException, IOException {
		return directory(directory, Compression.AUTO);
	}

	public static LogAnalyzer directory(final File directory, final Compression compression)
			throws FileNotFoundException, IOException {
		if (directory == null) {
			throw new NullPointerException("directory");
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + directory);
		}
		if (compression == null) {
			throw new NullPointerException("compression");
		}
		final LogAnalyzer a = new LogAnalyzer();
		final File[] files = directory.listFiles();
		if (files == null || files.length == 0) {
			throw new IllegalArgumentException("directory is empty: " + directory.getAbsolutePath());
		}
		final InputStream[] ins = new InputStream[files.length];
		for (int i = 0; i < files.length; i++) {
			ins[i] = FileUtils.in(files[i], compression);
		}
		a.setInputStreams(ins);
		return a;
	}

	public static LogAnalyzer file(final File file) throws FileNotFoundException, IOException {
		return file(file, Compression.AUTO);
	}

	public static LogAnalyzer file(final File file, final Compression compression) throws FileNotFoundException,
			IOException {
		if (file == null) {
			throw new NullPointerException("file");
		}
		if (compression == null) {
			throw new NullPointerException("compression");
		}
		final LogAnalyzer a = new LogAnalyzer();
		a.setInputStream(FileUtils.in(file, compression));
		return a;
	}

	public static LogAnalyzer stdin() throws IOException {
		return stdin(Compression.AUTO);
	}

	public static LogAnalyzer stdin(Compression compression) throws IOException {
		if (compression == null) {
			throw new NullPointerException("compression");
		}
		if (compression == Compression.AUTO) {
			compression = Compression.NONE;
		}
		final LogAnalyzer a = new LogAnalyzer();
		a.setInputStream(StreamUtils.decompress(System.in, compression));
		return a;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		if (args.length == 0) {
			throw new IllegalArgumentException("file missing");
		}
		final HashSet<String> argsSet = new HashSet<String>(Arrays.asList(args).subList(0, args.length - 1));

		final String fileName = args[args.length - 1];
		File file = null;

		final boolean readStdin = "-".equals(fileName);
		Compression compression = Compression.AUTO;

		if (argsSet.remove(FLAG_GZIP)) {
			compression = Compression.GZIP;
		}
		if (argsSet.remove(FLAG_BZIP2)) {
			compression = Compression.BZIP2;
		}
		boolean directory = argsSet.remove(FLAG_DIRECTORY);

		if (!readStdin) {
			file = new File(fileName);
			if (!file.exists()) {
				throw new IllegalArgumentException("file does not exist: " + file.getAbsolutePath());
			}
			directory = directory || file.isDirectory();
		} else {
			if (directory) {
				throw new IllegalArgumentException("can't read directory from stdin");
			}
		}

		if (argsSet.size() > 0) {
			throw new IllegalArgumentException("unknown argument(s): " + argsSet);
		}

		LogAnalyzer a;
		if (readStdin) {
			a = LogAnalyzer.stdin(compression);
		} else if (!directory) {
			a = LogAnalyzer.file(file, compression);
		} else {
			a = LogAnalyzer.directory(file, compression);
		}

		// a.addHandler(new DefaultHandler("All"));

		a.addHandler(new DefaultHandler("Agents")
		// .addFilter(new AgentFilter("Googlebot", false))
		// .addFilter(new StatusFilter(302))
				.addCollector(new AgentCollector())
		// .addCollector(new RefererCollector())
		// .addCollector(new RequestCollector())
		// .addFilter(new RequestPrefixFilter("/img", "/script", "/css"))
		// .addFilter(new RequestPrefixFilter("widgets/setlist-image"))
		);

		// a.addHandler(new DefaultHandler("all"));
		// a.addHandler(new DefaultHandler("resources").addFilter(new
		// RequestPrefixFilter("/img", "/script", "/css")));
		// a.addHandler(new DefaultHandler("widget").addFilter((new
		// RequestPrefixFilter("/widgets/setlist-image"))));

		// a.addHandler(
		// new DefaultHandler("Mediapartners-Google")
		// .addFilter(new AgentFilter("Mediapartners-Google", true)));

		a.analyze().report();
	}
}
