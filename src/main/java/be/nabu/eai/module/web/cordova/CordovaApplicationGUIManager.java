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

package be.nabu.eai.module.web.cordova;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationMethods;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.component.WebComponent;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Density;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Dimension;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.ImageType;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Orientation;
import be.nabu.eai.module.web.cordova.CordovaApplicationConfiguration.Platform;
import be.nabu.eai.module.web.cordova.plugin.CordovaPlugin;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Translator;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.StringSubstituter;
import be.nabu.glue.api.StringSubstituterProvider;
import be.nabu.glue.core.impl.methods.SystemMethods.SystemProperty;
import be.nabu.glue.core.impl.parsers.GlueParserProvider;
import be.nabu.glue.core.impl.providers.StaticJavaMethodProvider;
import be.nabu.glue.core.repositories.ScannableScriptRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatters.SimpleOutputFormatter;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.utils.MultipleRepository;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.glue.utils.ScriptUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.GlueWebParserProvider;
import be.nabu.libs.http.glue.impl.ResponseMethods;
import be.nabu.libs.http.glue.impl.ServerMethods;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.file.FileDirectory;
import be.nabu.libs.resources.file.FileItem;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.security.KeyStoreHandler;

// TODO: add "clear" button to clear the project (also the keystore!!)
// TODO: add "run in release" mode can have debug mode, not sure if "run in release" is necessary? perhaps simply build
// TODO: add "build" button
public class CordovaApplicationGUIManager extends BaseJAXBGUIManager<CordovaApplicationConfiguration, CordovaApplication> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	public CordovaApplicationGUIManager() {
		super("Cordova Application", CordovaApplication.class, new CordovaApplicationManager(), CordovaApplicationConfiguration.class);
		MainController.registerStyleSheet("cordova.css");
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

	private void filterByPlatform(CordovaApplication artifact, Map<String, ?> map) throws IOException {
		// synchronize the map for the applications
		List<String> platforms = new ArrayList<String>(map.keySet());
		if (artifact.getConfiguration().getPlatforms() != null) {
			for (Platform platform : artifact.getConfiguration().getPlatforms()) {
				if (!map.containsKey(platform.name())) {
					map.put(platform.name(), null);
				}
				platforms.remove(platform.name());
			}
		}
		for (String platform : platforms) {
			map.remove(platform);
		}
	}
	
	@Override
	public void display(MainController controller, AnchorPane pane, CordovaApplication artifact) {
		try {
			VBox vbox = new VBox();
			vbox.getStyleClass().add("content");
			vbox.getStyleClass().add("cordova");
			HBox buttons = new HBox();
			buttons.getStyleClass().add("buttons");
			
			ComboBox<Platform> combo = new ComboBox<Platform>();
			if (artifact.getConfiguration().getPlatforms() != null) {
				combo.getItems().addAll(artifact.getConfiguration().getPlatforms());
			}
			CheckBox release = new CheckBox("As Final Version");
			release.setSelected(false);
			Button runButton = new Button("Run");
			statusBox = new HBox();
			runButton.disableProperty().bind(combo.getSelectionModel().selectedItemProperty().isNull());
				
			filterByPlatform(artifact, artifact.getConfiguration().getPlatformVersions());
			
			final TextArea outputLog = new TextArea();
			final TextArea errorLog = new TextArea();
			
			runButton.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					run(artifact, combo.getSelectionModel().getSelectedItem(), release.isSelected(), outputLog, errorLog, true);
				}
			});
			
			Button runWithoutCleanButton = new Button("Run without clean");
			runWithoutCleanButton.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					run(artifact, combo.getSelectionModel().getSelectedItem(), release.isSelected(), outputLog, errorLog, false);
				}
			});
			runWithoutCleanButton.disableProperty().bind(combo.getSelectionModel().selectedItemProperty().isNull());
			
			buttons.getChildren().addAll(combo, release, runButton, runWithoutCleanButton, statusBox);
			AnchorPane anchor = new AnchorPane();
			display(artifact, anchor);
			vbox.getChildren().addAll(buttons, anchor, outputLog, errorLog);
			
			if (artifact.getConfiguration().getPlatforms() != null) {
				ResourceContainer<?> iconFolder = ResourceUtils.mkdirs(artifact.getDirectory(), EAIResourceRepository.PRIVATE + "/icon");
				ResourceContainer<?> splashFolder = ResourceUtils.mkdirs(artifact.getDirectory(), EAIResourceRepository.PRIVATE + "/splash");
				VBox imageBox = new VBox();
				imageBox.getStyleClass().add("images");
				for (Platform platform : artifact.getConfiguration().getPlatforms()) {
					// icons
					HBox iconButtons = new HBox();
					iconButtons.getStyleClass().add("buttons");
					Button setIcon = new Button("Set Icon");
					Resource iconChild = iconFolder.getChild(platform.name() + ".png");
					setIcon.addEventHandler(ActionEvent.ANY, new EventHandler<Event>() {
						@Override
						public void handle(Event event) {
							getImage("Set icon for " + platform.name(), new ImageHandlerImplementation(iconFolder, platform.name() + ".png", "image/png"));
						}
					});
					Button deleteIcon = new Button("Delete Icon");
					deleteIcon.addEventHandler(ActionEvent.ANY, new ImageDeletionHandler(iconFolder, platform.name() + ".png"));
					iconButtons.getChildren().addAll(setIcon, deleteIcon);
					Label iconLabel = new Label("Icon for: " + platform.name());
					iconLabel.getStyleClass().add("title");
					imageBox.getChildren().addAll(new Separator(), iconLabel, iconButtons);
					if (iconChild != null) {
						InputStream input = IOUtils.toInputStream(((ReadableResource) iconChild).getReadable());
						try {
							ImageView imageView = new ImageView(new Image(input));
							imageView.fitWidthProperty().set(200);
							imageView.setPreserveRatio(true);
							imageBox.getChildren().add(imageView);
						}
						finally {
							input.close();
						}
					}
					
					// splash screens
					HBox splashButtons = new HBox();
					splashButtons.getStyleClass().add("buttons");
					Button setSplash = new Button("Set Splash Image");
					Resource splashChild = splashFolder.getChild(platform.name() + ".png");
					setSplash.addEventHandler(ActionEvent.ANY, new EventHandler<Event>() {
						@Override
						public void handle(Event event) {
							getImage("Set splash image for " + platform.name(), new ImageHandlerImplementation(splashFolder, platform.name() + ".png", "image/png"));
						}
					});
					Button deleteSplash = new Button("Delete Splash Image");
					deleteSplash.addEventHandler(ActionEvent.ANY, new ImageDeletionHandler(splashFolder, platform.name() + ".png"));
					splashButtons.getChildren().addAll(setSplash, deleteSplash);
					Label splashLabel = new Label("Splash image for: " + platform.name());
					splashLabel.getStyleClass().add("title");
					imageBox.getChildren().addAll(new Separator(), splashLabel, splashButtons);
					if (splashChild != null) {
						InputStream input = IOUtils.toInputStream(((ReadableResource) splashChild).getReadable());
						try {
							ImageView imageView = new ImageView(new Image(input));
							imageView.fitWidthProperty().set(200);
							imageView.setPreserveRatio(true);
							imageBox.getChildren().add(imageView);
						}
						finally {
							input.close();
						}
					}
				}
				vbox.getChildren().add(imageBox);
			}
			
			ScrollPane scroll = new ScrollPane();
			scroll.setContent(vbox);
			vbox.prefWidthProperty().bind(scroll.widthProperty().subtract(50));
			AnchorPane.setLeftAnchor(scroll, 0d);
			AnchorPane.setRightAnchor(scroll, 0d);
			AnchorPane.setTopAnchor(scroll, 0d);
			AnchorPane.setBottomAnchor(scroll, 0d);
			pane.getChildren().add(scroll);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void run(CordovaApplication artifact, Platform targetPlatform, boolean release, final TextArea outputLog, final TextArea errorLog, boolean clean) {
		try {
			outputLog.clear();
			errorLog.clear();
			// (MUST) CONFIGURE, e.g. /usr/bin/
			String nodePath = MainController.getProperties().getProperty("NODE_PATH", System.getProperty("NODE_PATH", System.getenv("NODE_PATH")));
			if (nodePath == null) {
				logger.warn("NODE_PATH is not configured in developer.properties, make sure both node and npm are available in your system PATH");
			}
			else if (!nodePath.endsWith("/")) {
				nodePath += "/";
			}
			// (MUST) CONFIGURE e.g. /home/alex/Android/Sdk
			String androidHome = MainController.getProperties().getProperty("ANDROID_HOME", System.getProperty("ANDROID_HOME", System.getenv("ANDROID_HOME")));
			List<SystemProperty> properties = new ArrayList<SystemProperty>();
			SystemProperty systemProperty = new SystemProperty("PATH", (nodePath == null ? "" : nodePath + System.getProperty("path.separator"))
				+ (androidHome == null ? "" : androidHome + "/emulator" + System.getProperty("path.separator"))
				// apparently android platform tools has to be on the path as well for the VMs to work, otherwise the VM might start up but the app is not loaded
				+ (androidHome == null ? "" : androidHome + "/platform-tools" + System.getProperty("path.separator"))
				+ (androidHome == null ? "" : androidHome + "/tools" + System.getProperty("path.separator"))
				+ System.getenv("PATH"));
			properties.add(systemProperty);
			// (SHOULD) CONFIGURE e.g. /home/alex/Android/Sdk/platform-tools
			String androidRoot = MainController.getProperties().getProperty("ANDROID_SDK_ROOT", System.getProperty("ANDROID_SDK_ROOT", System.getenv("ANDROID_SDK_ROOT")));
			if (androidRoot != null) {
				properties.add(new SystemProperty("ANDROID_SDK_ROOT", androidRoot));
			}
			// (SHOULD) configure, especially if developer is not running in a version that is supported (does not work though?)
			if (System.getProperty("CORDOVA_JAVA_HOME") != null) {
				properties.add(new SystemProperty("JAVA_HOME", System.getProperty("CORDOVA_JAVA_HOME")));
			}
			else if (System.getProperty("JAVA_HOME") != null) {
				properties.add(new SystemProperty("JAVA_HOME", System.getProperty("JAVA_HOME")));
			}
			else if (System.getProperty("java.home") != null) {
				properties.add(new SystemProperty("JAVA_HOME", System.getProperty("java.home")));
			}
			if (androidHome != null) {
				properties.add(new SystemProperty("ANDROID_HOME", androidHome));
			}
			File folder = new File(".nabu/cordova");
			logger.info("Using build path: " + folder.getAbsolutePath());
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
			if (targetPlatform.equals(Platform.IOS)) {
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
			
			if (clean) {
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
			String version = artifact.getConfiguration().getPlatformVersions().get(targetPlatform.name());
			version = version == null || version.trim().isEmpty() ? "" : "@" + version;
			logger.info("Adding platform " + targetPlatform.getCordovaName() + version);
			exec(
				project.getAbsolutePath(),
				new String [] { cordovaPath + "cordova", "platform", "add", targetPlatform.getCordovaName() + version },
				null,
				properties,
				outputLog,
				errorLog,
				true
			);
			// TODO: clean plugins every time (it will NOT override update variable properties)
			// same for the plugins
			List<CordovaPlugin> plugins = new ArrayList<CordovaPlugin>();

			if (artifact.getConfiguration().getPlugins() != null) {
				plugins.addAll(artifact.getConfiguration().getPlugins());
			}
			boolean hasSplash = false;
			boolean hasFile = false;
			for (CordovaPlugin plugin : plugins) {
				if ("cordova-plugin-file".equals(plugin.getConfiguration().getName())) {
					hasFile = true;
				}
				else if ("cordova-plugin-splashscreen".equals(plugin.getConfiguration().getName())) {
					hasSplash = true;
				}
			}
//			if (!hasSplash) {
//				// always add the splash plugin
//				// could limit this to only when you have splash images...
//				CordovaPlugin splashPlugin = new CordovaPlugin(artifact.getId(), artifact.getDirectory(), artifact.getRepository());
//				splashPlugin.getConfiguration().setName("cordova-plugin-splashscreen");
//				splashPlugin.getConfiguration().setVersion("3.2.2");
//				plugins.add(splashPlugin);
//			}
			if (!hasFile) {
				// it seems that we _always_ need the cordova-plugin-file plugin? when installing without it (on vm at least) it threw an exception that it could not access the /.../www/index.html file
				CordovaPlugin filePlugin = new CordovaPlugin(artifact.getId(), artifact.getDirectory(), artifact.getRepository());
				filePlugin.getConfiguration().setName("cordova-plugin-file");
				plugins.add(filePlugin);
			}
			
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
		
			logger.info("Cleaning the www directory");
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
			WebApplication application = artifact.getConfiguration().getApplication();

			// run all the scripts
			ServiceMethodProvider serviceMethodProvider = new ServiceMethodProvider(artifact.getRepository(), artifact.getRepository(), artifact.getRepository().getServiceRunner());
//			GlueParserProvider parserProvider = new GlueParserProvider(serviceMethodProvider);
			GlueParserProvider parserProvider = new GlueWebParserProvider(serviceMethodProvider, new StaticJavaMethodProvider(new WebApplicationMethods(application)));
			
			// add the repository for this artifact
			addRepository(repository, artifact.getConfiguration().getApplication().getDirectory(), parserProvider);
			// add repository of web fragments
			addRepository(repository, artifact.getConfiguration().getApplication().getConfiguration().getWebFragments(), parserProvider);
			
			logger.info("Copying pages...");
			String hostName = artifact.getConfig().getHost() == null ? application.getConfiguration().getVirtualHost().getConfiguration().getHost() : artifact.getConfig().getHost();
			Integer port = artifact.getConfig().getPort() == null ? application.getConfiguration().getVirtualHost().getServer().getConfiguration().getPort() : artifact.getConfig().getPort();
			boolean secure = artifact.getConfig().getSecure() == null ? application.getConfiguration().getVirtualHost().getServer().isSecure() : artifact.getConfig().getSecure();
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
			environment.put("platform", targetPlatform.getCordovaName());
			if (version != null && !version.isEmpty()) {
				environment.put("platformVersion", version);
			}
			// nope, not doing this! we use server.root
			// we want a fully qualified host, with the subpath included if relevant
//			if (!"/".equals(application.getServerPath())) {
//				host += "/" + application.getServerPath().replaceFirst("^/", "");
//				// because this will serve as the base, it must end with a "/"
//				if (!host.endsWith("/")) {
//					host += "/";
//				}
//			}
			environment.put("realm", application.getRealm());
			environment.put("web", "false");
			environment.put("url", host);
			environment.put("host", hostName);
			environment.put("hostName", artifact.getConfig().getHost() == null ? application.getConfiguration().getVirtualHost().getConfiguration().getHost() : artifact.getConfig().getHost());
			environment.put("webApplicationId", application.getId());
			environment.put("secure", Boolean.toString(secure));
			if (artifact.getConfig().getLanguage() != null) {
				StringBuilder builder = new StringBuilder();
				String defaultLanguage = null;
				for (String language : artifact.getConfig().getLanguage()) {
					if (!builder.toString().isEmpty()) {
						builder.append(",");
					}
					builder.append(language);
					if (defaultLanguage == null) {
						defaultLanguage = language;
					}
				}
				if (defaultLanguage != null) {
					environment.put("defaultLanguage", defaultLanguage);
					environment.put("availableLanguages", builder.toString());
				}
			}
			List<String> languages = new ArrayList<String>();
			String defaultLanguage = null;
			if (artifact.getConfig().getLanguage() != null) {
				languages.addAll(artifact.getConfig().getLanguage());
				if (languages.size() > 0) {
					defaultLanguage = languages.get(0);
				}
			}
			for (Script script : repository) {
				// only copy public scripts?
				if (GlueListener.isPublicScript(script)) {
					// these are glue REST services, ignore them
					if (script.getRoot().getContext().getAnnotations().containsKey("path")) {
						continue;
					}
					if (!script.getRoot().getContext().getAnnotations().containsKey("cordova")) {
						continue;
					}
					if (languages.size() > 0) {
						for (String language : languages) {
							renderFile(application, script, wwwDirectory, environment, language, defaultLanguage);	
						}
					}
					else {
						renderFile(application, script, wwwDirectory, environment, null, null);
					}
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
			// refactor to use structural xml...
			String config = new String(bytes, "UTF-8");
			// remove current option for fullscreen
			config = config.replaceAll("(?s)<preference name=\"Fullscreen\"[^>]+/>", "");
			// add new option
			config = config.replaceAll("(?s)(</widget>)", "\t<preference name=\"Fullscreen\" value=\"" + (artifact.getConfiguration().getFullscreen() != null && artifact.getConfiguration().getFullscreen()) + "\" />\n$1");
			// remove current option for orientation
			config = config.replaceAll("(?s)<preference name=\"Orientation\"[^>]+/>", "");
			// add new option
			config = config.replaceAll("(?s)(</widget>)", "\t<preference name=\"Orientation\" value=\"" + (artifact.getConfiguration().getOrientation() != null ? artifact.getConfiguration().getOrientation().getCordovaName() : Orientation.BOTH.getCordovaName()) + "\" />\n$1");
			// add option for disallow overscroll
			if (artifact.getConfiguration().getDisableOverscroll() != null && artifact.getConfiguration().getDisableOverscroll()) {
				config = config.replaceAll("(?s)(</widget>)", "\t<preference name=\"DisallowOverscroll\" value=\"true\" />\n$1");
			}
			// update version if set
			if (artifact.getConfiguration().getVersion() != null) {
				config = config.replaceAll("(?s)(<widget[^>]+version[\\s]*=[\\s]*)(?:'|\")[^'\"]+(?:'|\")", "$1\"" + artifact.getConfiguration().getVersion() + "\"");
			}
			if (artifact.getConfiguration().getBuild() != null && targetPlatform.getBuildAttribute() != null) {
				config = config.replaceAll("(?s)<widget", "<widget " + targetPlatform.getBuildAttribute() + "=\"" + artifact.getConfiguration().getBuild() + "\"");
			}
			
			// if the target is not secure, we need to explicitly allow plain text connections since android 9, otherwise you will get "net::ERR_CLEARTEXT_NOT_PERMITTED"
			if (!secure) {
				// the android namespace is not defined by default it seems
				config = config.replace("<widget", "<widget xmlns:android=\"http://schemas.android.com/apk/res/android\"");
				config = config.replaceAll("(<platform[^>]+android[^>]+>)", "$1<access origin=\"*\" /><edit-config file=\"app/src/main/AndroidManifest.xml\" mode=\"merge\" target=\"/manifest/application\">\n" + 
						"      <application android:usesCleartextTraffic=\"true\" />\n" + 
						"  </edit-config><allow-navigation href=\"*\" />");
			}
			
			// add images if required
			config = addImages(config, artifact, wwwDirectory, targetPlatform);
			
			WritableContainer<ByteBuffer> writable = configurationChild.getWritable();
			try {
				writable.write(IOUtils.wrap(config.getBytes("UTF-8"), true));
			}
			finally {
				writable.close();
			}
			
			// for android we need signatures based on the configured keystore
			if (Platform.ANDROID.equals(targetPlatform)) {
				if (artifact.getConfiguration().getKeystore() == null || artifact.getConfiguration().getSignatureAlias() == null) {
					MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Please configure a keystore and alias that can be used to sign the apk"));
				}
				else {
					// TODO: always overwrite the keystore otherwise changes are not picked up (e.g. if you updated the password!)
					File keystore = new File(project, "keystore.jks");
					if (!keystore.exists()) {
						try {
							OutputStream output = null;
							try {
								output = new BufferedOutputStream(new FileOutputStream(keystore)); 
								new KeyStoreHandler(artifact.getConfiguration().getKeystore().getKeyStore().getKeyStore()).save(output, artifact.getConfig().getKeystore().getKeyStore().getPassword());
							}
							finally {
								if (output != null) {
									output.close();
								}
							}
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
//						artifact.getConfiguration().getKeystore().getKeyStore().save(new FileItem(null, keystore, false));
					}
					// now run it
					List<String> commands = new ArrayList<String>(Arrays.asList(new String [] { 
						cordovaPath + "cordova", 
						"run", 
						targetPlatform.getCordovaName(),
//						"--release",
						"--",
						"--keystore=keystore.jks", 
						"--storePassword=" + artifact.getConfiguration().getKeystore().getConfiguration().getPassword(),
						"--alias=" + artifact.getConfiguration().getSignatureAlias(),
						"--password=" + artifact.getConfiguration().getKeystore().getConfiguration().getKeyPasswords().get(artifact.getConfiguration().getSignatureAlias())
					}));
					if (release) {
						commands.add(3, "--release");
					}
					StringBuilder builder = new StringBuilder();
					for (String command : commands) {
						if (!builder.toString().isEmpty()) {
							builder.append(" ");
						}
						builder.append(command);
					}
					logger.info("Running ANDROID in " + project.getAbsolutePath() + ": " + builder.toString());
					exec(
						project.getAbsolutePath(),
						commands.toArray(new String[commands.size()]),
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
					new String [] { cordovaPath + "cordova", "run", targetPlatform.getCordovaName() },
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
	
	// type is "icon" or "splash"
	private String addImages(String config, CordovaApplication artifact, ResourceContainer<?> www, Platform platform) throws IOException {
		for (ImageType type : ImageType.values()) {
			Resource resolved = ResourceUtils.resolve(artifact.getDirectory(), EAIResourceRepository.PRIVATE + "/" + type.name().toLowerCase() + "/" + platform.name() + ".png");
			if (resolved != null) {
				ReadableContainer<ByteBuffer> readable = ((ReadableResource) resolved).getReadable();
				BufferedImage image;
				try {
					image = ImageIO.read(IOUtils.toInputStream(readable));
				}
				finally {
					readable.close();
				}
				Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/png");
				if (!writers.hasNext()) {
					throw new IllegalArgumentException("No handler for the content type: image/png");
				}
				ImageWriter writer = writers.next();
				String replacement = "\t<platform name=\"" + platform.getCordovaName() + "\">\n";
				for (Density density : platform.getDensities()) {
					for (Dimension dimension : density.getDimensions(type)) {
						// find biggest division
						byte[] bytes = resize(image, writer, dimension);
						String fileName = "resources/platform/" + type.name().toLowerCase() + "/" + platform.getCordovaName() + "." + dimension.getWidth() + "x" + dimension.getHeight() + ".png";
						Resource touch = ResourceUtils.touch(www, fileName);
						WritableContainer<ByteBuffer> writable = ((WritableResource) touch).getWritable();
						try {
							writable.write(IOUtils.wrap(bytes, true));
						}
						finally {
							writable.close();
						}
						String name = "";
						if (dimension.isLandscape()) {
							name = "land-";
						}
						else if (dimension.isPortrait()) {
							name = "port-";
						}
						name += density.name().toLowerCase();
						if (platform.useFullPath()) {
							fileName = "www/" + fileName;
						}
						replacement += "\t\t<" + type.name().toLowerCase() + " src=\"" + fileName + "\" density=\"" + name + "\" width=\"" + dimension.getWidth() + "\" height=\"" + dimension.getHeight() + "\"/>\n";
					}
				}
				replacement += "\t</platform>\n";
				config = config.replaceAll("(?s)(</widget>)", replacement + "$1");
			}
		}
		return config;
	}

	private byte[] resize(BufferedImage image, ImageWriter writer, Dimension dimension) throws IOException {
		double factor = Math.max((double) image.getWidth() / (double) dimension.getWidth(), (double) image.getHeight() / (double) dimension.getHeight());
		double targetWidth = (double) image.getWidth() / factor;
		double targetHeight = (double) image.getHeight() / factor;
		BufferedImage resizedImage = new BufferedImage((int) targetWidth, (int) targetHeight, image.getType());
		Graphics2D graphics = resizedImage.createGraphics();
		graphics.drawImage(image, 0, 0, (int) targetWidth, (int) targetHeight, null);
		graphics.dispose();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output);
		writer.setOutput(imageOutput);
		writer.write(resizedImage);
		return output.toByteArray();
	}
	
	private final class ImageDeletionHandler implements EventHandler<Event> {
		
		private ResourceContainer<?> folder;
		private String name;

		public ImageDeletionHandler(ResourceContainer<?> folder, String name) {
			this.folder = folder;
			this.name = name;
		}

		@Override
		public void handle(Event arg0) {
			Resource child = folder.getChild(name);
			if (child != null) {
				try {
					((ManageableContainer<?>) folder).delete(child.getName());
					MainController.getInstance().setChanged();
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private final class ImageHandlerImplementation implements ImageHandler {
		
		private ResourceContainer<?> folder;
		private String name;
		private String contentType;

		public ImageHandlerImplementation(ResourceContainer<?> folder, String name, String contentType) {
			this.folder = folder;
			this.name = name;
			this.contentType = contentType;
		}

		@Override
		public void handle(byte[] bytes) {
			try {
				if (bytes != null) {
					Resource target = folder.getChild(name);
					if (target == null) {
						target = ((ManageableContainer<?>) folder).create(name, contentType);
					}
					WritableContainer<ByteBuffer> writable = ((WritableResource) target).getWritable();
					try {
						writable.write(IOUtils.wrap(bytes, true));
						MainController.getInstance().setChanged();
					}
					finally {
						writable.close();
					}
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static interface ImageHandler {
		public void handle(byte[] bytes);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void getImage(String title, final ImageHandler handler) {
		Set properties = new LinkedHashSet(Arrays.asList(new Property [] {
			new SimpleProperty<byte[]>("Content", byte[].class, true)
		}));
		final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
		EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, title, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				byte [] content = updater.getValue("Content");
				handler.handle(content);
			}
		});
	}
	
	@SuppressWarnings("unused")
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
	private HBox statusBox;
	
	private void exec(String directory, String [] commands, List<byte[]> inputContents, List<SystemProperty> systemProperties, final TextArea outputTarget, final TextArea errorTarget, boolean waitFor) throws IOException, InterruptedException {
		javafx.application.Platform.runLater(new Runnable() {
			@Override
			public void run() {
				statusBox.getChildren().clear();
				statusBox.getChildren().add(MainController.loadGraphic("status/running.png"));
			}
		});
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
		logger.info("Running command: " + Arrays.asList(commands).toString().replace(",", "").replaceAll("^\\[", "").replaceAll("\\]$", ""));
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
		errorThread = new Thread(new InputCopier(process.getErrorStream(), errorTarget, null));
		errorThread.start();
		
		outputThread = new Thread(new InputCopier(process.getInputStream(), outputTarget, process));
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
		private Process process;

		private InputCopier(InputStream input, TextArea target, Process process) {
			this.input = input;
			this.target = target;
			this.process = process;
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
				if (process != null) {
					javafx.application.Platform.runLater(new Runnable() {
						@Override
						public void run() {
							statusBox.getChildren().clear();
							if (process.exitValue() == 0) {
								statusBox.getChildren().add(MainController.loadGraphic("status/success.png"));
							}
							else {
								statusBox.getChildren().add(MainController.loadGraphic("status/failed.png"));
							}
						}
					});
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void renderFile(WebApplication application, Script script, FileDirectory wwwDirectory, Map<String, String> environment, final String language, final String defaultLanguage) throws IOException {
		environment.put("currentLanguage", language);
		ScriptRuntime runtime = new ScriptRuntime(script, new SimpleExecutionEnvironment("local", environment), false, new HashMap<String, Object>());
		StringWriter writer = new StringWriter();
		SimpleOutputFormatter outputFormatter = new SimpleOutputFormatter(writer, false, false);
		runtime.setFormatter(outputFormatter);
		
		// copy paste from web application...
		final String additional;
		final String key;
		final Map<String, ComplexContent> translatorValues = application.getInputValues(application.getConfig().getTranslationService(), WebApplication.getMethod(Translator.class, "translate"));
		if (translatorValues != null && translatorValues.size() > 0) {
			if (translatorValues.size() > 1) {
				throw new RuntimeException("Translation services can only have one extended field");
			}
			key = translatorValues.keySet().iterator().next();
			JSONBinding binding = new JSONBinding(translatorValues.get(key).getType());
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			binding.marshal(output, translatorValues.get(key));
			additional = new String(output.toByteArray(), Charset.forName("UTF-8")).replace("'", "\\'");
		}
		else {
			additional = null;
			key = null;
		}
		StringSubstituterProvider languageSubstituter = new StringSubstituterProvider() {
			@Override
			public StringSubstituter getSubstituter(ScriptRuntime runtime) {
				if (additional != null) {
					// in the olden days instead of null as default category, we passed in: \"page:" + ScriptUtils.getFullName(runtime.getScript()) + "\")
					// however, because of concatting and possible other processing, the runtime script name rarely has a relation to the context anymore
					// it is clearer to work without a context then allowing for cross-context translations
					return new be.nabu.glue.impl.ImperativeSubstitutor("%", "script.template(" + application.getConfig().getTranslationService().getId() 
							+ "(control.when(\"${value}\" ~ \"^[a-zA-Z0-9.]+:.*\", string.replace(\"^([a-zA-Z0-9.]+):.*\", \"$1\", \"${value}\"), null), "
							+ "control.when(\"${value}\" ~ \"^[a-zA-Z0-9.]+:.*\", string.replace(\"^[a-zA-Z0-9.]+:(.*)\", \"$1\", \"${value}\"), \"${value}\"), " + (language == null ? "null" : "\"" + language + "\"")
							+ ", " + key + ": json.objectify('" + additional + "'))/translation)");
				}
				else {
					return new be.nabu.glue.impl.ImperativeSubstitutor("%", "script.template(" + application.getConfig().getTranslationService().getId() 
							+ "(control.when(\"${value}\" ~ \"^[a-zA-Z0-9.]+:.*\", string.replace(\"^([a-zA-Z0-9.]+):.*\", \"$1\", \"${value}\"), null), "
							+ "control.when(\"${value}\" ~ \"^[a-zA-Z0-9.]+:.*\", string.replace(\"^[a-zA-Z0-9.]+:(.*)\", \"$1\", \"${value}\"), \"${value}\"), " + (language == null ? "null" : "\"" + language + "\"") + ")/translation)");
				}
			}
		};
		runtime.addSubstituterProviders(Arrays.asList(languageSubstituter));
		// make sure we have the correct root path
		runtime.getContext().put(ServerMethods.ROOT_PATH, application.getServerPath());
		runtime.run();
		List<Header> headers = (List<Header>) runtime.getContext().get(ResponseMethods.RESPONSE_HEADERS);
		String path = ScriptUtils.getFullName(script).replace(".", "/") + (script.getName().equals("index") ? ".html" : "");
		// make sure we have the correct extensions for the pages
		if (headers != null) {
			String contentType = MimeUtils.getContentType(headers.toArray(new Header[0]));
			if (contentType.equalsIgnoreCase("application/javascript")) {
				if (!path.endsWith(".js")) {
					path += ".js";
				}
			}
			else if (contentType.equalsIgnoreCase("text/css")) {
				if (!path.endsWith(".css")) {
					path += ".css";
				}
			}
		}
		if (language != null && !language.equals(defaultLanguage)) {
			path = path.replaceAll("(\\.[^.]+$)", "-" + language + "$1");
		}
		Resource file = ResourceUtils.touch(wwwDirectory, path);
		WritableContainer<ByteBuffer> writable = ((WritableResource) file).getWritable();
		try {
			String string = writer.toString();
			// make sure we add the correct extension, otherwise it might not get picked up
			string = string.replaceAll("(<script[^>]+src=\".*?)\\?[^\"]+", "$1");
			// we don't "need" to have the javascript end in .js (yet), but for css it is required
			string = string.replaceAll("(<script[^>]+src=\"[^\"]+)", "$1.js").replaceAll("(<script[^>]+src=\"[^\"]+)\\.js\\.js", "$1.js");
			// we can't have absolute references as they won't get picked up correctly from the file system
			string = string.replaceAll("(<script[^>]+src=\")/", "$1");
			// remove the query parameters, it doesn't really matter much but still...
			string = string.replaceAll("(<link[^>]+href=\".*?)\\?[^\"]+", "$1");
			// the css files must end in ".css" or they won't get picked up
			string = string.replaceAll("(<link[^>]+href=\"[^\"]+)", "$1.css").replaceAll("(<link[^>]+href=\"[^\"]+)\\.css\\.css", "$1.css");
			// we can't have absolute references as they won't get picked up correctly from the file system
			string = string.replaceAll("(<link[^>]+href=\")/", "$1");
			
			// any local include has to be differentiated for language
			if (language != null && !language.equals(defaultLanguage)) {
				string = string.replaceAll("(<script[^>]+src=\"(?!cordova\\.js)[^/\"]+[^\"]*)\\.js", "$1-" + language + ".js");
				string = string.replaceAll("(<link[^>]+href=\"[^/\"]+[^\"]*)\\.css", "$1-" + language + ".css");
			}
			writable.write(IOUtils.wrap(string.getBytes(), true));
		}
		finally {
			writable.close();
		}
	}
}
