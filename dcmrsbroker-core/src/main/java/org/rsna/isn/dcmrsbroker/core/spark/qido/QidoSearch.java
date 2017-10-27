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
 * Created Oct 25, 2017
 */
package org.rsna.isn.dcmrsbroker.core.spark.qido;

import org.rsna.isn.dcmrsbroker.core.dcm.Level;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Implements QIDO-RS query
 * 
 * @author Clifton Li
 * @version 1.0.0
 */

public class QidoSearch implements Route
{
	private final Level level;

	public QidoSearch(Level level)
	{
		this.level = level;
	}
		
	@Override
	public Object handle(Request request, Response response) throws Exception
	{
		QidoResponse qRsp = new QidoResponse(request, response, this.level);
		
		return qRsp.send();
	}	
}
