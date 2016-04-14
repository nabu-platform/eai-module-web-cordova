package be.nabu.eai.module.web.cordova;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class CordovaApplicationManager extends JAXBArtifactManager<CordovaApplicationConfiguration, CordovaApplication> {

	public CordovaApplicationManager() {
		super(CordovaApplication.class);
	}

	@Override
	protected CordovaApplication newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new CordovaApplication(id, container, repository);
	}

}
