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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomOutputStream;
import org.rsna.isn.dcmrsbroker.core.spark.wado.RetrieveParameters;
import org.rsna.isn.dcmrsbroker.core.util.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.rsna.isn.dcmrsbroker.core.util.Environment.Key.SCP_CACHE_DIR_PATH;
import static org.rsna.isn.dcmrsbroker.core.dcm.wado.CacheEntry.Status.*;

/**
 * Interface to the local DICOM cache
 *
 * @author Wyatt Tellis
 *
 * @since 1.0.0
 * @version 1.0.0
 */
class CacheManager
{
	private static final Logger logger
			= LoggerFactory.getLogger(CacheManager.class);

	private static final File cacheDir;

	static {
		String path = Environment.getProperty(SCP_CACHE_DIR_PATH);
		cacheDir = new File(path);

		if (!cacheDir.isDirectory()) {
			logger.warn("{} is not a directory.", cacheDir);
			
			throw new ExceptionInInitializerError(cacheDir + " is not a directory.");
		}
		
		try {
			FileUtils.cleanDirectory(cacheDir);
		}
		catch (IOException ex) {
			logger.warn("Unable to purge cache directory: " + cacheDir, ex);
			
			throw new ExceptionInInitializerError(ex);
		}
		
		CacheReaper reaper = new CacheReaper();
		reaper.start();
	}

	private CacheManager()
	{
	}

	private static CacheEntry getEntry(String studyUid,
									   String seriesUid,
									   String instanceUid)
			throws Exception
	{
		File studyDir = new File(cacheDir, studyUid);


		File infoFile;
		if (StringUtils.isNotBlank(seriesUid)) {
			File seriesDir = new File(studyDir, seriesUid);

			if (StringUtils.isNotBlank(instanceUid)) {
				infoFile = new File(seriesDir, instanceUid + ".info");
				if (!infoFile.isFile()) {
					// Try looking for the info file at a higher level
					CacheEntry entry = getEntry(studyUid, seriesUid, null);
					if (entry != null && COMPLETED.equals(entry.getStatus())) {
						File dcmFile = buildFile(studyUid,
												 seriesUid,
												 instanceUid,
												 "dcm");

						return new CacheEntry(dcmFile,
											  entry.getCompleted(),
											  entry.getWarning());
					}
					else {
						return entry;
					}
				}
			}
			else {
				infoFile = new File(seriesDir, "series.info");
				if (!infoFile.isFile()) {
					// Try looking for the info file at a higher level
					CacheEntry entry = getEntry(studyUid, null, null);
					if (entry != null && COMPLETED.equals(entry.getStatus())) {
						return new CacheEntry(seriesDir,
											  entry.getCompleted(),
											  entry.getWarning());
					}
					else {
						return entry;
					}
				}
			}
		}
		else {
			infoFile = new File(studyDir, "study.info");
		}

		if (infoFile.isFile()) {
			try (FileInputStream fin = new FileInputStream(infoFile)) {
				ObjectInputStream oin = new ObjectInputStream(fin);

				return (CacheEntry) oin.readObject();
			}
		}
		else {
			return null;
		}
	}

	private static File buildFile(RetrieveParameters params, String suffix)
	{
		return buildFile(params.getStudyUid(),
						 params.getSeriesUid(),
						 params.getInstanceUid(),
						 suffix);
	}

	private static File buildFile(String studyUid,
								  String seriesUid,
								  String instanceUid,
								  String suffix)
	{
		File studyDir = new File(cacheDir, studyUid);
		if (StringUtils.isNotBlank(seriesUid)) {
			File seriesDir = new File(studyDir, seriesUid);

			if (StringUtils.isNotBlank(instanceUid)) {
				return new File(seriesDir, instanceUid + "." + suffix);
			}
			else {
				return seriesDir;
			}
		}
		else {
			return studyDir;
		}
	}

	private static void updateEntry(CacheEntry entry,
									RetrieveParameters params)
			throws IOException
	{
		String studyUid = params.getStudyUid();
		String seriesUid = params.getSeriesUid();
		String instanceUid = params.getInstanceUid();

		File studyDir = new File(cacheDir, studyUid);

		File infoFile;
		if (StringUtils.isNotBlank(seriesUid)) {
			File seriesDir = new File(studyDir, seriesUid);
			if (StringUtils.isNotBlank(instanceUid)) {
				infoFile = new File(seriesDir, instanceUid + ".info");
			}
			else {
				infoFile = new File(seriesDir, "series.info");
			}
		}
		else {
			infoFile = new File(studyDir, "study.info");
		}

		FileUtils.touch(infoFile); // Create parent dirs if necessary
		try (FileOutputStream fos = new FileOutputStream(infoFile)) {
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(entry);
		}
	}

	static CacheEntry getEntry(RetrieveParameters params)
			throws Exception
	{
		return getEntry(params.getStudyUid(),
						params.getSeriesUid(),
						params.getInstanceUid());
	}

	static CacheEntry setInProgress(RetrieveParameters params,
									int remaining,
									int completed,
									int failed,
									int warning) throws IOException
	{
		CacheEntry entry = new CacheEntry(remaining,
										  completed,
										  failed,
										  warning);

		updateEntry(entry, params);

		return entry;
	}

	static CacheEntry setFailed(RetrieveParameters params,
								String msg,
								int completed,
								int failed,
								int warning) throws IOException
	{
		CacheEntry entry = new CacheEntry(msg,
										  completed,
										  failed,
										  warning);

		updateEntry(entry, params);

		return entry;
	}

	static CacheEntry setCompleted(RetrieveParameters params,
								   int completed,
								   int warning) throws IOException
	{
		CacheEntry entry = new CacheEntry(buildFile(params, "dcm"),
										  completed,
										  warning);

		updateEntry(entry, params);

		return entry;
	}

	static long getFileCount(RetrieveParameters params) throws IOException
	{
		File root = buildFile(params, "dcm");
		if (!root.exists()) {
			return 0;
		}

		return Files.walk(root.toPath())
				.filter(new FileFilter("dcm"))
				.count();
	}

	static void writeObject(Attributes obj, String txUid, String classUid)
			throws IOException
	{
		String studyUid = obj.getString(Tag.StudyInstanceUID);
		String seriesUid = obj.getString(Tag.SeriesInstanceUID);
		String instanceUid = obj.getString(Tag.SOPInstanceUID);

		File dcmFile = buildFile(studyUid, seriesUid, instanceUid, "dcm");
		File tmpFile = buildFile(studyUid, seriesUid, instanceUid, "tmp");
		File errFile = buildFile(studyUid, seriesUid, instanceUid, "err");
		try {
			if (errFile.isFile()) {
				logger.info("Overwriting error file: {}", errFile);

				FileUtils.deleteQuietly(errFile);
			}

			FileUtils.touch(tmpFile); // Create parent directories if needed

			try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
				if (UID.ImplicitVRLittleEndian.equals(txUid)) {
					// DicomOutputStream throws exception when writing dataset with LEI
					txUid = UID.ExplicitVRLittleEndian;
				}
				else if (UID.ExplicitVRBigEndianRetired.equals(txUid)) {
					// Should never happen, but just in case
					txUid = UID.ExplicitVRLittleEndian;

					logger.info("Trancoding dataset from big to "
								+ "little endian for: {}", dcmFile);
				}


				Attributes fmi = Attributes.createFileMetaInformation(instanceUid,
																	  classUid,
																	  txUid);

				DicomOutputStream dos = new DicomOutputStream(fos, txUid);
				dos.writeDataset(fmi, obj);
				dos.close();
			}

			Files.move(tmpFile.toPath(),
					   dcmFile.toPath(),
					   StandardCopyOption.ATOMIC_MOVE,
					   StandardCopyOption.REPLACE_EXISTING);
		}
		catch (Exception ex) {
			logger.warn("Unable save DICOM object to: " + dcmFile, ex);

			FileUtils.touch(errFile);
			FileUtils.deleteQuietly(tmpFile);
			FileUtils.deleteQuietly(dcmFile);

			if (ex instanceof IOException) {
				throw (IOException) ex;
			}
			else {
				throw new IOException(ex);
			}
		}
	}

}
