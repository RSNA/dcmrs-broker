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
 * Created Oct 17, 2017
 */
package org.rsna.isn.dcmrsbroker.core.spark.wado;

import java.io.IOException;
import java.nio.file.Path;
import org.rsna.isn.dcmrsbroker.core.dcm.wado.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

/**
 * Base class for all WADO-RS response handlers
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
abstract class WadoResponse
{
	private static final Logger logger = LoggerFactory.getLogger(WadoResponse.class);

	protected WadoResponse(CacheEntry entry, Request request, Response response)
	{
		this.entry = entry;
		this.request = request;
		this.response = response;
	}

	private final CacheEntry entry;

	protected CacheEntry getEntry()
	{
		return entry;
	}

	private final Request request;

	protected Request getRequest()
	{
		return request;
	}

	private final Response response;

	protected Response getResponse()
	{
		return response;
	}

	private Boolean acceptable = null;

	/**
	 * Determine if there are any instances that can be sent to the client
	 *
	 * @return true if at least once instance is acceptable to the client, false
	 * if no instances are acceptable
	 */
	protected final boolean hasAcceptableInstance() throws IOException
	{
		checkInstances();

		return acceptable;
	}

	private Boolean unacceptable = null;

	/**
	 * Determine if there are any instances that <b>cannot</b> be sent to the
	 * client
	 *
	 * @return true if at least once instance cannot be sent, false if all
	 * instances can be sent
	 */
	protected final boolean hasUnacceptableInstance() throws IOException
	{
		checkInstances();

		return unacceptable;
	}

	/**
	 * Determine whether a specific instance can be sent to the client.
	 *
	 * @param path to the DICOM part 10 file
	 * @return true if object is acceptable, false if not
	 * @throws IOException if there was an error determine if the instance is
	 * acceptable
	 */
	protected abstract boolean isAcceptable(Path path) throws IOException;

	private void checkInstances() throws IOException
	{
		if (acceptable != null && unacceptable != null) {
			return;
		}

		for (Path p : (Iterable<Path>) entry.getFiles()::iterator) {
			if (isAcceptable(p)) {
				acceptable = true;
			}
			else {
				acceptable = false;
			}

			if (acceptable != null && unacceptable != null) {
				return;
			}
		}

		if (acceptable == null) {
			// No acceptable instances			
			acceptable = false;
		}

		if (unacceptable == null) {
			// No unacceptable instances				
			unacceptable = false;
		}
	}

	/**
	 * Send the given DICOM Part 10 file
	 *
	 * @param path of the file to send
	 */
	protected abstract void send(Path path) throws Exception;

	/**
	 * Get the content type of the response
	 *
	 * @return the content type
	 */
	protected abstract String getContentType() throws Exception;

	/**
	 * Indicates the response has finished sending.
	 */
	protected abstract void finish() throws Exception;

	/**
	 * Send the response
	 *
	 * @return
	 */
	protected Object send() throws Exception
	{
		if (hasAcceptableInstance()) {
			if (hasUnacceptableInstance()) {
				// Some instances cannot be sent
				response.status(206);
			}
			else {
				response.status(200);
			}

			response.type(getContentType());

			for (Path p : (Iterable<Path>) entry.getFiles()::iterator) {
				if (isAcceptable(p)) {
					send(p);
				}
				else {
					logger.info("Ignoring file: {}", p);
				}
			}

			finish();
		}
		else if (hasAcceptableInstance()) {
			// All instances are unacceptable			
			response.status(406);
		}
		else {
			logger.warn("No objects available to send for:" + entry);

			response.status(500);
		}

		return response.raw();
	}

}
