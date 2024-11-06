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

import java.util.List;

import javax.jws.WebParam;

import be.nabu.libs.services.api.ServiceException;

/**
 * Take a list of objects and enrich (or persist updates to) the listed fields
 * 
 * You can set two properties on a type (or element!):
 * - enricher: you fill in the name of the enricher implementation, e.g. nabu.cms.core.providers.enricher.address
 * 		You can add the id field, otherwise the primary key field is assumed: nabu.cms.core.providers.enricher.address:id
 * 		You can add additional named configuration: nabu.cms.core.providers.enricher.address:id;addressType=test;drop=street,number
 * 		We can set the default enrichment specs on NodeAddress type itself! The implementation can also make assumptions, for instance if the field you are injecting into is a non-list called "invoiceAddress" and there is no explicit addressType configuration, it can assume that the type is "invoice"
 * 		If you want specific behavior, you can add additional configuration when actually adding NodeAddress into your presumed node extension
 * 		NodeTag can ship with a custom simple type extension of boolean (and date!), the nodetag enricher can then inspect the type to see which value you want (boolean to indicate whether you have it or not, date to indicate since when you have it)
 * - persister: ideally speaking you separated out the "fetching of additional data" from the enrichment because the persister will need to fetch the additional data as well so it can detect changes to it and persist them
 * 
 * The implementation gets a list of everything (objects and fields) in the assumption that it can optimize the resolving (e.g. 1 big select rather than 10 tiny selects)
 * It needs to enrich the object instances itself, not generate new instances. Enrichers don't really know much about the parent object so they shouldn't be able to do fancy stuff anyway (e.g. start from an id and resolve the full object)
 * 
 * Enrichment can take place from any source (e.g. odata), not just the database.
 * JDBC services can be updated to automatically support enrichment. This will make it work out of the box for a lot of things.
 * 
 * CRUD providers should get a new boolean that indicate if they have enriched the data themselves. JDBC-based providers should set this to true, other providers probably to false.
 * This way CRUD can also perform enrichment on entirely different types. This should make it work end to end
 */
public interface ObjectEnricher {
	public void apply(@WebParam(name = "typeId") String typeId, @WebParam(name = "language") String language, @WebParam(name = "instances") List<Object> instances, @WebParam(name = "keyField") String keyField, @WebParam(name = "fields") List<String> fieldsToEnrich) throws ServiceException;
	public void persist(@WebParam(name = "typeId") String typeId, @WebParam(name = "language") String language, @WebParam(name = "instances") List<Object> instances, @WebParam(name = "keyField") String keyField, @WebParam(name = "fields") List<String> fieldsToPersist) throws ServiceException;
}
