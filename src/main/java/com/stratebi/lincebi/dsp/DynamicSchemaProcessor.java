package com.stratebi.lincebi.dsp;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import mondrian.i18n.LocalizingDynamicSchemaProcessor;
import mondrian.olap.Util;

public class DynamicSchemaProcessor extends LocalizingDynamicSchemaProcessor {

	private static final String STR_QUOTE = "'";

	@Override
	public String filter(String schemaUrl, Util.PropertyList connectInfo, InputStream stream) throws Exception {
		String schema = super.filter(schemaUrl, connectInfo, stream);

		IPentahoSession session = PentahoSessionHolder.getSession();
		String user = session.getName();

		IUserRoleListService service = PentahoSystem.get(IUserRoleListService.class);
		List<String> roles = service.getRolesForUser(null, user);

		Boolean printToLogFound = Pattern.compile("\\[CDATA\\[(DSP_PRINT_TO_LOG=true)\\]\\]", Pattern.CASE_INSENSITIVE).matcher(schema).find();
		Boolean userPatternFound = false, rolesPatternFound = false;

		if (printToLogFound) {
			userPatternFound = Pattern.compile("\\$\\{USER\\}").matcher(schema).find();
			rolesPatternFound = Pattern.compile("\\$\\{ROLES\\}").matcher(schema).find();
		}

		try {
			schema = schema.replaceAll("\\$\\{USER\\}", STR_QUOTE + user + STR_QUOTE);
			schema = schema.replaceAll("\\$\\{ROLES\\}", STR_QUOTE + String.join(STR_QUOTE + "," + STR_QUOTE, roles) + STR_QUOTE);
		} catch (PatternSyntaxException pse) {
			System.out.println("[DSP] Error. Schema was not processed: " + schema);
			pse.printStackTrace();
		}

		if (printToLogFound) {
			System.out.println("[DSP] User session: " + user);
			System.out.println("[DSP] User pattern found: " + userPatternFound);
			System.out.println("[DSP] Roles session: " + String.join(", ", roles));
			System.out.println("[DSP] Roles pattern found: " + rolesPatternFound);
			System.out.println("[DSP] Replaced Schema: " + schema);
		}

		return schema;
	}

}
