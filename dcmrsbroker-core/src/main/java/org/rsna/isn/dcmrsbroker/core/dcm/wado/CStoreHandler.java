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
 * Created Oct 5, 2017
 */
package org.rsna.isn.dcmrsbroker.core.dcm.wado;

import java.io.IOException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles C-STORE requests
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class CStoreHandler extends BasicCStoreSCP
{
	private static final Logger logger = LoggerFactory.getLogger(CStoreHandler.class);

	@Override
	protected void store(Association as, 
						 PresentationContext pc, 
						 Attributes rq,
						 PDVInputStream pin, 
						 Attributes rsp) throws IOException
	{
		String tsuid = pc.getTransferSyntax();
		String classUid = rq.getString(Tag.AffectedSOPClassUID);
		
		DicomInputStream din = new DicomInputStream(pin, tsuid);
		Attributes obj = din.readDataset(-1, -1);

		CacheManager.writeObject(obj, tsuid, classUid);
	}

}
