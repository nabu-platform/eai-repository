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

import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.authentication.api.Token;

public interface FeatureConfigurator {
	@WebResult(name = "enabledFeatures")
	public List<String> getEnabledFeatures(@WebParam(name = "token") Token token);
	// the context that this configurator operates in
	// if no context, it operates everywhere
	// it can be a comma separated list of contexts
	public String getContext();
	// when the feature configuration was last modified
	public Date getLastModified();
}
