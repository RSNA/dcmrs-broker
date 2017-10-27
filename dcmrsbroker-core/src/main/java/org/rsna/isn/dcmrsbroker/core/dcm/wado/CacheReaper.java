/*
 * Copyright 2017 Radiological Society of North America (RSNA).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Created Oct 18, 2017
 */
package org.rsna.isn.dcmrsbroker.core.dcm.wado;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.rsna.isn.dcmrsbroker.core.util.Environment;
import static org.rsna.isn.dcmrsbroker.core.util.Environment.Key.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread for cleaning up old files in cache
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
class CacheReaper extends Thread
{
	private static final Logger logger
			= LoggerFactory.getLogger(CacheReaper.class);

	private static final File cacheDir;
	
	private static final long maxAge;

	static {
		String path = Environment.getProperty(SCP_CACHE_DIR_PATH);
		cacheDir = new File(path);

		if (!cacheDir.isDirectory()) {
			throw new ExceptionInInitializerError(cacheDir + " is not a directory.");
		}
		
		logger.info("Cache directory set to: {}", cacheDir);

		maxAge = Environment.getPropertyAsInt(SCP_CACHE_MAX_AGE) * 
				 DateUtils.MILLIS_PER_MINUTE;
		
		logger.info("Max age set to: {}", 
					DurationFormatUtils.formatDurationWords(maxAge, true, true));
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				keepRunning = false;
			}

		});
	}

	CacheReaper()
	{
		super("cache-reaper");

		setDaemon(true);
	}

	private static boolean keepRunning = false;

	@Override
	public void run()
	{
		logger.info("Started reaper thread");

		keepRunning = true;
		while (keepRunning) {
			for (File studyDir : cacheDir.listFiles()) {
				if (studyDir.isDirectory()) {
					try {
						long modified = getLastModified(studyDir);
						long age = System.currentTimeMillis() - modified;
						
						if(age >= maxAge) {
							FileUtils.deleteDirectory(studyDir);
							
							logger.warn("Purged directory: {}", studyDir);
						}						
					}
					catch (Exception ex) {
						logger.warn("Error processing directory: "
									+ studyDir, ex);
					}
				}
				else if (studyDir.isFile()) {
					logger.warn("Deleting extraneous file: {}", studyDir);

					studyDir.delete();
				}
			}

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				logger.warn("Reaper thread terminated", ex);

				return;
			}
		}

		logger.info("Stopped reaper thread");
	}

	private static long getLastModified(File studyDir)
	{
		long newest = Long.MIN_VALUE;

		for (File file : (Iterable<File>)()-> FileUtils.iterateFiles(studyDir,
																 TrueFileFilter.INSTANCE,
																 TrueFileFilter.INSTANCE)) {
			newest = Math.max(newest, file.lastModified());
		}
		
		return newest;
	}

}
