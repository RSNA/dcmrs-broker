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

import org.rsna.isn.dcmrsbroker.core.spark.qido.QueryParameters;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.QueryOption;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import static org.rsna.isn.dcmrsbroker.core.dcm.Level.*;
import org.rsna.isn.dcmrsbroker.core.dcm.Scu;
import org.rsna.isn.dcmrsbroker.core.util.Environment;
import static org.rsna.isn.dcmrsbroker.core.util.Environment.Key.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary class for handling C-Find requests
 *
 * @author Wyatt Tellis
 * @version 1.0.0
 */
public class FindScu extends Scu
{
	private static final Logger logger = LoggerFactory.getLogger(FindScu.class);

	public AttributeId REQUIRED_STUDY_ATTRIBUTE_IDS[] = {
		new AttributeId("SpecificCharacterSet"),
		new AttributeId("StudyDate"),
		new AttributeId("StudyTime"),
		new AttributeId("AccessionNumber"),
		new AttributeId("InstanceAvailability"),
		new AttributeId("ModalitiesInStudy"),
		new AttributeId("ReferringPhysicianName"),
		new AttributeId("TimezoneOffsetFromUTC"),
		new AttributeId("RetrieveURL"),
		new AttributeId("PatientName"),
		new AttributeId("PatientID"),
		new AttributeId("PatientBirthDate"),
		new AttributeId("PatientSex"),
		new AttributeId("StudyInstanceUID"),
		new AttributeId("StudyID"),
		new AttributeId("NumberOfStudyRelatedSeries"),
		new AttributeId("NumberOfStudyRelatedInstances")
	};

	public FindScu()
	{
		super(Environment.getProperty(QIDO_LOCAL_AE),
			  Environment.getProperty(QIDO_REMOTE_AE),
			  Environment.getProperty(QIDO_REMOTE_HOST),
			  Environment.getPropertyAsInt(QIDO_REMOTE_PORT),
			  UID.StudyRootQueryRetrieveInformationModelFIND,
			  EnumSet.allOf(QueryOption.class));
	}

	public List<Attributes> doQuery(QueryParameters params) throws IOException,
			InterruptedException, IncompatibleConnectionException, GeneralSecurityException
	{
		Association assoc = connect();
		try {
			ResponseHandler handler = new ResponseHandler(assoc, params);
			
			Attributes query = new Attributes();
			if (params.getLevel().equals(STUDY)) {
				ensure(query, REQUIRED_STUDY_ATTRIBUTE_IDS);
			}

			query.addAll(params.getIncludedAttributes());
			query.addAll(params.getParameters());
			query.setString(Tag.QueryRetrieveLevel, VR.CS, params.getLevel().name());

			assoc.cfind(getSopClass(),
						0,
						query,
						null,
						handler);

			return handler.results;
		}
		finally {
			releaseGracefully();
		}
	}

	private static class ResponseHandler extends DimseRSPHandler
	{
		private final List<Attributes> results = new ArrayList();

		private final int offset;

		private final int limit;

		private int index;

		private ResponseHandler(Association assoc, QueryParameters params)
		{
			super(assoc.nextMessageID());

			this.offset = params.getOffset();
			this.limit = params.getLimit();
		}

		@Override
		public void onDimseRSP(Association as, Attributes cmd,
							   Attributes data)
		{
			super.onDimseRSP(as, cmd, data);

			if (index++ >= offset) {
				if (limit == 0 || results.size() < limit) {
					int status = cmd.getInt(Tag.Status, -1);
					if(Status.isPending(status)) {
						results.add(data);
					}

				}
				else {
					// TODO: Add support for issuing a cancel
					logger.debug("Ignoring: {}", data);
				}
			}
		}

	}

	private void ensure(Attributes attr, AttributeId attrIds[])
	{
		for (AttributeId attrId : attrIds) {
			attrId.ensureExists(attr);
		}
	}

}
