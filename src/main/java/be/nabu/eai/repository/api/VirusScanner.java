/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.repository.api;

import java.io.InputStream;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.http.api.HTTPEntity;
import be.nabu.libs.services.api.ServiceException;

public interface VirusScanner extends Artifact {
	// icap has specific support for http entities, streams are wrapped into entities...
	public VirusInfection scan(HTTPEntity entity) throws ServiceException;
	// others may only implement the stream-based scanning, at that point it needs to format the http entities in order to scan them...
	public VirusInfection scan(InputStream input) throws ServiceException;
}
