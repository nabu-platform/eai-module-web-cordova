package be.nabu.eai.module.web.cordova.plugin;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class CordovaPlugin extends JAXBArtifact<CordovaPluginConfiguration> {

	public CordovaPlugin(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "cordova-plugin.xml", CordovaPluginConfiguration.class);
	}

}
