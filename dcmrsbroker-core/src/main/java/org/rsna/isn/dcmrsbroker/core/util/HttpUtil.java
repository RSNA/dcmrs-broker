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
package org.rsna.isn.dcmrsbroker.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import org.dcm4che3.util.StringUtils;
import org.jboss.resteasy.util.MediaTypeHelper;
import spark.Request;

/**
 * Utilities for working with HTTP
 * 
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class HttpUtil
{
	private HttpUtil()
	{
	}

	public static List<MediaType> getAcceptableMediaTypes(Request request)
	{
		// See: http://docs.oracle.com/javaee/7/api/javax/ws/rs/core/HttpHeaders.html#getAcceptableMediaTypes--
		HttpServletRequest rawReq = request.raw();
		List<String> values = Collections.list(rawReq.getHeaders("Accept"));
		if (values.isEmpty()) {
			return Collections.singletonList(MediaType.WILDCARD_TYPE);
		}
		ArrayList<MediaType> acceptable = new ArrayList();
		for (String value : values) {
			for (String type : StringUtils.split(value, ',')) {
				acceptable.add(MediaType.valueOf(type.trim()));
			}
		}
		MediaTypeHelper.sortByWeight(acceptable);
		return Collections.unmodifiableList(acceptable);
	}

}
