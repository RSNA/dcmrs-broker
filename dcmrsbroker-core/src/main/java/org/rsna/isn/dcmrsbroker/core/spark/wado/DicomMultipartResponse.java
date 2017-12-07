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
 * Created Oct 17, 2017
 */
package org.rsna.isn.dcmrsbroker.core.spark.wado;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import static org.dcm4che3.data.UID.*;
import org.dcm4che3.imageio.codec.Transcoder;
import org.dcm4che3.io.DicomInputStream;
import static org.dcm4che3.ws.rs.MediaTypes.*;
import org.rsna.isn.dcmrsbroker.core.dcm.wado.CacheEntry;
import org.rsna.isn.dcmrsbroker.core.io.MultipartRelatedOutputStream;
import org.rsna.isn.dcmrsbroker.core.io.MultipartRelatedOutputStream.Part;
import org.rsna.isn.dcmrsbroker.core.util.DicomUtil;
import org.rsna.isn.dcmrsbroker.core.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

/**
 * Handles a DICOM multipart/related response
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
class DicomMultipartResponse extends WadoResponse
{
	private static final Logger logger
			= LoggerFactory.getLogger(DicomMultipartResponse.class);
	
	private final Set<String> acceptableTransferSyntaxes = new LinkedHashSet();
	
	private final MultipartRelatedOutputStream out;
	
	DicomMultipartResponse(CacheEntry entry,
						   Request request,
						   Response response) throws IOException
	{
		super(entry, request, response);
		
		for (MediaType type : HttpUtil.getAcceptableMediaTypes(request)) {
			if (type.isWildcardType()) {
				acceptableTransferSyntaxes.add(ExplicitVRLittleEndian);
				
				continue;
			}
			
			MediaType relatedType = getMultiPartRelatedType(type);
			if (APPLICATION_DICOM_TYPE.isCompatible(relatedType)) {
				String tx = getTransferSyntax(relatedType);
				if (StringUtils.isNotBlank(tx)) {
					if (ImplicitVRLittleEndian.equals(tx)) {
						logger.warn("LEI is not permitted by WADO-RS. Ignoring "
									+ " type: {}", type);
						
						continue;
					}
					else if (ExplicitVRBigEndianRetired.equals(tx)) {
						logger.warn("BEE is not permitted by WADO-RS. Ignoring "
									+ " type: {}", type);
						
						continue;
					}
					
					acceptableTransferSyntaxes.add(tx);
				}
				else {
					acceptableTransferSyntaxes.add(ExplicitVRLittleEndian);
				}
			}
		}
		
		if (acceptableTransferSyntaxes.isEmpty()) {
			acceptableTransferSyntaxes.add(ExplicitVRLittleEndian);
		}
		
		this.out = new MultipartRelatedOutputStream(response.raw().getOutputStream(),
													APPLICATION_DICOM);
	}
	
	@Override
	protected boolean isAcceptable(Path path) throws IOException
	{
		if (acceptableTransferSyntaxes.contains("*")) {
			// Client accepts everything
			return true;
		}
		
		String tx = DicomUtil.getTransferSyntax(path);
		if (acceptableTransferSyntaxes.contains(tx)) {
			// We can send the file as is
			return true;
		}
		else if (acceptableTransferSyntaxes.contains(ExplicitVRLittleEndian)) {
			// We'll need to transcode it to LEE

			return true;
		}
		else {
			// The client is asking for an compressed transfer syntax which we don't support
			return false;
		}
	}
	
	@Override
	protected String getContentType()
	{
		return out.getContentType();
	}
	
	@Override
	protected void send(Path path) throws Exception
	{
		Part part = new Part(APPLICATION_DICOM);
		out.addPart(part);
		
		if (acceptableTransferSyntaxes.contains("*")) {
			// Client accepts any transfer syntax so just send the file
			FileUtils.copyFile(path.toFile(), out);
		}
		else {
			String tx = DicomUtil.getTransferSyntax(path);
			if (acceptableTransferSyntaxes.contains(tx)) {
				// Client accepts the transfer syntax of the file, so just send it
				FileUtils.copyFile(path.toFile(), out);
			}
			else {
				// Client does not accept the transfer syntax of the file, so we transcode it to LEE
				try (Transcoder transcoder = new Transcoder(path.toFile())) {
					transcoder.setDestinationTransferSyntax(ExplicitVRLittleEndian);
					transcoder.setIncludeFileMetaInformation(true);
					transcoder.setCloseInputStream(true);
					transcoder.setCloseOutputStream(false);
					transcoder.setIncludeBulkData(DicomInputStream.IncludeBulkData.YES);
					
					transcoder.transcode((Transcoder t, Attributes dataset) -> out);
				}
			}
		}
	}
	
	@Override
	protected void finish() throws Exception
	{
		out.finish();
	}
	
}
