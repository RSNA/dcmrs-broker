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
package org.rsna.isn.dcmrsbroker.core.dcm;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import static org.dcm4che3.net.Association.LOG;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.QueryOption;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import org.rsna.isn.dcmrsbroker.core.util.ExecutorServiceFactory;

/**
 * Base class for SCUs
 *
 * @author Wyatt Tellis
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class Scu
{
	protected Scu(String localAeTitle,
				  String remoteAeTitle,
				  String remoteHost,
				  int remotePort,
				  String sopClass)
	{
		this.localAeTitle = localAeTitle;
		this.remoteAeTitle = remoteAeTitle;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.sopClass = sopClass;
	}

	protected Scu(String localAeTitle,
				  String remoteAeTitle,
				  String remoteHost,
				  int remotePort,
				  String sopClass,
				  EnumSet<QueryOption> options)
	{
		this(localAeTitle, remoteAeTitle, remoteHost, remotePort, sopClass);

		queryOptions.addAll(options);
	}

	private final String localAeTitle;

	/**
	 * Get the local (calling) AE title
	 *
	 * @return the AE title
	 */
	public String getLocalAeTitle()
	{
		return localAeTitle;
	}

	private final String remoteAeTitle;

	/**
	 * Get the remote (called) AE title
	 *
	 * @return the AE title
	 */
	public String getRemoteAeTitle()
	{
		return remoteAeTitle;
	}

	private final String remoteHost;

	/**
	 * Get the remote host/IP address
	 *
	 * @return the remote host/IP
	 */
	public String getRemoteHost()
	{
		return remoteHost;
	}

	private final int remotePort;

	/**
	 * Get the remote port
	 *
	 * @return the port
	 */
	public int getRemotePort()
	{
		return remotePort;
	}

	private final String sopClass;

	/**
	 * Get the requested query/retrieve SOP
	 *
	 * @return the SOP class UID
	 */
	public String getSopClass()
	{
		return sopClass;
	}

	private final EnumSet<QueryOption> queryOptions = EnumSet.noneOf(QueryOption.class);

	/**
	 * Get the extended negotiation query options requested in the association
	 *
	 * @return the options
	 */
	public EnumSet<QueryOption> getQueryOptions()
	{
		return EnumSet.copyOf(queryOptions);
	}

	private Association association;

	/**
	 * Get the currently active association or null if this SCU is not connected
	 *
	 * @return the association or null
	 */
	public Association getAssociation()
	{
		return association;
	}

	protected synchronized Association connect() throws IOException,
			InterruptedException, IncompatibleConnectionException, GeneralSecurityException
	{
		if (association != null) {
			return association;
		}

		Device localDev = new Device("SCU");
		localDev.setExecutor(ExecutorServiceFactory.getService());
		localDev.setScheduledExecutor(ExecutorServiceFactory.getScheduledService());

		Connection localCon = new Connection();
		localDev.addConnection(localCon);

		ApplicationEntity localAe = new ApplicationEntity(localAeTitle);
		localAe.addConnection(localCon);
		localDev.addApplicationEntity(localAe);



		Device remoteDev = new Device("SCP");

		Connection rc = new Connection("", remoteHost, remotePort);
		remoteDev.addConnection(rc);

		ApplicationEntity remoteAe = new ApplicationEntity(remoteAeTitle);
		remoteAe.addConnection(rc);
		remoteDev.addApplicationEntity(remoteAe);

		AAssociateRQ rq = new AAssociateRQ();
		rq.addPresentationContextFor(sopClass,
									 UID.ImplicitVRLittleEndian);
		rq.addPresentationContextFor(sopClass,
									 UID.ExplicitVRLittleEndian);

		if (!queryOptions.isEmpty()) {
			rq.addExtendedNegotiation(new ExtendedNegotiation(sopClass,
															  QueryOption.toExtendedNegotiationInformation(queryOptions)));
		}

		association = localAe.connect(remoteAe, rq);

		return association;
	}

	protected synchronized void releaseGracefully()
	{
		if(association == null) {
			return;			
		}
		
		
		// Copied from v3.3.8 of the dcm4che toolkit
		if (association.isReadyForDataTransfer()) {

			try {
				association.waitForOutstandingRSP();
			}
			catch (InterruptedException ignored) {
				// if we get interrupted while trying to close the association, most likely we still want to close it
				Thread.currentThread().interrupt();
				LOG.warn("Interrupted while preparing to close the association, "
						 + "will try to release the association anyway: " + this.toString(), ignored);
			}

			try {
				association.release();
			}
			catch (IOException e) {
				LOG.warn("Failed to release association to " + association.getRemoteAET(), e);
			}
		}
		else {
			LOG.warn("Attempted to close the association, "
					 + "but it was not ready for data transfer",
					 new IOException("Association not ready for data transfer"));
		}
		
		association = null;
	}

}
