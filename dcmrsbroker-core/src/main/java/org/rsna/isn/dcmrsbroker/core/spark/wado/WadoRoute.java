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
package org.rsna.isn.dcmrsbroker.core.spark.wado;

import java.util.List;
import javax.ws.rs.core.MediaType;
import static org.dcm4che3.ws.rs.MediaTypes.APPLICATION_DICOM_TYPE;
import static org.dcm4che3.ws.rs.MediaTypes.getMultiPartRelatedType;
import org.rsna.isn.dcmrsbroker.core.dcm.Level;
import org.rsna.isn.dcmrsbroker.core.dcm.wado.CacheEntry;
import org.rsna.isn.dcmrsbroker.core.dcm.wado.CacheEntry.Status;
import static org.rsna.isn.dcmrsbroker.core.dcm.wado.CacheEntry.Status.*;
import org.rsna.isn.dcmrsbroker.core.dcm.wado.MoveScu;
import org.rsna.isn.dcmrsbroker.core.util.Environment;
import static org.rsna.isn.dcmrsbroker.core.util.Environment.Key.*;
import org.rsna.isn.dcmrsbroker.core.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Route that services WADO-RS requests
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class WadoRoute implements Route
{
	private static final Logger logger = LoggerFactory.getLogger(WadoRoute.class);
	
	private static String retryAfter = Environment.getProperty(WADO_HTTP_RETRY_AFTER);

	protected final Level level;

	public WadoRoute(Level level)
	{
		this.level = level;
	}

	@Override
	public final Object handle(Request request, Response response) throws Exception
	{
		RetrieveParameters params = new RetrieveParameters(request, level);

		MoveScu move = new MoveScu(params);
		CacheEntry entry = move.doRetrieve();

		Status status = entry.getStatus();
		if (status == IN_PROGRESS) {
			response.status(503);
			response.header("Retry-After", retryAfter);

			return "";
		}
		else if (status == FAILED) {
			response.status(500);
			response.type("text/plain; charset=UTF-8");

			return entry.getError();
		}
		else {
			WadoResponse wRsp = buildResponse(request, response, entry);
			if (wRsp != null) {
				return wRsp.send();
			}
			else {
				logger.warn("Unsupported accept type: " + request.headers("Accept"));
				
				response.status(406);

				return "";
			}
		}
	}

	private WadoResponse buildResponse(Request request,
									   Response response,
									   CacheEntry entry) throws Exception
	{
		List<MediaType> acceptable = HttpUtil.getAcceptableMediaTypes(request);
		MediaType primary = getMultiPartRelatedType(acceptable.get(0));

		if (APPLICATION_DICOM_TYPE.equals(primary)
			|| MediaType.WILDCARD_TYPE.equals(primary)) {

			return new DicomMultipartResponse(entry, request, response);
		}
		else {
			// return new BulkDataMultipartResponse(entry, request, response);

			return null;
		}
	}

}
