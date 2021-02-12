package com.stratebi.lincebi.dsp;

import java.util.List;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;

import mondrian.spi.DynamicSchemaProcessor;

public class GenericDynamicSchemaProcessor extends BaseDynamicSchemaProcessor implements DynamicSchemaProcessor {

	@Override
	public String preReplaceHook(String schema) {
		schema = super.preReplaceHook(schema);

		IPentahoSession session = PentahoSessionHolder.getSession();
		String user = session.getName();
		this.addVar("USER", user);

		IUserRoleListService roleListService = PentahoSystem.get(IUserRoleListService.class);
		List<String> roles = roleListService.getRolesForUser(null, user);
		this.addVar("ROLES", roles);

		String lang = LocaleHelper.getLocale().getLanguage();
		this.addVar("LANG", lang);

		return schema;
	}

}
