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

import org.rsna.isn.dcmrsbroker.core.dcm.Level;

/**
 * Implements the WADO-RS RetrieveSeries transaction
 *
 * @see <a href=
 *      "http://medical.nema.org/medical/dicom/current/output/html/part18.html#sect_6.5">
 * DICOM PS3.18 (Web Services) WADO-RS</a>
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class RetrieveSeries extends RetrieveDataRoute
{
	public RetrieveSeries()
	{
		super(Level.SERIES);
	}
}
