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

package be.nabu.eai.repository.impl;

import java.util.Date;

import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;

// we want to make sure all events are timestamped in the correct order
public class CreatedDateEnricher implements EventEnricher {

	@SuppressWarnings("unchecked")
	@Override
	public Object enrich(Object object) {
		if (!(object instanceof ComplexContent)) {
			object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
		}
		if (object != null) {
			// if we have a field called "sessionId", we enrich it
			if (((ComplexContent) object).getType().get("created") != null) {
				if (((ComplexContent) object).get("created") == null) {
					((ComplexContent) object).set("created", new Date());
				}
			}
		}
		return null;
	}

}
