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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Encapsulates a cache entry
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class CacheEntry implements Serializable
{

	/**
	 * Create an entry indicating the retrieve request is in progress
	 *
	 * @param remaining the number of objects remaining to be transferred
	 * @param completed the number of objects that were successfully transferred
	 * @param failed the number of objects that failed to transferred
	 * @param warning the number of object that transferred but had warnings
	 *
	 */
	CacheEntry(int remaining,
			   int completed,
			   int failed,
			   int warning)
	{
		this.status = Status.IN_PROGRESS;

		this.remaining = remaining;
		this.completed = completed;
		this.failed = failed;
		this.warning = warning;

		this.root = null;
		this.error = null;
	}

	/**
	 * Create an entry indicating the retrieve request has been completed
	 *
	 * @param root the location of the resulting files
	 * @param completed the number of objects that were successfully transferred
	 */
	CacheEntry(File root,
			   int completed,
			   int warning)
	{
		this.status = Status.COMPLETED;

		this.remaining = 0;
		this.completed = completed;
		this.failed = 0;
		this.warning = warning;

		this.root = root;
		this.error = null;
	}

	/**
	 * Create an entry indicating the retrieve request has failed
	 *
	 * @param error the error message
	 * @param completed the number of objects that were successfully transferred
	 * @param failed the number of objects that failed to transferred
	 * @param warning the number of object that transferred but had warnings
	 */
	CacheEntry(String error,
			   int completed,
			   int failed,
			   int warning)
	{
		this.status = Status.FAILED;

		this.remaining = 0;
		this.completed = completed;
		this.failed = failed;
		this.warning = warning;

		this.root = null;
		this.error = error;
	}

	private final File root;

	/**
	 * Get the DICOM files associated that were received as part of this request
	 *
	 * @return A stream containing the files or null if the retrieve is in
	 * progress or if there was an error
	 *
	 * @throws IOException if there was an error building the stream
	 */
	public Stream<Path> getFiles() throws IOException
	{
		if (root != null) {
			return Files.walk(root.toPath()).filter(new FileFilter("dcm"));
		}
		else {
			return null;
		}
	}

	private final String error;

	/**
	 * Get the error associated with a failed request
	 *
	 * @return the error message (null if this is not a failed request)
	 */
	public String getError()
	{
		return error;
	}

	private final Status status;

	/**
	 * Get the status of the retrieve request
	 *
	 * @return the status
	 */
	public Status getStatus()
	{
		return status;
	}

	private final int remaining;

	/**
	 * Get the number of objects remaining to be transferred.  <i>Note: maybe
	 * -1 if this information is not provided by the C-MOVE SCP</i>
	 *
	 * @return the number of objects
	 */
	public int getRemaining()
	{
		return remaining;
	}

	private final int completed;

	/**
	 * Get the number of objects that have been successfully transferred.
	 * <i>Note: maybe -1 if this information is not provided by the C-MOVE
	 * SCP</i>
	 *
	 * @return the number of objects
	 */
	public int getCompleted()
	{
		return completed;
	}

	private final int failed;

	/**
	 * Get the number of objects that failed to transferred <i>Note: maybe -1
	 * if this information is not provided by the C-MOVE SCP</i>
	 *
	 * @return the number of objects
	 */
	public int getFailed()
	{
		return failed;
	}

	private final int warning;

	/**
	 * Get the number of object that transferred but had warnings <i>Note: maybe
	 * -1 if this information is not provided by the C-MOVE SCP</i>
	 *
	 * @return the number of objects
	 */
	public int getWarning()
	{
		return warning;
	}

	public static enum Status
	{
		IN_PROGRESS,
		COMPLETED,
		FAILED

	}
}
