package be.nabu.eai.module.web.cordova;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.cordova.plugin.CordovaPlugin;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;

@XmlRootElement(name = "cordovaApplication")
@XmlType(propOrder = {"namespace", "name", "title", "platforms", "platformVersions", "plugins", "application", "keystore", "signatureAlias", "fullscreen", "orientation" })
public class CordovaApplicationConfiguration {
	
	private String name, namespace, title;
	
	private List<Platform> platforms;
	
	private List<CordovaPlugin> plugins;
	
	private WebApplication application;
	
	private KeyStoreArtifact keystore;
	private String signatureAlias;
	private Boolean fullscreen;
	private Orientation orientation;
	
	private Map<String, String> platformVersions;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public List<Platform> getPlatforms() {
		return platforms;
	}
	public void setPlatforms(List<Platform> platforms) {
		this.platforms = platforms;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<CordovaPlugin> getPlugins() {
		return plugins;
	}
	public void setPlugins(List<CordovaPlugin> plugins) {
		this.plugins = plugins;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeystore() {
		return keystore;
	}
	public void setKeystore(KeyStoreArtifact keystore) {
		this.keystore = keystore;
	}
	
	public String getSignatureAlias() {
		return signatureAlias;
	}
	public void setSignatureAlias(String signatureAlias) {
		this.signatureAlias = signatureAlias;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public WebApplication getApplication() {
		return application;
	}
	public void setApplication(WebApplication application) {
		this.application = application;
	}
	
	public Boolean getFullscreen() {
		return fullscreen;
	}
	public void setFullscreen(Boolean fullscreen) {
		this.fullscreen = fullscreen;
	}
	public Orientation getOrientation() {
		return orientation;
	}
	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}
	
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getPlatformVersions() {
		if (platformVersions == null) {
			platformVersions = new HashMap<String, String>();
		}
		return platformVersions;
	}
	public void setPlatformVersions(Map<String, String> platformVersions) {
		this.platformVersions = platformVersions;
	}

	public enum Platform {
		ANDROID("android"),
		IOS("ios"),
		WINDOWS("windows"),
		BLACKBERRY10("blackberry10"),
		FIRE_OS("fireos"),
		FIREFOX_OS("firefoxos"),
		UBUNTU("ubuntu"),
		WEB_OS("webos"),
		TIZEN("tizen");
		
		private String cordovaName;

		private Platform(String cordovaName) {
			this.cordovaName = cordovaName;
		}

		public String getCordovaName() {
			return cordovaName;
		}
	}

	public enum Orientation {
		BOTH("default"),
		PORTRAIT("portrait"),
		LANDSCAPE("landscape");
		
		private String name;

		private Orientation(String name) {
			this.name = name;
		}

		public String getCordovaName() {
			return name;
		}
	}
}
