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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Filters files by suffix
 *
 * @author Wyatt Tellis
 * @since 1.0.0
 * @version 1.0.0
 */
class FileFilter implements Predicate<Path>
{
	private final String suffix;

	FileFilter(String suffix)
	{
		this.suffix = "." + suffix;
	}

	@Override
	public boolean test(Path p)
	{
		if (Files.isDirectory(p)) {
			return false;
		}
		else if (p.getFileName().toString().endsWith(suffix)) {
			return true;
		}
		else {
			return false;
		}
	}

}
