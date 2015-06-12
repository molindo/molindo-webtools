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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import at.molindo.utils.data.ComparatorUtils;
import at.molindo.utils.io.Compression;
import at.molindo.utils.io.FileUtils;
import at.molindo.utils.io.StreamUtils;
import at.molindo.webtools.loganalyzer.collector.AgentCollector;
import at.molindo.webtools.loganalyzer.handler.DefaultHandler;
import at.molindo.webtools.loganalyzer.handler.Handler;

/**
 * main class that
 * 
 * 
 * @author stf
 */
public class LogAnalyzer {

	private static final String FLAG_GZIP = "--gzip";
	private static final String FLAG_BZIP2 = "--bzip2";
	private static final String FLAG_DIRECTORY = "--directory";

	private final List<Handler> _handlers = new ArrayList<Handler>();

	private InputStream[] _ins;

	private final long _start = System.currentTimeMillis();
	private int _lineCount = 0;
	private int _skippedCount = 0;
	private int _progress = 0;

	public LogAnalyzer() {
	}

	public LogAnalyzer setProgressStep(int progress) {
		_progress = progress;
		return this;
	}

	public LogAnalyzer addHandler(final Handler handler) {
		_handlers.add(handler);
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
		if (_handlers.size() == 0) {
			throw new IllegalStateException("no log handlers");
		}

		if (_ins == null || _ins.length == 0) {
			throw new IllegalStateException("no input stream to read");
		}

		for (final Handler h : _handlers) {
			h.onBeforeAnalyze();
		}
		for (final InputStream in : _ins) {
			for (final Handler h : _handlers) {
				h.onBeforeFile();
			}
			try {
				final BufferedReader r = new BufferedReader(new InputStreamReader(in));
				String line;
				final Request request = new Request(); // reuse

				while ((line = r.readLine()) != null) {
					_lineCount++;

					if (_progress > 0 && _lineCount % _progress == 0) {
						System.out.println("processed " + _lineCount + " lines in "
								+ (System.currentTimeMillis() - _start) + "ms");
					}

					if (!request.populate(line)) {
						_skippedCount++;
						System.err.println("skipping illegal line: '" + line + "'");
						continue;
					}

					for (final Handler h : _handlers) {
						h.handle(request);
					}
				}

				r.close();
			} finally {
				for (final Handler h : _handlers) {
					h.onAfterFile();
				}
			}
		}
		for (final Handler h : _handlers) {
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

		for (final Handler h : _handlers) {
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
		final File[] files = directory.listFiles();
		if (files == null || files.length == 0) {
			throw new IllegalArgumentException("directory is empty: " + directory.getAbsolutePath());
		}
		return files(files, compression);
	}

	public static LogAnalyzer files(final File[] files, final Compression compression) throws FileNotFoundException,
			IOException {

		if (files == null || files.length == 0) {
			throw new IllegalArgumentException("no files");
		}

		// sort files alphabetically TODO allow other sort options
		List<File> list = new ArrayList<File>(Arrays.asList(files));
		Collections.sort(list, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				return ComparatorUtils.nullLowCompareTo(o1.getName(), o2.getName());
			}
		});

		final InputStream[] ins = new InputStream[files.length];
		int i = 0;
		for (File file : files) {
			ins[i++] = FileUtils.in(file, compression);
		}

		return new LogAnalyzer().setInputStreams(ins);
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

	public static LogAnalyzer args(String[] args) throws IOException {
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

		return a;

	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		LogAnalyzer a = args(args);

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
