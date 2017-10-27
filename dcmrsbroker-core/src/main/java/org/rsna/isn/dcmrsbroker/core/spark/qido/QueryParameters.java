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
package org.rsna.isn.dcmrsbroker.core.spark.qido;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.rsna.isn.dcmrsbroker.core.dcm.qido.AttributeId;
import org.rsna.isn.dcmrsbroker.core.dcm.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

/**
 * Collection of QIDO-RS query parameters
 * 
 * @author Wyatt Tellis
 * @since 1.0.0
 */
public class QueryParameters
{
	private static final Logger logger
			= LoggerFactory.getLogger(QueryParameters.class);

	private static final ElementDictionary dict
			= ElementDictionary.getStandardElementDictionary();

	public QueryParameters(Request rqst, Level level)
	{
		this.level = level;

		String studyUid = rqst.params("studyUid");
		if (StringUtils.isNotEmpty(studyUid)) {
			params.setString(Tag.StudyInstanceUID, VR.UI, studyUid);			
		}

		String seriesUid = rqst.params("seriesUid");
		if (StringUtils.isNotEmpty(seriesUid)) {
			params.setString(Tag.SeriesInstanceUID, VR.UI, seriesUid);
		}
		
		for (String key : rqst.queryParams()) {
			if ("fuzzymatching".equals(key)) {
				fuzzy = Boolean.parseBoolean(rqst.queryParams("fuzzymatching"));
			}
			else if ("limit".equals(key)) {
				limit = NumberUtils.toInt(rqst.queryParams("limit"));
			}
			else if ("offset".equals(key)) {
				offset = NumberUtils.toInt(rqst.queryParams("offset"));
			}
			else if ("includefield".equals(key)) {
				for (String field : rqst.queryParamsValues(key)) {
					if ("all".equals(field)) {
						// TODO make this configurable
						continue;
					}

					try {
						AttributeId id = new AttributeId(field);
						id.ensureExists(included);
					}
					catch (IllegalArgumentException ex) {
						logger.warn("Invalid included attribute id: {}.", key);
					}
				}

				logger.info("Included: {}", included);
			}
			else {
				try {
					AttributeId id = new AttributeId(key);
					String value = StringUtils.join(rqst.queryParamsValues(key), '\\');
					id.setValue(params, value);
				}
				catch (IllegalArgumentException ex) {
					logger.warn("Invalid attribute id: {}.", key);
				}
			}
		}

		logger.info("Parameters: {}", params);
	}

	private final Level level;

	public Level getLevel()
	{
		return level;
	}

	private boolean fuzzy;

	public boolean isFuzzyMatchingEnabled()
	{
		return fuzzy;
	}

	private int limit;

	public int getLimit()
	{
		return limit;
	}

	private int offset;

	public int getOffset()
	{
		return offset;
	}

	private Attributes included = new Attributes(0);

	public Attributes getIncludedAttributes()
	{
		return included;
	}

	private Attributes params = new Attributes(0);

	public Attributes getParameters()
	{
		return params;
	}

	private static int attrToTag(String attr)
	{
		int tag = dict.tagForKeyword(attr);
		if (tag != -1) {
			return tag;
		}

		try {
			return Integer.parseInt(attr, 16);
		}
		catch (Exception ignore) {
			return -1;
		}
	}

	private static List<Integer> attrsToTags(String attrs)
	{
		List<Integer> tags = new ArrayList();
		for (String attr : StringUtils.split(attrs, '.')) {
			int tag = attrToTag(attr);
			if (tag == -1) {
				logger.warn("Unable parse {} to tag.", attr);

				// Skip this attribute
				return null;
			}

			tags.add(tag);
		}

		return tags;
	}

	private static void setAttr(Attributes parent, Collection<Integer> tagPath,
								Object value)
	{
		for (int tag : tagPath) {
			VR vr = dict.vrOf(tag);
			if (VR.SQ.equals(vr)) {
				Sequence sq = parent.ensureSequence(tag, 0);
				parent = new Attributes();
				sq.add(parent);
			}
			else {
				if (value == null) {
					parent.setNull(tag, vr);
				}
				else {
					parent.setValue(tag, vr, value);
				}
			}
		}
	}

}
