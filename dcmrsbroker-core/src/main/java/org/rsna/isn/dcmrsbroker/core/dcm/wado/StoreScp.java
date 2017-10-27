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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rsna.isn.dcmrsbroker.core.util.Environment;
import static org.rsna.isn.dcmrsbroker.core.util.Environment.Key.*;
import org.rsna.isn.dcmrsbroker.core.util.ExecutorServiceFactory;

/**
 * Implements a C-STORE (and C-ECHO) SCP
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class StoreScp
{
	private static final Logger logger = LoggerFactory.getLogger(StoreScp.class);

	private static final String transferSyntaxes[] = {
		UID.ExplicitVRLittleEndian,
		UID.ImplicitVRLittleEndian, // Required by the DICOM spec
	};

	private static Device device;

	public synchronized static void start() throws IOException,
			GeneralSecurityException
	{
		if (device == null) {
			int port = Environment.getPropertyAsInt(SCP_LOCAL_PORT);
			Connection con = new Connection();
			con.setPort(port);


			String aeTitle = Environment.getProperty(SCP_LOCAL_AE);
			ApplicationEntity ae = new ApplicationEntity(aeTitle);
			ae.addConnection(con);
			ae.setAssociationAcceptor(true);

			DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
			serviceRegistry.addDicomService(new CEchoHandler());
			serviceRegistry.addDicomService(new CStoreHandler());


			device = new Device("C-STORE SCP");
			device.setExecutor(ExecutorServiceFactory.getService());
			device.setScheduledExecutor(ExecutorServiceFactory.getScheduledService());

			device.addConnection(con);
			device.addApplicationEntity(ae);

			device.setDimseRQHandler(serviceRegistry);


			ae.addTransferCapability(
					new TransferCapability(null,
										   "*",
										   TransferCapability.Role.SCP,
										   transferSyntaxes));

			device.bindConnections();

			logger.info("Started listening on port: {}, with AE title: {}",
						port,
						aeTitle);
		}
	}

	public synchronized static void stop()
	{
		if (device != null) {
			device.unbindConnections();

			logger.info("Stopped SCP");
		}

		device = null;
	}

}
