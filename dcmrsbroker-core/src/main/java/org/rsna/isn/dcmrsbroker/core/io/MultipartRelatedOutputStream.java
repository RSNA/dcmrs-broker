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
package org.rsna.isn.dcmrsbroker.core.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;

/**
 * Output stream for writing multipart/related content
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class MultipartRelatedOutputStream extends FilterOutputStream
{
	private final String boundary = UUID.randomUUID().toString();

	private final String type;

	private Part currentPart;

	public MultipartRelatedOutputStream(OutputStream out, String type)
	{
		super(out);

		this.type = type;
	}

	public String getContentType()
	{
		return "multipart/related; "
			   + "type=" + type + "; "
			   + "boundary=\"" + boundary + "\"";
	}

	public void addPart(Part part) throws IOException
	{
		if (currentPart != null) {
			IOUtils.write("\r\n", out, "UTF-8");
		}

		IOUtils.write("--" + boundary, out, "UTF-8");
		IOUtils.write("\r\n", out, "UTF-8");
		for (String name : part.headers.keySet()) {
			String value = part.headers.get(name);

			IOUtils.write(name + ": " + value, out, "UTF-8");
			IOUtils.write("\r\n", out, "UTF-8");
		}

		IOUtils.write("\r\n", out, "UTF-8");

		currentPart = part;
	}

	@Override
	public void write(int b) throws IOException
	{
		if (currentPart == null) {
			throw new IOException("No part currently defined");
		}

		super.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		if (currentPart == null) {
			throw new IOException("No part currently defined");
		}

		super.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (currentPart == null) {
			throw new IOException("No part currently defined");
		}

		super.write(b, off, len);
	}

	@Override
	public void close() throws IOException
	{
		if (currentPart != null) {
			finish();
		}

		super.close();
	}

	/**
	 * Write the final boundary, indicating the multipart content is finished
	 *
	 * @throws IOException
	 */
	public void finish() throws IOException
	{
		IOUtils.write("\r\n", out, "UTF-8");
		IOUtils.write("--" + boundary + "--", out, "UTF-8");

		out.flush();

		currentPart = null;
	}

	public static class Part
	{
		private final Map<String, String> headers = new LinkedHashMap();

		public Part()
		{
		}

		public Part(String contentType)
		{
			headers.put("Content-Type", contentType);
		}

		public void addHeader(String name, String value)
		{
			headers.put(name, value);
		}

	}
}
