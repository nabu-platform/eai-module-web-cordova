package be.nabu.eai.module.web.cordova;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Platform;
import be.nabu.eai.module.web.cordova.plugin.CordovaPlugin;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.glue.ScriptRuntime;
import be.nabu.glue.ScriptUtils;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatters.SimpleOutputFormatter;
import be.nabu.glue.impl.methods.SystemMethods.SystemProperty;
import be.nabu.glue.impl.parsers.GlueParserProvider;
import be.nabu.glue.impl.providers.SystemMethodProvider;
import be.nabu.glue.repositories.ScannableScriptRepository;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.file.FileDirectory;
import be.nabu.libs.resources.file.FileItem;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class CordovaApplicationGUIManager extends BaseJAXBGUIManager<CordovaApplicationConfiguration, CordovaApplication> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	public CordovaApplicationGUIManager() {
		super("Cordova Application", CordovaApplication.class, new CordovaApplicationManager(), CordovaApplicationConfiguration.class);
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
	public void display(MainController controller, AnchorPane pane, CordovaApplication artifact) {
		try {
			VBox vbox = new VBox();
			HBox buttons = new HBox();
			
			ComboBox<Platform> combo = new ComboBox<Platform>();
			if (artifact.getConfiguration().getPlatforms() != null) {
				combo.getItems().addAll(artifact.getConfiguration().getPlatforms());
			}
			Button runButton = new Button("Run");
			runButton.disableProperty().bind(combo.getSelectionModel().selectedItemProperty().isNull());
			runButton.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						String nodePath = MainController.getProperties().getProperty("NODE_PATH", System.getProperty("NODE_PATH", System.getenv("NODE_PATH")));
						if (nodePath == null) {
							MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Please configure the file path to the bin folder of your local nodejs installation in the property 'NODE_PATH'"));
						}
						else {
							if (!nodePath.endsWith("/")) {
								nodePath += "/";
							}
							String androidHome = MainController.getProperties().getProperty("ANDROID_HOME", System.getProperty("ANDROID_HOME", System.getenv("ANDROID_HOME")));
							List<SystemProperty> properties = new ArrayList<SystemProperty>();
							SystemProperty systemProperty = new SystemProperty("PATH", nodePath);
							properties.add(systemProperty);
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
								logger.info("Installing cordova in: " + folder.getAbsolutePath());
								SystemMethodProvider.exec(
									folder.getAbsolutePath(),
									// the --prefix forces npm to install it in the local directory instead of potentially refreshing a previous installation in an unknown location
									new String [] { nodePath + "npm", "install", "--prefix", folder.getAbsolutePath(), "cordova" },
									null,
									properties
								);
							}
							String cordovaPath = cordova.getAbsolutePath();
							if (!cordovaPath.endsWith("/")) {
								cordovaPath += "/";
							}
							systemProperty.setValue(systemProperty.getValue() + System.getProperty("path.separator") + cordovaPath + System.getProperty("path.separator") + System.getenv("PATH"));
							// add cordova to the path variable
							File project = new File(folder, artifact.getConfiguration().getName());
							// create the project if it doesn't exist yet
							if (!project.exists()) {
								logger.info("Creating the application in: " + folder.getAbsolutePath() + " using cordova in: " + cordovaPath);
								SystemMethodProvider.exec(
									folder.getAbsolutePath(), 
									new String [] { cordovaPath + "cordova", "create", artifact.getConfiguration().getName(), artifact.getConfiguration().getNamespace(), artifact.getConfiguration().getTitle() }, 
									null, 
									properties
								);
							}
							// TODO: interpret the results of "cordova platform list" to only add if required and delete if necessary
							// currently we simply keep adding as cordova simply prints an exception if the platform already exists
							SystemMethodProvider.exec(
								project.getAbsolutePath(),
								new String [] { cordovaPath + "cordova", "platform", "add", combo.getSelectionModel().getSelectedItem().getCordovaName() },
								null,
								properties
							);
							// TODO: clean plugins every time (it will NOT override update variable properties)
							// same for the plugins
							if (artifact.getConfiguration().getPlugins() != null) {
								for (CordovaPlugin plugin : artifact.getConfiguration().getPlugins()) {
									List<String> parts = new ArrayList<String>();
									parts.add(cordovaPath + "cordova");
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
							
							logger.info("Cleaning the www directory");
							// clean the www folder (TODO: do a smart merge for performance reasons)
							File www = new File(project, "www");
							FileDirectory projectDirectory = new FileDirectory(null, project, false);
							projectDirectory.delete("www");
							FileDirectory wwwDirectory = (FileDirectory) projectDirectory.create("www", Resource.CONTENT_TYPE_DIRECTORY);
							
							ResourceContainer<?> publicFolder = (ResourceContainer<?>) artifact.getConfiguration().getApplication().getDirectory().getChild(EAIResourceRepository.PUBLIC);
							ResourceContainer<?> privateFolder = (ResourceContainer<?>) artifact.getConfiguration().getApplication().getDirectory().getChild(EAIResourceRepository.PRIVATE);
							
							// copy the resource files to the www directory
							if (publicFolder != null) {
								ResourceContainer<?> resources = (ResourceContainer<?>) publicFolder.getChild("resources");
								if (resources != null) {
									logger.info("Copying resources...");
									ResourceUtils.copy(resources, wwwDirectory);
								}
								ResourceContainer<?> pagesFolder = (ResourceContainer<?>) publicFolder.getChild("pages");
								if (pagesFolder != null) {
									logger.info("Copying pages...");
									ServiceMethodProvider serviceMethodProvider = new ServiceMethodProvider(artifact.getRepository(), artifact.getRepository());
									ScriptRepository repository = new ScannableScriptRepository(null, pagesFolder, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset());
									for (Script script : repository) {
										Map<String, String> environment = new HashMap<String, String>();
										environment.put("mobile", "true");
										ScriptRuntime runtime = new ScriptRuntime(script, new SimpleExecutionEnvironment("local", environment), false, new HashMap<String, Object>());
										StringWriter writer = new StringWriter();
										SimpleOutputFormatter outputFormatter = new SimpleOutputFormatter(writer, false);
										runtime.setFormatter(outputFormatter);
										runtime.run();
										String path = ScriptUtils.getFullName(script).replace(".", "/") + ".html";
										Resource file = ResourceUtils.touch(wwwDirectory, path);
										WritableContainer<ByteBuffer> writable = ((WritableResource) file).getWritable();
										try {
											writable.write(IOUtils.wrap(writer.toString().getBytes(), true));
										}
										finally {
											writable.close();
										}
									}
								}
							}
							
							// for android we need signatures based on the configured keystore
							if (Platform.ANDROID.equals(combo.getSelectionModel().getSelectedItem())) {
								if (artifact.getConfiguration().getKeystore() == null || artifact.getConfiguration().getSignatureAlias() == null) {
									MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Please configure a keystore and alias that can be used to sign the apk"));
								}
								else {
									// TODO: always overwrite the keystore otherwise changes are not picked up (e.g. if you updated the password!)
									File keystore = new File(project, "keystore.jks");
									if (!keystore.exists()) {
										artifact.getConfiguration().getKeystore().getKeyStore().save(new FileItem(null, keystore, false));
									}
									// now run it
									System.out.println(SystemMethodProvider.exec(
										project.getAbsolutePath(),
										new String [] { cordovaPath + "cordova", "run", combo.getSelectionModel().getSelectedItem().getCordovaName(),
											"--release",
											"--",
											"--keystore=keystore.jks", 
											"--storePassword=" + artifact.getConfiguration().getKeystore().getConfiguration().getPassword(),
											"--alias=" + artifact.getConfiguration().getSignatureAlias(),
											"--password=" + artifact.getConfiguration().getKeystore().getConfiguration().getKeyPasswords().get(artifact.getConfiguration().getSignatureAlias())},
											// can also set the private key password using "--password=password"
										null,
										properties
									));
								}
							}
							else {
								// now run it
								SystemMethodProvider.exec(
									project.getAbsolutePath(),
									new String [] { "cordova", "run", combo.getSelectionModel().getSelectedItem().getCordovaName() },
									null,
									properties
								);
							}
						}
					}
					catch (Exception e) {
						logger.error("Could not run application", e);
					}
				}
			});
	
			buttons.getChildren().addAll(combo, runButton);
			AnchorPane anchor = new AnchorPane();
			display(artifact, anchor);
			vbox.getChildren().addAll(buttons, anchor);
			AnchorPane.setLeftAnchor(vbox, 0d);
			AnchorPane.setRightAnchor(vbox, 0d);
			AnchorPane.setTopAnchor(vbox, 0d);
			AnchorPane.setBottomAnchor(vbox, 0d);
			pane.getChildren().add(vbox);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
