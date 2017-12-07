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
package org.rsna.isn.dcmrsbroker.core.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains methods for accessing information about the installation
 * environment.
 *
 * @author Clifton Li
 * @version 1.0.0
 */
public class Environment
{
	private static final String homePath;

	private static final File propFile;

	private static final Properties props;

	private static final Logger logger
			= LoggerFactory.getLogger(Environment.class);

	static {
		homePath = System.getProperty("dcmrsbroker.home");
		if (StringUtils.isNotBlank(homePath)) {

			File homeDir = new File(homePath);
			if (!homeDir.isDirectory()) {
				throw new ExceptionInInitializerError(homePath + " is not a directory.");
			}
			else {
				logger.info("dcmrsbroker.home set to " + homePath);
			}
			
			File confDir = new File(homePath, "conf");
			propFile = new File(confDir, "dcmrsbroker.properties");
			if (!propFile.exists()) {
				throw new ExceptionInInitializerError("Could not find "
													  + propFile
													  + " file.");
			}
			else {
				logger.info("Found " + propFile + " file.");
			}

			ConfigParseOptions options = ConfigParseOptions.defaults()
					.setSyntax(ConfigSyntax.CONF);
			
			Config config = ConfigFactory.parseFile(propFile,options).resolve();

			props = toProperties(config);
			validatePropFile();
		}
		else {
			throw new ExceptionInInitializerError("dcmrsbroker.home property is not set.");
		}
	}

	private Environment()
	{
	}

	private static void validatePropFile()
	{
		for (Key key : Key.values()) {
			if(!key.required) {
				continue;
			}
			
			String value = props.getProperty(key.propName, key.defaultValue);

			if (value == null) {
				throw new RuntimeException(key.propName
										   + " key is missing in dcmrsbroker.properties file.");
			}
			else if (value.isEmpty()) {
				throw new RuntimeException("Missing value for key "
										   + key.propName
										   + " in dcmrsbroker.properties file.");
			}
		}

	}

	/**
	 * Convert Config to Properties
	 *
	 * @param config the config (must not be null)
	 * @return the properties
	 */
	private static Properties toProperties(Config config) 
	{
        Properties properties = new Properties();
		
        config.entrySet().forEach(e -> 
				properties.setProperty(e.getKey(), config.getString(e.getKey())));
		
        return properties;
    }
		
	/**
	 * Get the configured properties
	 *
	 * @return the properties
	 */
	public static Properties getProperties()
	{
		Properties clone = new Properties();
		clone.putAll(props);

		return clone;
	}

	/**
	 * Get the specified property as a string. Returning the default value if
	 * the property is not found
	 *
	 * @param key the key (must not be null)
	 * @return the property value or the default if not found
	 */
	public static String getProperty(Key key)
	{
		return props.getProperty(key.propName, key.defaultValue);
	}

	/**
	 * Get the specified property as an int. 
	 *
	 * @param key the key (must not be null)
	 * @return the property value or the default if not found
	 */
	public static int getPropertyAsInt(Key key)
	{
		return Integer.parseInt(getProperty(key));
	}

	/**
	 * Get the specified property as a boolean. 
	 *
	 * @param key the key (must not be null)
	 * @return the property value or the default if not found
	 */
	public static boolean getPropertyAsBoolean(Key key)
	{
		return Boolean.parseBoolean(getProperty(key));
	}

	public static enum Key
	{
		QIDO_REMOTE_AE("qido.remote_ae", true),
		QIDO_LOCAL_AE("qido.local_ae", true),
		QIDO_REMOTE_HOST("qido.remote_host", true),
		QIDO_REMOTE_PORT("qido.remote_port", "11112"),
		QIDO_URL_BASE("qido.url_base", "/qido-rs"),
		
		
		WADO_REMOTE_AE("wado.remote_ae", true),
		WADO_LOCAL_AE("wado.local_ae", true),
		WADO_REMOTE_HOST("wado.remote_host", true),
		WADO_REMOTE_PORT("wado.remote_port", "11112"),
		WADO_HTTP_RETRY_AFTER("wado.http_retry_after", "600"),
		WADO_RETRY_DELAY_IN_SECS("wado.retry_delay_in_secs", "600"),
		WADO_MAX_RETRY_ATTEMPTS("wado.max_retry_attempts", "6"),
		WADO_RETRIEVE_TIMEOUT_IN_SECS("wado.retrieve_timeout_in_secs", "120"),
		WADO_IGNORE_MISSING_OBJECTS("wado.ignore_missing_objects", "false"),
		WADO_URL_BASE("wado.url_base", "/wado-rs"),
		
		
		
		SCP_LOCAL_AE("scp.local_ae", true),
		SCP_LOCAL_PORT("scp.local_port", "11112"),
		SCP_CACHE_DIR_PATH("scp.cache_dir_path", true),
		SCP_CACHE_MAX_AGE("scp.cache_max_age_in_min", "60");

		private Key(String propName, boolean required)
		{
			this.propName = propName;
			this.defaultValue = null;
			this.required = required;
		}

		private Key(String propName, String defaultValue)
		{
			this.propName = propName;
			this.defaultValue = defaultValue;
			this.required = false;
		}

		private final String propName;

		public String getPropName()
		{
			return propName;
		}

		private final String defaultValue;

		public String getDefaultValue()
		{
			return defaultValue;
		}
		
		private final boolean required;
		
		public boolean isRequired()
		{
			return required;
		}

	}
}
