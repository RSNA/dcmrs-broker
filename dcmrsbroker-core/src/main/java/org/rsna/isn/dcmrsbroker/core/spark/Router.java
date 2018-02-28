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
package org.rsna.isn.dcmrsbroker.core.spark;

import org.rsna.isn.dcmrsbroker.core.dcm.Level;
import org.rsna.isn.dcmrsbroker.core.dcm.wado.StoreScp;
import org.rsna.isn.dcmrsbroker.core.spark.qido.QidoSearch;
import org.rsna.isn.dcmrsbroker.core.spark.wado.WadoRoute;
import org.rsna.isn.dcmrsbroker.core.util.Environment;
import static org.rsna.isn.dcmrsbroker.core.util.Environment.Key.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.*;
import spark.servlet.SparkApplication;

/**
 * Spark router
 *
 * @author Wyatt Tellis
 * @version 1.0.0
 *
 */
public class Router implements SparkApplication
{
	private static final Logger logger = LoggerFactory.getLogger(Router.class);

	@Override
	public void init()
	{
		try {
			StoreScp.start();
		}
		catch (Exception ex) {
			logger.warn("Failed to start store SCP.", ex);

			throw new RuntimeException(ex);
		}

		String qidoBase = Environment.getProperty(QIDO_URL_BASE);
		get(qidoBase + "/studies", new QidoSearch(Level.STUDY));
		get(qidoBase + "/studies/:studyUid/series", new QidoSearch(Level.SERIES));
		get(qidoBase + "/instances", new QidoSearch(Level.IMAGE));
		get(qidoBase + "/studies/:studyUid/instances", new QidoSearch(Level.IMAGE));
		get(qidoBase + "/studies/:studyUid/series/:seriedUid/instances", new QidoSearch(Level.IMAGE));
		
		String wadoBase = Environment.getProperty(WADO_URL_BASE);
		get(wadoBase + "/studies/:studyUid", new WadoRoute(Level.STUDY));
		get(wadoBase + "/studies/:studyUid/series/:seriesUid", new WadoRoute(Level.SERIES));
		get(wadoBase + "/studies/:studyUid/series/:seriesUid/instances/:instanceUid", new WadoRoute(Level.IMAGE));
	}

	@Override
	public void destroy()
	{

	}

}
