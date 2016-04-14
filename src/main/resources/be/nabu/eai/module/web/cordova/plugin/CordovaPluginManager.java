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
