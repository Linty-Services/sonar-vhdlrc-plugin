package com.linty.sonar.plugins.vhdlrc.rules;

import org.sonar.api.config.Configuration;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RulesDefinition;


@ServerSide
public class VhdlRulesDefinition implements RulesDefinition {

	
	public static final String HANDBOOK_PATH_KEY = "sonar.vhdlrc.handbook.path";
    public static final String DEFAULT_HANDBOOK_PATH = "rulechecker/default/VHDL_Handbook_STD-master";
    public static final String HANDBOOK_PATH_DESC = "Path to the handbook directory. The path may be absolute or relative to the SonarQube server base directory.";
	
    private final ServerFileSystem serverFileSystem;
    private final Configuration configuration;
	
	public VhdlRulesDefinition (Configuration configuration,ServerFileSystem serverFileSystem) {
		this.configuration = configuration;
		this.serverFileSystem = serverFileSystem;
	}
	
	@Override
	public void define(Context context) {
		
		
	}

}
