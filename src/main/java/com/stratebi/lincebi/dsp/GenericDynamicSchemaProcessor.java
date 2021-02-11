package com.stratebi.lincebi.dsp;

import java.io.InputStream;
import java.util.List;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;

import mondrian.olap.Util.PropertyList;

public class GenericDynamicSchemaProcessor extends BaseDynamicSchemaProcessor {

	@Override
	public String filter(String schemaUrl, PropertyList connectInfo, InputStream stream) throws Exception {
		IPentahoSession session = PentahoSessionHolder.getSession();
		String user = session.getName();
		this.addVar("USER", opts -> user);

		IUserRoleListService roleListService = PentahoSystem.get(IUserRoleListService.class);
		List<String> roles = roleListService.getRolesForUser(null, user);
		this.addVar("ROLES", opts -> roles);

		String lang = LocaleHelper.getLocale().getLanguage();
		this.addVar("LANG", opts -> lang);

		return super.filter(schemaUrl, connectInfo, stream);
	}

}
