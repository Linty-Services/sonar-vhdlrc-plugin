
package com.linty.sonar.plugins.vhdlrc.rules;


import java.io.File;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.annotations.VisibleForTesting;
import com.linty.sonar.plugins.vhdlrc.Vhdl;

import org.apache.commons.io.FilenameUtils;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;


@ServerSide
public class VhdlRulesDefinition implements RulesDefinition {

	
	public static final String HANDBOOK_PATH_KEY = "sonar.vhdlrc.handbook.path";
    private static final String DEFAULT_HANDBOOK_PATH = "rulechecker/default/VHDL_Handbook_STD-master" ;
    		/*TODO : set an internal default handbook "com/linty/sonar/plugins/vhdlrc/rules/conf/Default_handbook"
    		 Make a build in Quality Profile with the embbedded Handbook, in addition of the one created here. Internal HANDBOOk is always loaded.
    		 */
    public static final String HANDBOOK_PATH_DESC = "Path to the handbook directory. The path may be absolute or relative to the SonarQube server base directory.";
	
    private static final String RULE_SETS_PATH = "Rulesets";

    
    private final ServerFileSystem serverFileSystem;
    private final Configuration configuration;
    
	private static final Logger LOG = Loggers.get(VhdlRulesDefinition.class);
	
	private static final Map<String,Severity> SEVERITY_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private static final Map<String,RuleType> TYPE_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private static final Map<String,String> DEBT_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			static {
				SEVERITY_MAP.put("BLOCKER",  Severity.BLOCKER);
				SEVERITY_MAP.put("CRITICAL", Severity.CRITICAL);
				SEVERITY_MAP.put("MAJOR",    Severity.MAJOR);
				SEVERITY_MAP.put("MINOR",    Severity.MINOR);//Default
				SEVERITY_MAP.put("INFO",     Severity.INFO);
				
				TYPE_MAP.put("BUG",           RuleType.BUG);
				TYPE_MAP.put("VULNERABILITY", RuleType.VULNERABILITY);
				TYPE_MAP.put("CODE_SMELL",    RuleType.CODE_SMELL);//Default
				
				DEBT_MAP.put("Trivial", "5min");
				DEBT_MAP.put("Easy"   , "10min");//Default
				DEBT_MAP.put("Medium" , "20min");
				DEBT_MAP.put("Major"  , "1h");
				DEBT_MAP.put("High"   , "3h");
				DEBT_MAP.put("Complex", "1d");
			}
			
	
	public VhdlRulesDefinition (Configuration configuration, ServerFileSystem serverFileSystem) {
		this.configuration = configuration;
		this.serverFileSystem = serverFileSystem;
	}
	
	@Override
	public void define(Context context) {

		NewRepository repository = context
				.createRepository("vhdlrc-repository", Vhdl.KEY)
				.setName("VhdlRuleChecker");
		try {
			File hbDir = getHandbookDir();
			List<com.linty.sonar.plugins.vhdlrc.rules.Rule> rules = new HandbookXmlParser().parseXML(handbookXml(hbDir));
			if(rules == null) {
				LOG.warn("No VHDL RuleCheker rules loaded!");
			} else {
				new ExampleAndFigureLoader(hbDir.toPath()).load(rules);
				for(com.linty.sonar.plugins.vhdlrc.rules.Rule r : rules) {
					newRule(r,repository);
				}
				repository.done();	
			}
		}
		catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			LOG.error(e.getMessage());
		}
	}

	@VisibleForTesting
	protected void newRule(com.linty.sonar.plugins.vhdlrc.rules.Rule r, NewRepository repository) {
		NewRule nr = repository.createRule(r.ruleKey);
		nr.setHtmlDescription(r.buildHtmlDescritpion());
		addMetadataTo(nr,r);
	}

	private void addMetadataTo(NewRule nr, com.linty.sonar.plugins.vhdlrc.rules.Rule r) {
		nr
		.setInternalKey(r.ruleKey)
		.setName(r.name)
		.setSeverity(SEVERITY_MAP.getOrDefault(r.sonarSeverity, Severity.MINOR).toString())
		.setType(TYPE_MAP.getOrDefault(r.type, RuleType.CODE_SMELL))
		.setDebtRemediationFunction(nr.debtRemediationFunctions().constantPerIssue(DEBT_MAP.getOrDefault(r.remediationEffort,DEBT_MAP.get("easy"))))
		;
		addTags(nr,r.tag);
		addTags(nr,r.category);
		addTags(nr,r.subCategoty);
	}

	private void addTags(NewRule nr, String tag) {
		if(!tag.isEmpty() && !"tbd".equalsIgnoreCase(tag) ) {
			 nr.addTags(RuleTagFormat.isValid(tag) ? tag : tag.toLowerCase().replaceAll("[^a-z0-9\\\\+#\\\\-\\\\.]", "-"));
		}
	}

	private File getHandbookDir() throws FileNotFoundException {
		File handbookDir;	
		handbookDir = new File(FilenameUtils.separatorsToUnix(configuration.get(HANDBOOK_PATH_KEY).orElse(DEFAULT_HANDBOOK_PATH)));	
		if(!handbookDir.isAbsolute()) {
			handbookDir = new File(serverFileSystem.getHomeDir(),handbookDir.getPath());
		}		
		if(handbookDir.exists() && handbookDir.isDirectory()){
			return handbookDir;
		}
		else throw new FileNotFoundException("Handbook directory not found : " + handbookDir.getPath() +" ; Check parameter " + VhdlRulesDefinition.HANDBOOK_PATH_KEY);
	}

	private File handbookXml(File dir) throws FileNotFoundException {
		//File[] is used in case multiple handbooks must be handle
		File[] fileList = (new File(dir,RULE_SETS_PATH)).listFiles((directory,name) -> !name.contains("header") && name.matches(".*handbook.*\\.xml"));
        if(fileList != null && fileList.length != 0) {
        	return fileList[0];
        }
        else throw new FileNotFoundException("No handbook.xml found in : " + dir.getPath() + File.separator + RULE_SETS_PATH);
	}

	
}
