package com.stratebi.lincebi.dsp;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import mondrian.i18n.LocalizingDynamicSchemaProcessor;
import mondrian.olap.Util.PropertyList;
import mondrian.spi.DynamicSchemaProcessor;

public class BaseDynamicSchemaProcessor extends LocalizingDynamicSchemaProcessor implements DynamicSchemaProcessor {

	private static final String OPT_DSP_DEBUG = "DSP_DEBUG";

	private final Map<String, String> optMap = new HashMap<String, String>();
	private final Map<String, Function<Map<String, String>, Object>> varMap = new HashMap<String, Function<Map<String, String>, Object>>();

	@Override
	public String filter(String schemaUrl, PropertyList connectInfo, InputStream stream) throws Exception {
		String schema = super.filter(schemaUrl, connectInfo, stream);
		return this.postReplaceHook(this.replaceSchema(this.preReplaceHook(schema)));
	}

	public String preReplaceHook(String schema) {
		this.addOpt(OPT_DSP_DEBUG, "false");

		return schema;
	}

	public String postReplaceHook(String schema) {
		return schema;
	}

	public Map<String, String> getOpts() {
		return new HashMap<String, String>(this.optMap);
	}

	public String getOpt(String name) {
		return this.optMap.get(name);
	}

	public void addOpt(String name, String value) {
		this.optMap.put(name, value);
	}

	public void removeOpt(String name) {
		this.optMap.remove(name);
	}

	public Map<String, Function<Map<String, String>, Object>> getVars() {
		return new HashMap<String, Function<Map<String, String>, Object>>(this.varMap);
	}

	public Function<Map<String, String>, Object> getVar(String name) {
		return this.varMap.get(name);
	}

	public void addVar(String name, Function<Map<String, String>, Object> operator) {
		this.varMap.put(name, operator);
	}

	public void addVar(String name, Object value) {
		this.varMap.put(name, opts -> value);
	}

	public void removeVar(String name) {
		this.varMap.remove(name);
	}

	private String replaceSchema(String schema) {
		try {
			Map<String, String> schemaOptMap = this.getOpts();
			Map<String, Function<Map<String, String>, Object>> schemaVarMap = this.getVars();

			for (Map.Entry<String, String> opt : schemaOptMap.entrySet()) {
				String name = opt.getKey();
				Matcher matcher = schemaOptPattern(name).matcher(schema);
				boolean found = matcher.find() && matcher.groupCount() > 0;
				if (found) schemaOptMap.put(name, matcher.group(1));
			}

			boolean debug = schemaOptMap.getOrDefault(OPT_DSP_DEBUG, "").equalsIgnoreCase("true");

			for (Map.Entry<String, Function<Map<String, String>, Object>> var : schemaVarMap.entrySet()) {
				String name = var.getKey();
				Matcher matcher = schemaVarPattern(name).matcher(schema);
				boolean found = matcher.find();
				Object value;

				try {
					value = var.getValue().apply(schemaOptMap);
				} catch (Exception ex) {
					System.err.println("[DSP][ERROR] ${" + name + "} was not processed");
					ex.printStackTrace();
					continue;
				}

				if (debug) {
					System.out.println("[DSP][INFO] ${" + name + "} value: " + value);
					System.out.println("[DSP][INFO] ${" + name + "} found: " + found);
				}

				if (found && value != null) {
					if (value instanceof Collection) {
						Collection<Object> collection = new ArrayList<>((Collection<?>) value);
						schema = replaceSchemaCollection(matcher, collection);
					} else {
						schema = replaceSchemaObject(matcher, value);
					}
				}
			}

			if (debug) {
				System.out.println("[DSP][INFO] Replaced Schema:\n" + schema);
			}
		} catch (Exception ex) {
			System.err.println("[DSP][ERROR] Schema was not processed:\n" + schema);
			ex.printStackTrace();
		}

		return schema;
	}

	private String replaceSchemaObject(Matcher matcher, Object obj) {
		return matcher.replaceAll(sqlQuote(obj));
	}

	private String replaceSchemaCollection(Matcher matcher, Collection<Object> collection) {
		return matcher.replaceAll(
			String.join(",", collection.stream()
				.filter(obj -> obj != null)
				.map(obj -> sqlQuote(obj))
				.collect(Collectors.toList())
			)
		);
	}

	private static Pattern schemaOptPattern(String name) {
		return Pattern.compile(
			"\\<!\\[CDATA\\[\\s*" + name + "\\s*=\\s*(.*?)\\s*\\]\\]>",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
		);
	}

	private static Pattern schemaVarPattern(String name) {
		return Pattern.compile(
			"\\$\\{" + name + "\\}",
			Pattern.CASE_INSENSITIVE
		);
	}

	// This method does a very simple sanitization, it is NOT safe to use with untrusted data.
	private static String sqlQuote(Object obj) {
		return "'" + obj.toString().replaceAll("'", "''") + "'";
	}

}
