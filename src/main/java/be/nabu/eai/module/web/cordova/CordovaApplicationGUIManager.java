package be.nabu.eai.module.web.cordova;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.component.WebComponent;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Orientation;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Platform;
import be.nabu.eai.module.web.cordova.plugin.CordovaPlugin;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.glue.MultipleRepository;
import be.nabu.glue.ScriptRuntime;
import be.nabu.glue.ScriptUtils;
import be.nabu.glue.api.Script;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatters.SimpleOutputFormatter;
import be.nabu.glue.impl.methods.SystemMethods.SystemProperty;
import be.nabu.glue.impl.parsers.GlueParserProvider;
import be.nabu.glue.repositories.ScannableScriptRepository;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.libs.http.glue.GlueListener;
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
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

// TODO: add "clear" button to clear the project (also the keystore!!)
// TODO: add "run in release" mode can have debug mode, not sure if "run in release" is necessary? perhaps simply build
// TODO: add "build" button
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
				
			// synchronize the map for the applications
			Map<String, String> platformVersions = artifact.getConfiguration().getPlatformVersions();
			List<String> platforms = new ArrayList<String>(platformVersions.keySet());
			for (Platform platform : artifact.getConfiguration().getPlatforms()) {
				if (!platformVersions.containsKey(platform.name())) {
					platformVersions.put(platform.name(), null);
				}
				platforms.remove(platform.name());
			}
			for (String platform : platforms) {
				platformVersions.remove(platform);
			}
			
			final TextArea outputLog = new TextArea();
			final TextArea errorLog = new TextArea();
			
			runButton.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						outputLog.clear();
						errorLog.clear();
						boolean alwaysClean = true;
						String nodePath = MainController.getProperties().getProperty("NODE_PATH", System.getProperty("NODE_PATH", System.getenv("NODE_PATH")));
						if (nodePath == null) {
							logger.warn("NODE_PATH is not configured in developer.properties, make sure both node and npm are available in your system PATH");
						}
						else if (!nodePath.endsWith("/")) {
							nodePath += "/";
						}
						String androidHome = MainController.getProperties().getProperty("ANDROID_HOME", System.getProperty("ANDROID_HOME", System.getenv("ANDROID_HOME")));
						List<SystemProperty> properties = new ArrayList<SystemProperty>();
						SystemProperty systemProperty = new SystemProperty("PATH", (nodePath == null ? "" : nodePath + System.getProperty("path.separator")) + System.getenv("PATH"));
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
							exec(
								folder.getAbsolutePath(),
								// the --prefix forces npm to install it in the local directory instead of potentially refreshing a previous installation in an unknown location
								new String [] { (nodePath == null ? "" : nodePath) + "npm", "install", "--prefix", folder.getAbsolutePath(), "cordova" },
								null,
								properties,
								outputLog,
								errorLog,
								true
							);
						}
						if (combo.getSelectionModel().getSelectedItem().equals(Platform.IOS)) {
							File ios = new File(folder, "node_modules/ios-deploy/build/Release");
							logger.info("Installing ios-deploy");
							exec(
								folder.getAbsolutePath(),
								// the --prefix forces npm to install it in the local directory instead of potentially refreshing a previous installation in an unknown location
								new String [] { (nodePath == null ? "" : nodePath) + "npm", "install", "--prefix", folder.getAbsolutePath(), "ios-deploy" },
								null,
								properties,
								outputLog,
								errorLog,
								true
							);
							String iosPath = ios.getAbsolutePath();
							if (!iosPath.endsWith("/")) {
								iosPath += "/";
							}
							systemProperty.setValue(iosPath + System.getProperty("path.separator") + systemProperty.getValue());
						}
						String cordovaPath = cordova.getAbsolutePath();
						if (!cordovaPath.endsWith("/")) {
							cordovaPath += "/";
						}
						systemProperty.setValue(cordovaPath + System.getProperty("path.separator") + systemProperty.getValue());
						logger.info("Using PATH = " + systemProperty.getValue());
						
						if (alwaysClean) {
							FileDirectory cordovaFolder = new FileDirectory(null, folder, false);
							cordovaFolder.delete(artifact.getConfiguration().getName());
						}

						if (!artifact.getConfiguration().getTitle().replaceAll("[\\s]+", "").equals(artifact.getConfiguration().getTitle())) {
							MainController.getInstance().notify(new ValidationMessage(Severity.WARNING, "There is whitespace in the application title. This can pose a problem for ios in combination with (certain) plugins, use something like 'cordova-plugin-app-name'"));
						}
						// add cordova to the path variable
						File project = new File(folder, artifact.getConfiguration().getName());
						// ios can break if the title contains whitespace because it uses the name as part of the folder structure to place plugins in
						// we create the application with a messed up title and replace it later on in post processing with the correct one
						// create the project if it doesn't exist yet
						if (!project.exists()) {
							logger.info("Creating the application in: " + folder.getAbsolutePath() + " using cordova in: " + cordovaPath);
							exec(
								folder.getAbsolutePath(), 
								new String [] { cordovaPath + "cordova", "create", artifact.getConfiguration().getName(), artifact.getConfiguration().getNamespace(), artifact.getConfiguration().getTitle() }, 
								null, 
								properties,
								outputLog,
								errorLog,
								true
							);
						}
						// TODO: interpret the results of "cordova platform list" to only add if required and delete if necessary
						// currently we simply keep adding as cordova simply prints an exception if the platform already exists
						String version = artifact.getConfiguration().getPlatformVersions().get(combo.getSelectionModel().getSelectedItem().name());
						version = version == null || version.trim().isEmpty() ? "" : "@" + version;
						logger.info("Adding platform " + combo.getSelectionModel().getSelectedItem().getCordovaName() + version);
						exec(
							project.getAbsolutePath(),
							new String [] { cordovaPath + "cordova", "platform", "add", combo.getSelectionModel().getSelectedItem().getCordovaName() + version },
							null,
							properties,
							outputLog,
							errorLog,
							true
						);
						// TODO: clean plugins every time (it will NOT override update variable properties)
						// same for the plugins
						if (artifact.getConfiguration().getPlugins() != null) {
							List<CordovaPlugin> plugins = new ArrayList<CordovaPlugin>(artifact.getConfiguration().getPlugins());
							// crosswalk does NOT play nice with other plugins, it has to be added _first_ before others otherwise you get strange behavior:
							// we have seen "VERSION DOWNGRADE"
							// we have seen "DUPLICATE PERMISSION"
							// once those were resolved by removing the application from the device, we deployed and got an error that "device ready" did not fire within 5 seconds and got an empty screen
							// by rebuilding with that plugin first, we got a decent deployment
							Collections.sort(plugins, new Comparator<CordovaPlugin>() {
								@Override
								public int compare(CordovaPlugin o1, CordovaPlugin o2) {
									try {
										if (o1.getConfiguration().getName().equals("cordova-plugin-crosswalk-webview")) {
											return -1;
										}
										else if (o2.getConfiguration().getName().equals("cordova-plugin-crosswalk-webview")) {
											return 1;
										}
										else {
											return 0;
										}
									}
									catch (IOException e) {
										throw new RuntimeException(e);
									}
								}
							});
							for (CordovaPlugin plugin : plugins) {
								List<String> parts = new ArrayList<String>();
								parts.add(cordovaPath + "cordova");
								parts.add("plugin");
								parts.add("add");
								if (plugin.getConfiguration().getVersion() != null) {
									parts.add(plugin.getConfiguration().getName() + "@" + plugin.getConfiguration().getVersion());
								}
								else {
									parts.add(plugin.getConfiguration().getName());
								}
								if (plugin.getConfiguration().getVariables() != null) {
									for (String variable : plugin.getConfiguration().getVariables()) {
										parts.add("--variable");
										// format: key=value
										parts.add(variable);
									}
								}
								exec(
									project.getAbsolutePath(),
									parts.toArray(new String[parts.size()]),
									null,
									properties,
									outputLog,
									errorLog,
									true
								);		
							}
						}
						
						logger.info("Cleaning the www directory");
						// clean the www folder (TODO: do a smart merge for performance reasons)
						File www = new File(project, "www");
						FileDirectory projectDirectory = new FileDirectory(null, project, false);
						projectDirectory.delete("www");
						FileDirectory wwwDirectory = (FileDirectory) projectDirectory.create("www", Resource.CONTENT_TYPE_DIRECTORY);
						
						MultipleRepository repository = new MultipleRepository(null);
						ResourceContainer<?> publicFolder = (ResourceContainer<?>) artifact.getConfiguration().getApplication().getDirectory().getChild(EAIResourceRepository.PUBLIC);
						// copy the resource files to the www directory
						if (publicFolder != null) {
							ResourceContainer<?> resources = (ResourceContainer<?>) publicFolder.getChild("resources");
							if (resources != null) {
								logger.info("Copying resources...");
								ResourceUtils.copy(resources, wwwDirectory);
							}
						}

						// run all the scripts
						ServiceMethodProvider serviceMethodProvider = new ServiceMethodProvider(artifact.getRepository(), artifact.getRepository(), artifact.getRepository().getServiceRunner());
						GlueParserProvider parserProvider = new GlueParserProvider(serviceMethodProvider);
						
						// add the repository for this artifact
						addRepository(repository, artifact.getConfiguration().getApplication().getDirectory(), parserProvider);
						// add repository of web fragments
						addRepository(repository, artifact.getConfiguration().getApplication().getConfiguration().getWebFragments(), parserProvider);
						
						logger.info("Copying pages...");
						WebApplication application = artifact.getConfiguration().getApplication();
						String hostName = application.getConfiguration().getVirtualHost().getConfiguration().getHost();
						Integer port = application.getConfiguration().getVirtualHost().getConfiguration().getServer().getConfiguration().getPort();
						boolean secure = application.getConfiguration().getVirtualHost().getConfiguration().getServer().getConfiguration().getKeystore() != null;
						String host = null;
						if (hostName != null) {
							if (port != null) {
								hostName += ":" + port;
							}
							host = secure ? "https://" : "http://";
							host += hostName;
						}
						Map<String, String> environment = new HashMap<String, String>();
						environment.put("mobile", "true");
						environment.put("web", "false");
						environment.put("url", host);
						environment.put("host", hostName);
						environment.put("hostName", application.getConfiguration().getVirtualHost().getConfiguration().getHost());
						environment.put("webApplicationId", application.getId());
						for (Script script : repository) {
							ScriptRuntime runtime = new ScriptRuntime(script, new SimpleExecutionEnvironment("local", environment), false, new HashMap<String, Object>());
							StringWriter writer = new StringWriter();
							SimpleOutputFormatter outputFormatter = new SimpleOutputFormatter(writer, false, false);
							runtime.setFormatter(outputFormatter);
							runtime.run();
							String path = ScriptUtils.getFullName(script).replace(".", "/") + (script.getName().equals("index") ? ".html" : "");
							Resource file = ResourceUtils.touch(wwwDirectory, path);
							WritableContainer<ByteBuffer> writable = ((WritableResource) file).getWritable();
							try {
								writable.write(IOUtils.wrap(writer.toString().getBytes(), true));
							}
							finally {
								writable.close();
							}
						}
						
						logger.info("Adding preferences...");
						FileItem configurationChild = (FileItem) projectDirectory.getChild("config.xml");
						if (configurationChild == null) {
							throw new IllegalStateException("Could not find config.xml in the application root, please clean the cordova application");
						}
						ReadableContainer<ByteBuffer> readable = configurationChild.getReadable();
						byte[] bytes;
						try {
							bytes = IOUtils.toBytes(readable);
						}
						finally {
							readable.close();
						}
						String config = new String(bytes, "UTF-8");
						// remove current option for fullscreen
						config = config.replaceAll("(?s)<preference name=\"Fullscreen\"[^>]+/>", "");
						// add new option
						config = config.replaceAll("(?s)(</widget>)", "\t<preference name=\"Fullscreen\" value=\"" + (artifact.getConfiguration().getFullscreen() != null && artifact.getConfiguration().getFullscreen()) + "\" />\n$1");
						// remove current option for orientation
						config = config.replaceAll("(?s)<preference name=\"Orientation\"[^>]+/>", "");
						// add new option
						config = config.replaceAll("(?s)(</widget>)", "\t<preference name=\"Orientation\" value=\"" + (artifact.getConfiguration().getOrientation() != null ? artifact.getConfiguration().getOrientation().getCordovaName() : Orientation.BOTH.getCordovaName()) + "\" />\n$1");
						WritableContainer<ByteBuffer> writable = configurationChild.getWritable();
						try {
							writable.write(IOUtils.wrap(config.getBytes("UTF-8"), true));
						}
						finally {
							writable.close();
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
								exec(
									project.getAbsolutePath(),
									new String [] { cordovaPath + "cordova", "run", combo.getSelectionModel().getSelectedItem().getCordovaName(),
//										"--release",
										"--",
										"--keystore=keystore.jks", 
										"--storePassword=" + artifact.getConfiguration().getKeystore().getConfiguration().getPassword(),
										"--alias=" + artifact.getConfiguration().getSignatureAlias(),
										"--password=" + artifact.getConfiguration().getKeystore().getConfiguration().getKeyPasswords().get(artifact.getConfiguration().getSignatureAlias())},
										// can also set the private key password using "--password=password"
									null,
									properties,
									outputLog,
									errorLog,
									false
								);
							}
						}
						else {
							// now run it
							exec(
								project.getAbsolutePath(),
								new String [] { "cordova", "run", combo.getSelectionModel().getSelectedItem().getCordovaName() },
								null,
								properties,
								outputLog,
								errorLog,
								false
							);
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
			vbox.getChildren().addAll(buttons, anchor, outputLog, errorLog);
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
	
	private static void replace(FileDirectory projectDirectory, String path, String search, String replace) throws IOException {
		FileItem configurationChild = (FileItem) ResourceUtils.resolve(projectDirectory, path);
		if (configurationChild == null) {
			throw new IllegalStateException("Could not find '" + path + "' in the application root, please clean the cordova application");
		}
		ReadableContainer<ByteBuffer> readable = configurationChild.getReadable();
		byte[] bytes;
		try {
			bytes = IOUtils.toBytes(readable);
		}
		finally {
			readable.close();
		}
		String config = new String(bytes, "UTF-8");
		config = config.replaceAll(search, replace);
		WritableContainer<ByteBuffer> writable = configurationChild.getWritable();
		try {
			writable.write(IOUtils.wrap(config.getBytes("UTF-8"), true));
		}
		finally {
			writable.close();
		}
	}

	private Thread errorThread, outputThread;
	
	private void exec(String directory, String [] commands, List<byte[]> inputContents, List<SystemProperty> systemProperties, final TextArea outputTarget, final TextArea errorTarget, boolean waitFor) throws IOException, InterruptedException {
		if (!directory.endsWith("/")) {
			directory += "/";
		}
		File dir = new File(directory);
		if (!dir.exists()) {
			throw new FileNotFoundException("Can not find directory: " + directory);
		}
		else if (!dir.isDirectory()) {
			throw new IOException("The file is not a directory: " + directory);
		}
		String [] env = null;
		if (systemProperties != null && !systemProperties.isEmpty()) {
			List<SystemProperty> allProperties = new ArrayList<SystemProperty>();
			// get the current environment properties, if you pass in _any_ properties, it will not inherit the ones from the current environment
			Map<String, String> systemEnv = System.getenv();
			for (String key : systemEnv.keySet()) {
				allProperties.add(new SystemProperty(key, systemEnv.get(key)));
			}
			allProperties.addAll(systemProperties);
			env = new String[allProperties.size()];
			for (int i = 0; i < allProperties.size(); i++) {
				env[i] = allProperties.get(i).getKey() + "=" + allProperties.get(i).getValue();
			}
		}
		Process process = Runtime.getRuntime().exec(commands, env, dir);
		if (inputContents != null && !inputContents.isEmpty()) {
			OutputStream output = new BufferedOutputStream(process.getOutputStream());
			try {
				for (byte [] content : inputContents) {
					output.write(content);
				}
			}
			finally {
				output.close();
			}
		}
		errorThread = new Thread(new InputCopier(process.getErrorStream(), errorTarget));
		errorThread.start();
		
		outputThread = new Thread(new InputCopier(process.getInputStream(), outputTarget));
		outputThread.start();
		
		if (waitFor) {
			process.waitFor(5, TimeUnit.MINUTES);
		}
	}

	private void addRepository(MultipleRepository repository, List<WebFragment> fragments, GlueParserProvider parserProvider) throws IOException {
		if (fragments != null) {
			for (WebFragment fragment : fragments) {
				if (fragment instanceof WebComponent) {
					addRepository(repository, ((WebComponent) fragment).getDirectory(), parserProvider);
					addRepository(repository, ((WebComponent) fragment).getConfiguration().getWebFragments(), parserProvider);
				}
			}
		}
	}
	
	private void addRepository(MultipleRepository repository, ResourceContainer<?> root, GlueParserProvider parserProvider) throws IOException {
		ResourceContainer<?> publicFolder = (ResourceContainer<?>) root.getChild(EAIResourceRepository.PUBLIC);
		ResourceContainer<?> privateFolder = (ResourceContainer<?>) root.getChild(EAIResourceRepository.PRIVATE);
		logger.info("Adding root repository: " + root + ", public = " + publicFolder + ", private = " + privateFolder);
		if (publicFolder != null) {
			ResourceContainer<?> pagesFolder = (ResourceContainer<?>) publicFolder.getChild("pages");
			if (pagesFolder != null) {
				ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, pagesFolder, parserProvider, Charset.defaultCharset());
				scannableScriptRepository.setGroup(GlueListener.PUBLIC);
				repository.add(scannableScriptRepository);
			}
		}
		// add private scripts
		if (privateFolder != null) {
			Resource child = privateFolder.getChild("scripts");
			if (child != null) {
				repository.add(new ScannableScriptRepository(repository, (ResourceContainer<?>) child, parserProvider, Charset.defaultCharset()));
			}
		}
	}
	
	private final class InputCopier implements Runnable {
		private final TextArea target;
		private InputStream input;

		private InputCopier(InputStream input, TextArea target) {
			this.input = input;
			this.target = target;
		}

		@Override
		public void run() {
			ReadableContainer<CharBuffer> wrapReadable = IOUtils.wrapReadable(IOUtils.wrap(input), Charset.defaultCharset());
			try {
				IOUtils.copyChars(wrapReadable, new WritableContainer<CharBuffer>() {
					@Override
					public void close() throws IOException {
						// do nothing
					}
					@Override
					public long write(CharBuffer buffer) throws IOException {
						long size = buffer.remainingData();
						final String content = IOUtils.toString(buffer);
						javafx.application.Platform.runLater(new Runnable() {
							@Override
							public void run() {
								target.appendText(content);
							}
						});
						return size;
					}

					@Override
					public void flush() throws IOException {
						// do nothing
					}
				});
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
