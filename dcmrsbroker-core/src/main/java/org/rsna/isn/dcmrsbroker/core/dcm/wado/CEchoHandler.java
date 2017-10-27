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
package org.rsna.isn.dcmrsbroker.core.dcm.wado;

import java.io.IOException;
import java.net.Socket;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles and logs C-ECHO requests
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
class CEchoHandler extends BasicCEchoSCP
{
	private static final Logger logger = LoggerFactory.getLogger(CEchoHandler.class);

	@Override
	public void onDimseRQ(Association as,
						  PresentationContext pc,
						  Dimse dimse,
						  Attributes cmd,
						  Attributes data) throws IOException
	{
		super.onDimseRQ(as, pc, dimse, cmd, data);

		Socket s = as.getSocket();
		
		logger.info("Handled C-ECHO request from: {}@{}:{}",
					as.getCallingAET(),
					s.getInetAddress().getHostAddress(),
					s.getPort());
	}

}
