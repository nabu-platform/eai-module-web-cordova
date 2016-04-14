package be.nabu.eai.module.web.cordova.plugin;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cordovaPlugin")
public class CordovaPluginConfiguration {
	private String name;
	private List<String> variables;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getVariables() {
		return variables;
	}
	public void setVariables(List<String> variables) {
		this.variables = variables;
	}
}
