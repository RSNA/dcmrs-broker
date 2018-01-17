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

import java.io.OutputStream;
import java.util.List;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.SAXWriter;
import org.dcm4che3.json.JSONWriter;
import static org.dcm4che3.ws.rs.MediaTypes.APPLICATION_DICOM_XML;
import org.rsna.isn.dcmrsbroker.core.dcm.Level;
import org.rsna.isn.dcmrsbroker.core.dcm.qido.FindScu;
import org.rsna.isn.dcmrsbroker.core.io.MultipartRelatedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;


/**
 * Response for QIDO-RS query
 * 
 * @author Clifton Li
 * @version 1.0.0
 */
public class QidoResponse
{
	private static final Logger logger = LoggerFactory.getLogger(QidoResponse.class);
		
	private final Level level;
	
	/**
	 * Handles a JSON or multipart/related response
	 * @param request the request (cannot be null)
	 * @param response the response (cannot be null)
	 * @param level the level (cannot be null)
	 */	
	public QidoResponse(Request request, Response response, Level level)
	{
		this.request = request;
		this.response = response; 
		this.level = level;
	}	
	
	private final Request request;

	public Request getRequest()
	{
		return request;
	}

	private final Response response;

	public Response getResponse()
	{
		return response;
	}
	
	public Object send() throws Exception
	{
		QueryParameters params = new QueryParameters(this.request, this.level);
		
		FindScu cfind = new FindScu();
		List<Attributes> results = cfind.doQuery(params);
		
		if(results.isEmpty()) {
			response.status(204);
			return "";
		}
		
		if (this.request.headers("Accept").equals("application/json")) {
			
			this.response.header("Content-Type", "application/dicom+json");

			OutputStream out = this.response.raw().getOutputStream();
			JsonGenerator gen = Json.createGenerator(out);
			gen.writeStartArray();

			JSONWriter writer = new JSONWriter(gen);
			
			for(Attributes dcm : results) {
					writer.write(dcm);
					gen.flush();
			}
			
			gen.writeEnd();
			gen.close();

		} else {			
			response.header("Content-Type", "multipart/related; type=\"application/dicom+xml\"");
			
			MultipartRelatedOutputStream out = new MultipartRelatedOutputStream(
								response.raw().getOutputStream(),
								APPLICATION_DICOM_XML);
			
			response.type(out.getContentType());
			
			for(Attributes dcm : results) {
				MultipartRelatedOutputStream.Part part = 
								new MultipartRelatedOutputStream.Part(APPLICATION_DICOM_XML);

				SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
				TransformerHandler handler = tf.newTransformerHandler();
				handler.setResult(new StreamResult(out));
				SAXWriter writer = new SAXWriter(handler);

				out.addPart(part);
				writer.write(dcm);	
			}
			
			out.finish();
		}
		return null;	
	}
}
