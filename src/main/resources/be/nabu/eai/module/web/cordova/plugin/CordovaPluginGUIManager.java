package be.nabu.eai.module.web.cordova.plugin;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class CordovaPluginGUIManager extends BaseJAXBGUIManager<CordovaPluginConfiguration, CordovaPlugin>{

	public CordovaPluginGUIManager() {
		super("Cordova Plugin", CordovaPlugin.class, new CordovaPluginManager(), CordovaPluginConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected CordovaPlugin newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new CordovaPlugin(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Mobile";
	}
}
