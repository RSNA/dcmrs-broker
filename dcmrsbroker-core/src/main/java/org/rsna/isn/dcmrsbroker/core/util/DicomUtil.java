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
 * Created Oct 18, 2017
 */
package org.rsna.isn.dcmrsbroker.core.util;

import java.io.IOException;
import java.nio.file.Path;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

/**
 * Utilities for working with DICOM
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
public class DicomUtil
{
	private DicomUtil()
	{
	}

	/**
	 * Extract the file meta-information from a DICOM part 10 file
	 *
	 * @param path the path to the file
	 * @return the file meta-information
	 * @throws IOException if there was an error extracting the file
	 * meta-information
	 */
	public static Attributes getFileMetaInformation(Path path) throws IOException
	{
		try (DicomInputStream din = new DicomInputStream(path.toFile())) {
			return din.getFileMetaInformation();
		}
	}

	/**
	 * Extract the transfer syntax of a DICOM part 10 file
	 *
	 * @param path the path to the file
	 * @return the transfer syntax
	 * @throws IOException if there was an error extracting the transfer syntax
	 */
	public static String getTransferSyntax(Path path) throws IOException
	{
		Attributes fmi = getFileMetaInformation(path);
		
		return fmi.getString(Tag.TransferSyntaxUID);
	}

}
