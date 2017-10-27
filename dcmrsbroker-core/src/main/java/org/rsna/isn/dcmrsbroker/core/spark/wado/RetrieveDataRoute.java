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
package org.rsna.isn.dcmrsbroker.core.spark.wado;

import java.util.List;
import javax.ws.rs.core.MediaType;
import static org.dcm4che3.ws.rs.MediaTypes.APPLICATION_DICOM_TYPE;
import static org.dcm4che3.ws.rs.MediaTypes.getMultiPartRelatedType;
import org.rsna.isn.dcmrsbroker.core.dcm.Level;
import org.rsna.isn.dcmrsbroker.core.dcm.wado.CacheEntry;
import org.rsna.isn.dcmrsbroker.core.util.HttpUtil;
import spark.Request;
import spark.Response;

/**
 * Base class for routes that retrieve data (e.g. RetrieveStudies,
 * RetrieveSeries, etc)
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
abstract class RetrieveDataRoute extends WadoRoute
{
	RetrieveDataRoute(Level level)
	{
		super(level);
	}

	@Override
	protected WadoResponse buildResponse(Request request,
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
