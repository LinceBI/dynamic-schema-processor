package com.stratebi.lincebi.dsp;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;

import mondrian.i18n.LocalizingDynamicSchemaProcessor;
import mondrian.olap.Util;

public class DynamicSchemaProcessor extends LocalizingDynamicSchemaProcessor {

	public static final String PRINT_TO_LOG_OPT = "DSP_PRINT_TO_LOG";

	public static final String USER_VAR = "USER";
	public static final String ROLES_VAR = "ROLES";
	public static final String LANG_VAR = "LANG";

	public static final String SQL_STR_QUOTE = "'";

	@Override
	public String filter(String schemaUrl, Util.PropertyList connectInfo, InputStream stream) throws Exception {
		String schema = super.filter(schemaUrl, connectInfo, stream);

		try {
			Pattern printToLogOptPattern = schemaOptPattern(PRINT_TO_LOG_OPT);
			boolean printToLogOpt = matcherGroup(printToLogOptPattern.matcher(schema), 1).equalsIgnoreCase("true");

			Pattern userVarPattern = schemaVarPattern(USER_VAR);
			boolean userVarFound = userVarPattern.matcher(schema).find();

			Pattern rolesVarPattern = schemaVarPattern(ROLES_VAR);
			boolean rolesVarFound = rolesVarPattern.matcher(schema).find();

			Pattern langVarPattern = schemaVarPattern(LANG_VAR);
			boolean langVarFound = langVarPattern.matcher(schema).find();

			IPentahoSession session = PentahoSessionHolder.getSession();
			String user = session.getName();
			if (userVarFound) schema = replaceSchemaString(userVarPattern, user, schema);

			IUserRoleListService roleListService = PentahoSystem.get(IUserRoleListService.class);
			List<String> roles = roleListService.getRolesForUser(null, user);
			if (rolesVarFound) schema = replaceSchemaStringList(rolesVarPattern, roles, schema);

			String lang = LocaleHelper.getLocale().getLanguage();
			if (langVarFound) schema = replaceSchemaString(langVarPattern, lang, schema);

			if (printToLogOpt) {
				System.out.println("[DSP] ${" + USER_VAR + "} value: " + user);
				System.out.println("[DSP] ${" + USER_VAR + "} found: " + userVarFound);
				System.out.println("[DSP] ${" + ROLES_VAR + "} value: " + String.join(", ", roles));
				System.out.println("[DSP] ${" + ROLES_VAR + "} found: " + rolesVarFound);
				System.out.println("[DSP] ${" + LANG_VAR + "} value: " + lang);
				System.out.println("[DSP] ${" + LANG_VAR + "} found: " + langVarFound);
				System.out.println("[DSP] Replaced Schema:\n" + schema);
			}
		} catch (Exception ex) {
			System.err.println("[DSP] Error. Schema was not processed:\n" + schema);
			ex.printStackTrace();
		}

		return schema;
	}

	public static Pattern schemaOptPattern(String opt) {
		return Pattern.compile(
			"\\<!\\[CDATA\\[\\s*" + opt + "\\s*=\\s*(.*?)\\s*\\]\\]>",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
		);
	}

	public static Pattern schemaVarPattern(String var) {
		return Pattern.compile(
			"\\$\\{" + var + "\\}",
			Pattern.CASE_INSENSITIVE
		);
	}

	public static String replaceSchemaString(Pattern pattern, String str, String schema) {
		return pattern.matcher(schema).replaceAll(sqlQuoteString(str));
	}

	public static String replaceSchemaStringList(Pattern pattern, List<String> list, String schema) {
		return pattern.matcher(schema).replaceAll(
			String.join(",", list.stream()
				.filter(e -> e != null)
				.map(e -> sqlQuoteString(e))
				.collect(Collectors.toList())
			)
		);
	}

	public static String matcherGroup(Matcher matcher, int index) {
		return matcher.find() && matcher.groupCount() >= index
			? matcher.group(index)
			: "";
	}

	// This method does a very simple sanitization, it is NOT safe to use with untrusted data.
	public static String sqlQuoteString(String str) {
		return SQL_STR_QUOTE + str.replaceAll(SQL_STR_QUOTE, "") + SQL_STR_QUOTE;
	}

}
