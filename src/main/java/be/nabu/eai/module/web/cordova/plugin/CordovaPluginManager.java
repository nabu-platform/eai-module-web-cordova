/*
* Copyright (C) 2016 Alexander Verbruggen
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

package be.nabu.eai.module.web.cordova.plugin;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class CordovaPluginManager extends JAXBArtifactManager<CordovaPluginConfiguration, CordovaPlugin> {

	public CordovaPluginManager() {
		super(CordovaPlugin.class);
	}

	@Override
	protected CordovaPlugin newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new CordovaPlugin(id, container, repository);
	}

}
