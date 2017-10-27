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
 */
package org.rsna.isn.dcmrsbroker.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for ExecutorService instances
 * 
 * @author Wyatt Tellis
 * @version 1.0.0
 * @since 1.0.0
 */
public class ExecutorServiceFactory
{
	private static final Logger logger = LoggerFactory.getLogger(ExecutorServiceFactory.class);

	private static ExecutorService executorService;

	private static ScheduledExecutorService scheduledExecutorService;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread()
		{

			@Override
			public void run()
			{
				shutdownAll();
			}

		});
	}

	private ExecutorServiceFactory()
	{
	}

	private static synchronized void init()
	{
		if (executorService == null) {
			executorService = Executors
					.newCachedThreadPool();
		}


		if (scheduledExecutorService == null) {
			scheduledExecutorService = Executors
					.newScheduledThreadPool(5);
		}
	}

	public static ExecutorService getService()
	{
		init();

		return executorService;
	}

	public static ScheduledExecutorService getScheduledService()
	{
		init();

		return scheduledExecutorService;
	}

	private static void shutdown(ExecutorService service)
	{
		try {
			service.shutdown();

			boolean done = service.awaitTermination(1, TimeUnit.MINUTES);
			if (!done) {
				// Force shutdown
				service.shutdownNow();
			}
		}
		catch (InterruptedException ex) {
			logger.warn("Uncaught exception while shutting down service: "
						+ service, ex);
		}
	}

	public static synchronized void shutdownAll()
	{
		if (executorService != null) {
			shutdown(executorService);

			executorService = null;
		}


		if (scheduledExecutorService != null) {
			shutdown(scheduledExecutorService);

			scheduledExecutorService = null;
		}
	}

}
