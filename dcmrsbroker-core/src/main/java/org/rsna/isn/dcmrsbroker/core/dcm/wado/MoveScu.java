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
package org.rsna.isn.dcmrsbroker.core.dcm.wado;

import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.Status;
import org.rsna.isn.dcmrsbroker.core.dcm.Scu;
import org.rsna.isn.dcmrsbroker.core.spark.wado.RetrieveParameters;
import org.rsna.isn.dcmrsbroker.core.util.Environment;
import static org.rsna.isn.dcmrsbroker.core.util.Environment.Key.*;
import org.rsna.isn.dcmrsbroker.core.util.ExecutorServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C-MOVE SCU
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class MoveScu extends Scu implements Runnable
{
	private static final Logger logger = LoggerFactory.getLogger(MoveScu.class);

	private static final String destinationAeTitle = Environment.getProperty(SCP_LOCAL_AE);

	private static final long retryDelay = Environment.getPropertyAsInt(WADO_RETRY_DELAY_IN_SECS) * DateUtils.MILLIS_PER_SECOND;

	private static final int retryAttempts = Environment.getPropertyAsInt(WADO_MAX_RETRY_ATTEMPTS);

	private static final long retrieveTimeout = Environment.getPropertyAsInt(WADO_RETRIEVE_TIMEOUT_IN_SECS) * DateUtils.MILLIS_PER_SECOND;

	private static final boolean ignoreMissing = Environment.getPropertyAsBoolean(WADO_IGNORE_MISSING_OBJECTS);

	private final RetrieveParameters params;

	public MoveScu(RetrieveParameters params)
	{
		super(Environment.getProperty(WADO_LOCAL_AE),
			  Environment.getProperty(WADO_REMOTE_AE),
			  Environment.getProperty(WADO_REMOTE_HOST),
			  Environment.getPropertyAsInt(WADO_REMOTE_PORT),
			  UID.StudyRootQueryRetrieveInformationModelMOVE);

		this.params = params;
	}

	public synchronized CacheEntry doRetrieve()
			throws Exception
	{
		CacheEntry entry = CacheManager.getEntry(params);

		if (entry != null) {
			return entry;
		}
		else {
			entry = CacheManager.setInProgress(params, -1, -1, -1, -1);

			ExecutorService service = ExecutorServiceFactory.getService();
			service.execute(this);

			return entry;
		}
	}

	@Override
	public void run()
	{
		try {
			CMoveHandler handler = null;
			Throwable lastError = null;

			for (int i = 0; i < retryAttempts; i++) {
				if (i > 0) {
					if (handler != null) {
						long expectedCount = handler.completed + handler.warning;
						long actualCount = CacheManager.getFileCount(params);

						if (expectedCount > 0 && actualCount >= expectedCount) {
							CacheManager.setCompleted(params,
													  handler.completed,
													  handler.warning);


							logger.info("Completed C-MOVE for request: {} after {} retries. Completed: {}. Warning: {}.",
										params,
										i,
										handler.completed,
										handler.warning);

							return;
						}
					}


					logger.warn("Retrying C-MOVE for request: {}.  This is retry #{}.",
								params, i);
				}

				try {
					handler = null;
					lastError = null;

					logger.info("Start C-MOVE for request: " + params);

					CacheManager.setInProgress(params, -1, -1, -1, -1);


					String level = params.getLevel().name();
					String studyUid = params.getStudyUid();
					String seriesUid = params.getSeriesUid();
					String instanceUid = params.getInstanceUid();


					Attributes keys = new Attributes();
					keys.setString(Tag.QueryRetrieveLevel, VR.CS, level);
					keys.setString(Tag.StudyInstanceUID, VR.UI, studyUid);
					if (StringUtils.isNotBlank(seriesUid)) {
						keys.setString(Tag.SeriesInstanceUID, VR.UI, seriesUid);
					}
					if (StringUtils.isNotBlank(instanceUid)) {
						keys.setString(Tag.SOPInstanceUID, VR.UI, instanceUid);
					}


					keys.setString(Tag.SeriesInstanceUID, VR.UI, seriesUid);


					Association assoc = connect();

					// http://dicom.nema.org/medical/Dicom/2016b/output/chtml/part04/sect_C.4.2.html
					handler = new CMoveHandler(assoc.nextMessageID());
					assoc.cmove(UID.StudyRootQueryRetrieveInformationModelMOVE,
								0,
								keys,
								null,
								destinationAeTitle,
								handler);


					if (assoc.isReadyForDataTransfer()) {
						assoc.waitForOutstandingRSP();
					}

					if (handler.status == Status.Success) {
						long expectedCount = handler.completed + handler.warning; // Might be zero

						// Some PACS system return a success status even though
						// they haven't even started sending.  So we include some logic
						// to wait for new images to arrive

						boolean completed = false;
						long lastCount = -1;
						StopWatch timer = new StopWatch();
						while (true) {
							long actualCount = CacheManager.getFileCount(params);
							long lapsed = timer.getTime();

							if (expectedCount > 0 && actualCount >= expectedCount) {
								completed = true;
							}
							else if (expectedCount > 0 && ignoreMissing && lapsed >= retrieveTimeout) {
								logger.warn("C-MOVE for request {} completed with "
											+ "missing objects.  Expected: {}, "
											+ "but only received: {}.",
											params,
											expectedCount,
											actualCount);

								completed = true;
							}
							else if (expectedCount < 1 && actualCount > 0 && lapsed >= retrieveTimeout) {
								completed = true;
							}
							else if (lapsed >= retrieveTimeout) {
								lastError = new RuntimeException("C-MOVE for request "
																 + params + " timed out");

								logger.warn("C-MOVE for request {} timed out.  Received {} objects.  "
											+ "Expected {} objects", params, actualCount, expectedCount);

								break;
							}


							if (completed) {
								CacheManager.setCompleted(params,
														  handler.completed,
														  handler.warning);


								logger.info("Completed C-MOVE for request: {}. Completed: {}. Warning: {}.",
											params,
											handler.completed,
											handler.warning);

								return;
							}

							if (lastCount != actualCount) {
								timer.reset();
								timer.start();
							}

							lastCount = actualCount;
						}
					}
					else {
						logger.info("C-MOVE for request: {} failed. Error: {}. Message: {}. "
									+ "Completed: {}. Warning: {}. Failed: {}",
									params,
									handler.status,
									handler.error,
									handler.completed,
									handler.warning,
									handler.failed);
					}
				}
				catch (Throwable ex) {
					logger.warn("Uncaught exception while processing "
								+ "C-MOVE request for: " + params, ex);

					lastError = ex;
				}
				finally {
					releaseGracefully();
				}

				if (i < (retryAttempts - 1)) {
					logger.warn("Retrying C-MOVE request for {} in {} sec(s)", 
								params, 
								DurationFormatUtils.formatDuration(retryDelay, "s"));
					
					Thread.sleep(retryDelay);
				}
			}


			String msg = "Timeout";
			if (lastError != null) {
				msg = StringUtils.defaultIfBlank(lastError.getMessage(),
												 ExceptionUtils.getStackTrace(lastError));
			}
			else if (handler != null) {
				msg = "DICOM Error: " + handler.status
					  + ". DICOM Error Comment: " + handler.error;
			}

			if (handler != null) {
				CacheManager.setFailed(params,
									   msg,
									   handler.completed,
									   handler.failed,
									   handler.remaining);
			}
			else {
				CacheManager.setFailed(params, msg, -1, -1, -1);
			}

			logger.warn("C-MOVE request for: {} failed with error: {}",
						params,
						msg);
		}
		catch (Exception ex) {
			logger.warn("Uncaught exception while processing "
						+ "C-MOVE request for: " + params, ex);
		}
	}

	private class CMoveHandler extends DimseRSPHandler
	{
		private int remaining = -1;

		private int completed = -1;

		private int failed = -1;

		private int warning = -1;

		private int status = -1;

		private String error = "";

		private CMoveHandler(int msgId)
		{
			super(msgId);
		}

		@Override
		public void onDimseRSP(Association as, Attributes cmd, Attributes data)
		{
			super.onDimseRSP(as, cmd, data);

			status = cmd.getInt(Tag.Status, -1);
			if (Status.isPending(status)) {
				completed = cmd.getInt(Tag.NumberOfCompletedSuboperations, -1);
				remaining = cmd.getInt(Tag.NumberOfRemainingSuboperations, -1);
				warning = cmd.getInt(Tag.NumberOfWarningSuboperations, -1);
				failed = cmd.getInt(Tag.NumberOfFailedSuboperations, -1);
			}
			else {
				completed = cmd.getInt(Tag.NumberOfCompletedSuboperations, -1);
				remaining = 0;
				warning = cmd.getInt(Tag.NumberOfWarningSuboperations, -1);
				failed = cmd.getInt(Tag.NumberOfFailedSuboperations, -1);

				error = cmd.getString(Tag.ErrorComment, "");
			}
		}

	}
}
