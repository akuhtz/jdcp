/*
 * Copyright (c) 2008 Bradley W. Kimmel
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.eandb.jdcp.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;


import ca.eandb.jdcp.remote.JobService;
import ca.eandb.util.StringUtil;

/**
 * @author brad
 *
 */
public final class FileCachingJobServiceClassLoaderStrategy extends
		CachingJobServiceClassLoaderStrategy {

	private final File directory;

	/**
	 * @param service
	 * @param jobId
	 * @param directory
	 */
	public FileCachingJobServiceClassLoaderStrategy(JobService service,
			UUID jobId, File directory) {
		super(service, jobId);
		this.directory = directory;
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("directory must be a directory.");
		}
	}

	/**
	 * @param service
	 * @param jobId
	 * @param directory
	 */
	public FileCachingJobServiceClassLoaderStrategy(JobService service,
			UUID jobId, String directory) {
		this(service, jobId, new File(directory));
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.CachingJobServiceClassLoaderStrategy#cacheLookup(java.lang.String, byte[])
	 */
	@Override
	protected byte[] cacheLookup(String name, byte[] digest) {
		File file = getCacheEntryFile(name, digest, false);
		byte[] def = null;

		if (file.exists()) {
			try {
				FileInputStream stream = new FileInputStream(file);
				def = new byte[(int) file.length()];
				stream.read(def);
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
				def = null;
			}
		}

		return def;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.CachingJobServiceClassLoaderStrategy#cacheStore(java.lang.String, byte[], byte[])
	 */
	@Override
	protected void cacheStore(String name, byte[] digest, byte[] def) {
		File file = getCacheEntryFile(name, digest, true);
		try {
			FileOutputStream stream = new FileOutputStream(file);
			stream.write(def);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File getCacheEntryFile(String name, byte[] digest, boolean createDirectory) {
		File entryDirectory = new File(directory, name.replace('.', '/'));
		if (createDirectory && !entryDirectory.isDirectory()) {
			entryDirectory.mkdirs();
		}
		return new File(entryDirectory, StringUtil.toHex(digest));
	}

}
