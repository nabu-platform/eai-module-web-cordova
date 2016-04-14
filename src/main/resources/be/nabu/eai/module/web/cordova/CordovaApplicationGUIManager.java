package be.nabu.eai.module.web.cordova;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BasePortableGUIManager;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Platform;
import be.nabu.eai.module.web.cordova.plugin.CordovaPlugin;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.glue.impl.methods.SystemMethods.SystemProperty;
import be.nabu.glue.impl.providers.SystemMethodProvider;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class CordovaApplicationGUIManager extends BasePortableGUIManager<CordovaApplication, BaseArtifactGUIInstance<CordovaApplication>> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public CordovaApplicationGUIManager() {
		super("Cordova Application", CordovaApplication.class, new CordovaApplicationManager());
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected CordovaApplication newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new CordovaApplication(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Mobile";
	}

	@Override
	public void display(MainController controller, AnchorPane pane, CordovaApplication artifact) throws IOException, ParseException {
		VBox vbox = new VBox();
		HBox buttons = new HBox();
		
		ComboBox<Platform> combo = new ComboBox<Platform>();
		if (artifact.getConfiguration().getPlatforms() != null) {
			combo.getItems().addAll(artifact.getConfiguration().getPlatforms());
		}
		Button button = new Button("Run");
		button.disableProperty().bind(combo.getSelectionModel().selectedItemProperty().isNull());
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				try {
					String nodePath = MainController.getProperties().getProperty("NODE_PATH", System.getProperty("NODE_PATH", System.getenv("NODE_PATH")));
					if (nodePath == null) {
						MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Please configure the file path to the bin folder of your local nodejs installation in the property 'NODE_PATH'"));
					}
					else {
						String androidHome = MainController.getProperties().getProperty("ANDROID_HOME", System.getProperty("ANDROID_HOME", System.getenv("ANDROID_HOME")));
						List<SystemProperty> properties = new ArrayList<SystemProperty>();
						SystemProperty pathProperty = new SystemProperty("PATH", nodePath);
						properties.add(pathProperty);
						if (androidHome != null) {
							properties.add(new SystemProperty("ANDROID_HOME", androidHome));
						}
						File folder = new File(".nabu/cordova");
						// create a work folder for cordova
						if (!folder.exists()) {
							folder.mkdirs();
						}
						// install cordova if it does not exist yet
						File cordova = new File(folder, "node_modules/cordova/bin");
						if (!cordova.exists()) {
							SystemMethodProvider.exec(
								folder.getAbsolutePath(),
								new String [] { "npm", "install", "cordova" },
								null,
								properties
							);
						}
						// add cordova to the path variable
						pathProperty.setValue(pathProperty.getValue() + System.getProperty("path.separator") + cordova.getAbsolutePath());
						File project = new File(folder, artifact.getConfiguration().getName());
						// create the project if it doesn't exist yet
						if (!project.exists()) {
							SystemMethodProvider.exec(
								folder.getAbsolutePath(), 
								new String [] { "cordova", "create", artifact.getConfiguration().getName(), artifact.getConfiguration().getNamespace(), artifact.getConfiguration().getTitle() }, 
								null, 
								properties
							);
						}
						// TODO: interpret the results of "cordova platform list" to only add if required and delete if necessary
						// currently we simply keep adding as cordova simply prints an exception if the platform already exists
						SystemMethodProvider.exec(
							project.getAbsolutePath(),
							new String [] { "cordova", "platform", "add", combo.getSelectionModel().getSelectedItem().getCordovaName() },
							null,
							properties
						);
						// same for the plugins
						if (artifact.getConfiguration().getPlugins() != null) {
							for (CordovaPlugin plugin : artifact.getConfiguration().getPlugins()) {
								List<String> parts = new ArrayList<String>();
								parts.add("cordova");
								parts.add("plugin");
								parts.add("add");
								parts.add(plugin.getConfiguration().getName());
								if (plugin.getConfiguration().getVariables() != null) {
									for (String variable : plugin.getConfiguration().getVariables()) {
										parts.add("--variable");
										// format: key=value
										parts.add(variable);
									}
								}
								SystemMethodProvider.exec(
									project.getAbsolutePath(),
									parts.toArray(new String[parts.size()]),
									null,
									properties
								);		
							}
						}
						// now run it
						SystemMethodProvider.exec(
							
						);
					}
				}
				catch (Exception e) {
					logger.error("Could not run application", e);
				}
			}
		});
		
		vbox.getChildren().addAll(buttons);
		AnchorPane.setLeftAnchor(vbox, 0d);
		AnchorPane.setRightAnchor(vbox, 0d);
		AnchorPane.setTopAnchor(vbox, 0d);
		AnchorPane.setBottomAnchor(vbox, 0d);
		pane.getChildren().add(vbox);
	}

	@Override
	protected BaseArtifactGUIInstance<CordovaApplication> newGUIInstance(Entry entry) {
		return new BaseArtifactGUIInstance<CordovaApplication>(this, entry);
	}

	@Override
	protected void setEntry(BaseArtifactGUIInstance<CordovaApplication> guiInstance, ResourceEntry entry) {
		guiInstance.setEntry(entry);
	}

	@Override
	protected void setInstance(BaseArtifactGUIInstance<CordovaApplication> guiInstance, CordovaApplication instance) {
		guiInstance.setArtifact(instance);
	}
	
}
