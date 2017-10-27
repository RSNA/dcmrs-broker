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
package org.rsna.isn.dcmrsbroker.core.dcm.qido;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.VR;

/**
 * Utility class for working with QIDO-RS attribute ids
 *
 * @author Wyatt Tellis
 * @version 1.0.0
 */
public class AttributeId
{
	private static final ElementDictionary dict
			= ElementDictionary.getStandardElementDictionary();

	private final List<Integer> tagPath;

	/**
	 * Create a new attribute ID instance for the given string.
	 *
	 * @param attrId the id with the appropriate encoding (see: 6.7.1.1.1
	 * {attributeID} encoding rules)
	 * @throws IllegalArgumentException if the encoding is invalid)
	 */
	public AttributeId(String attrId) throws IllegalArgumentException
	{
		tagPath = parse(attrId);
	}

	private static List<Integer> parse(String attrs)
	{
		List<Integer> tagPath = new ArrayList();
		for (String attr : StringUtils.split(attrs, '.')) {
			int tag = dict.tagForKeyword(attr);
			if (tag == -1) {
				try {
					tag = Integer.parseInt(attr, 16);
				}
				catch (Exception ignore) {
					throw new IllegalArgumentException("Unable parse: " + attr);
				}
			}

			tagPath.add(tag);
		}

		return tagPath;
	}

	/**
	 * Ensures the attribute specified by this id is present in the provided
	 * attributes. If not present, the attribute is added and its value is set
	 * to null
	 *
	 * @param attr the attributes to check
	 *
	 */
	public void ensureExists(Attributes attr)
	{
		Attributes toUpdate = attr;
		Sequence sq = null;
		for (int tag : tagPath) {
			if (toUpdate == null) {
				for (Attributes item : sq) {
					if (item.contains(tag)) {
						toUpdate = item;

						break;
					}
				}

				if (toUpdate == null) {
					if (sq.size() == 1) {
						toUpdate = sq.get(0);
					}
					else {
						toUpdate = new Attributes();
						sq.add(toUpdate);
					}
				}
			}


			VR vr = dict.vrOf(tag);
			if (VR.SQ.equals(vr)) {
				sq = toUpdate.ensureSequence(tag, 0);
				toUpdate = null;
			}
			else {
				if (!toUpdate.contains(tag)) {
					toUpdate.setNull(tag, vr);
				}

				return;
			}
		}
	}

	/**
	 * Add or update the value of the attribute specified by this attribute id
	 *
	 * @param attr attributes to be updated
	 * @param value new value (can be null)
	 */
	public void setValue(Attributes attr, Object value)
	{
		Attributes toUpdate = attr;
		Sequence sq = null;
		for (int tag : tagPath) {
			if (toUpdate == null) {
				for (Attributes item : sq) {
					if (item.contains(tag)) {
						toUpdate = item;

						break;
					}
				}

				if (toUpdate == null) {
					if (sq.size() == 1) {
						toUpdate = sq.get(0);
					}
					else {
						toUpdate = new Attributes();
						sq.add(toUpdate);
					}
				}
			}


			VR vr = dict.vrOf(tag);
			if (VR.SQ.equals(vr)) {
				sq = toUpdate.ensureSequence(tag, 0);
				toUpdate = null;
			}
			else {
				if (value == null) {
					toUpdate.setNull(tag, vr);
				}
				else {
					toUpdate.setValue(tag, vr, value);
				}

				return;
			}
		}
	}

	public static void main(String argv[])
	{
		AttributeId id = new AttributeId("OtherPatientIDsSequence.IssuerOfPatientIDQualifiersSequence.UniversalEntityID");

		Attributes test = new Attributes();
		id.setValue(test, "1234");

		id = new AttributeId("OtherPatientIDsSequence.IssuerOfPatientIDQualifiersSequence.IdentifierTypeCode");
		id.setValue(test, "5678");

		id = new AttributeId("OtherPatientIDsSequence.IssuerOfPatientIDQualifiersSequence.AssigningFacilitySequence.UniversalEntityID");
		id.setValue(test, "abcd");

		id = new AttributeId("OtherPatientIDsSequence.IssuerOfPatientIDQualifiersSequence.AssigningFacilitySequence.LocalNamespaceEntityID");
		id.ensureExists(test);

		System.out.println(test);
	}

}
