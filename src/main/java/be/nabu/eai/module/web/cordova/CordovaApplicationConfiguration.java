package be.nabu.eai.module.web.cordova;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.cordova.plugin.CordovaPlugin;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;

@XmlRootElement(name = "cordovaApplication")
@XmlType(propOrder = {"namespace", "name", "title", "version", "host", "port", "secure", "language", "build", "platforms", "platformVersions", "plugins", "application", "keystore", "signatureAlias", "fullscreen", "orientation", "disableOverscroll" })
public class CordovaApplicationConfiguration {
	
	private String name, namespace, title, version, build;
	
	private List<Platform> platforms;
	
	private List<CordovaPlugin> plugins;
	
	private WebApplication application;
	
	private KeyStoreArtifact keystore;
	private String signatureAlias;
	private Boolean fullscreen;
	private Orientation orientation;
	
	// if you want to use a different host from the one configured in the virtual host (e.g. cause you're building from dev), you can configure it here
	private String host;
	private Integer port;
	private Boolean secure;
	
	// the default language to render
	private List<String> language;
	
	private Map<String, String> platformVersions;
	
	private Boolean disableOverscroll;

	public Boolean getSecure() {
		return secure;
	}
	public void setSecure(Boolean secure) {
		this.secure = secure;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	@NotNull
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@NotNull
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@NotNull
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getBuild() {
		return build;
	}
	public void setBuild(String build) {
		this.build = build;
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
	
	@NotNull
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeystore() {
		return keystore;
	}
	public void setKeystore(KeyStoreArtifact keystore) {
		this.keystore = keystore;
	}
	
	@NotNull
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
	public List<String> getLanguage() {
		return language;
	}
	public void setLanguage(List<String> language) {
		this.language = language;
	}
	public Boolean getDisableOverscroll() {
		return disableOverscroll;
	}
	public void setDisableOverscroll(Boolean disableOverscroll) {
		this.disableOverscroll = disableOverscroll;
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
	
	public enum ImageType {
		ICON,
		SPLASH
	}
	
	public static class Dimension {
		private int width, height;
		private ImageType type;
		public Dimension(int width, int height, ImageType type) {
			this.width = width;
			this.height = height;
			this.type = type;
		}
		public int getWidth() {
			return width;
		}
		public int getHeight() {
			return height;
		}
		public ImageType getType() {
			return type;
		}
		public Dimension invert() {
			return new Dimension(height, width, type);
		}
		public boolean isLandscape() {
			return width > height;
		}
		public boolean isPortrait() {
			return height > width;
		}
	}
	
	public enum Density {
		LDPI(new Dimension(36, 36, ImageType.ICON), new Dimension(320, 200, ImageType.SPLASH)),
		MDPI(new Dimension(48, 48, ImageType.ICON), new Dimension(480, 320, ImageType.SPLASH)), 
		HDPI(new Dimension(72, 72, ImageType.ICON), new Dimension(800, 480, ImageType.SPLASH)),
		XHDPI(new Dimension(96, 96, ImageType.ICON), new Dimension(1280, 720, ImageType.SPLASH)),
		XXHDPI(new Dimension(144, 144, ImageType.ICON), new Dimension(1600, 960, ImageType.SPLASH)),
		XXXHDPI(new Dimension(192, 192, ImageType.ICON), new Dimension(1920, 1280, ImageType.SPLASH)),
		// currently not supported as of cordova 6.1.1
		XXXXHDPI(new Dimension(384, 384, ImageType.ICON), new Dimension(2560, 1600, ImageType.SPLASH)),

		// https://developer.apple.com/library/ios/documentation/UserExperience/Conceptual/MobileHIG/IconMatrix.html
		IPHONE4(new Dimension(57, 57, ImageType.ICON), new Dimension(114, 114, ImageType.ICON), new Dimension(480, 320, ImageType.SPLASH)),
		IPHONE4_RETINA(new Dimension(114, 114, ImageType.ICON), new Dimension(960, 640, ImageType.SPLASH)),
		IPHONE5(new Dimension(60, 60, ImageType.ICON), new Dimension(120, 120, ImageType.ICON), new Dimension(1136, 640, ImageType.SPLASH)),
		IPHONE6(new Dimension(180, 180, ImageType.ICON), new Dimension(2208, 1242, ImageType.SPLASH), new Dimension(1334, 750, ImageType.SPLASH)),
		IPAD(new Dimension(76, 76, ImageType.ICON), new Dimension(72, 72, ImageType.ICON), new Dimension(152, 152, ImageType.ICON), new Dimension(144, 144, ImageType.ICON), new Dimension(1024, 768, ImageType.SPLASH)),
		IPHONE_SPOTLIGHT(new Dimension(29, 29, ImageType.ICON), new Dimension(58, 58, ImageType.ICON)),
		IPAD_SPOTLIGHT(new Dimension(50, 50, ImageType.ICON), new Dimension(100, 100, ImageType.ICON)),
		;
		
		private Dimension[] dimensions;
		
		private Density(Dimension...dimensions) {
			this.dimensions = dimensions;
		}
		public Dimension[] getDimensions() {
			return dimensions;
		}
		public List<Dimension> getDimensions(ImageType type) {
			List<Dimension> dimensions = new ArrayList<Dimension>();
			for (Dimension dimension : this.dimensions) {
				if (dimension.getType() == type) {
					dimensions.add(dimension);
					if (dimension.getHeight() != dimension.getWidth()) {
						dimensions.add(dimension.invert());
					}
				}
			}
			return dimensions;
		}
	}
	
	public enum Platform {
		ANDROID("android", true, "android-versionCode", Density.LDPI, Density.MDPI, Density.HDPI, Density.XHDPI, Density.XXHDPI, Density.XXXHDPI),
		IOS("ios", true, "ios-CFBundleVersion", Density.IPHONE4, Density.IPHONE4_RETINA, Density.IPHONE5, Density.IPHONE6, Density.IPAD, Density.IPHONE_SPOTLIGHT, Density.IPAD_SPOTLIGHT),
		WINDOWS("windows", false, "windows-packageVersion"),
		BLACKBERRY10("blackberry10", false, null),
		FIRE_OS("fireos", false, null),
		FIREFOX_OS("firefoxos", false, null),
		UBUNTU("ubuntu", false, null),
		WEB_OS("webos", false, null),
		TIZEN("tizen", false, null);
		
		private String cordovaName;
		private Density[] densities;
		private boolean useFullPath;
		private String buildAttribute;

		private Platform(String cordovaName, boolean useFullPath, String buildAttribute, Density...dimensions) {
			this.cordovaName = cordovaName;
			this.useFullPath = useFullPath;
			this.buildAttribute = buildAttribute;
			this.densities = dimensions;
		}

		public String getBuildAttribute() {
			return buildAttribute;
		}

		public String getCordovaName() {
			return cordovaName;
		}

		public Density[] getDensities() {
			return densities;
		}
		
		public boolean useFullPath() {
			return useFullPath;
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
