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

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.rsna.isn.dcmrsbroker.core.dcm.Level;
import static org.rsna.isn.dcmrsbroker.core.dcm.Level.*;
import spark.Request;

/**
 * Container for WADO-RS retrieve parameters
 *
 * @author Wyatt Tellis
 * @version 1.0.0
 * @since 1.0.0
 */
public class RetrieveParameters
{

	/**
	 * Create a retrieve parameters instance from the specified request
	 * @param request the request (cannot be null)
	 * @param level the level (cannot be null)
	 */
	public RetrieveParameters(Request request, Level level)
	{
		this.studyUid = request.params("studyUid");
		if (StringUtils.isBlank(studyUid)) {
			throw new IllegalArgumentException("Study UID cannot be blank");
		}

		this.seriesUid = request.params("seriesUid");
		if (SERIES.equals(level) || IMAGE.equals(level)) {
			if (StringUtils.isBlank(seriesUid)) {
				throw new IllegalArgumentException("Series UID cannot be blank");
			}
		}

		this.instanceUid = request.params("instanceUid");
		if (IMAGE.equals(level)) {
			if (StringUtils.isBlank(instanceUid)) {
				throw new IllegalArgumentException("Instance UID cannot be blank");
			}
		}

		this.level = level;
	}

	private final String studyUid;

	/**
	 * Get the requested study UID
	 *
	 * @return the study UID
	 */
	public String getStudyUid()
	{
		return studyUid;
	}

	private final String seriesUid;

	/**
	 * Get the requested series UID
	 *
	 * @return the series UID or an empty string
	 */
	public String getSeriesUid()
	{
		return seriesUid;
	}

	private final String instanceUid;

	/**
	 * Get the requested instance UID
	 *
	 * @return the instance UID or an empty string
	 */
	public String getInstanceUid()
	{
		return instanceUid;
	}

	private final LinkedHashSet<String> transferSyntaxes = new LinkedHashSet();

	/**
	 * Get the preferred transfer syntaxes for this request
	 *
	 * @return a set of transfer syntaxes sorted by preference
	 */
	public Set<String> getTransferSyntaxes()
	{
		return new LinkedHashSet(transferSyntaxes);
	}

	private final Level level;

	/**
	 * Get the C-MOVE level associated with this request
	 *
	 * @return the level
	 */
	public Level getLevel()
	{
		return level;
	}

	@Override
	public String toString()
	{
		switch(level) {
			case IMAGE:
				return studyUid + "/" + seriesUid + "/" +instanceUid;
			case SERIES:
				return studyUid + "/" + seriesUid;
			default:
				return studyUid;
		}
	}
	
	

}
