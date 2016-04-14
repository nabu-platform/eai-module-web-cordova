package be.nabu.eai.module.web.cordova;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class CordovaApplication extends JAXBArtifact<CordovaApplicationConfiguration> {

	public CordovaApplication(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "cordova-application.xml", CordovaApplicationConfiguration.class);
	}

}
