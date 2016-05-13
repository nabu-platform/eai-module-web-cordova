package be.nabu.eai.module.web.cordova;

import java.util.ArrayList;
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
		XXXXHDPI(new Dimension(384, 384, ImageType.ICON), new Dimension(2560, 1600, ImageType.SPLASH)),

//		IPHONE6(180, 180, )
		
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
		ANDROID("android", true, Density.LDPI, Density.MDPI, Density.HDPI, Density.XHDPI, Density.XXHDPI, Density.XXXHDPI, Density.XXXXHDPI),
		IOS("ios", false),
		WINDOWS("windows", false),
		BLACKBERRY10("blackberry10", false),
		FIRE_OS("fireos", false),
		FIREFOX_OS("firefoxos", false),
		UBUNTU("ubuntu", false),
		WEB_OS("webos", false),
		TIZEN("tizen", false);
		
		private String cordovaName;
		private Density[] densities;
		private boolean useFullPath;

		private Platform(String cordovaName, boolean useFullPath, Density...dimensions) {
			this.cordovaName = cordovaName;
			this.useFullPath = useFullPath;
			this.densities = dimensions;
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
