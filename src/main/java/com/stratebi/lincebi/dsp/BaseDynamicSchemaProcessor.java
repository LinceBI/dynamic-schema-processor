package com.stratebi.lincebi.dsp;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import mondrian.i18n.LocalizingDynamicSchemaProcessor;
import mondrian.olap.Util.PropertyList;
import mondrian.spi.DynamicSchemaProcessor;

public class BaseDynamicSchemaProcessor extends LocalizingDynamicSchemaProcessor implements DynamicSchemaProcessor {

	private static final String OPT_DSP_DEBUG = "DSP_DEBUG";

	private final Map<String, String> optMap = new HashMap<>();
	private final Map<String, Function<Map<String, String>, String>> varMap = new HashMap<>();

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
		return new HashMap<>(this.optMap);
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

	public Map<String, Function<Map<String, String>, String>> getVars() {
		return new HashMap<>(this.varMap);
	}

	public Function<Map<String, String>, String> getVar(String name) {
		return this.varMap.get(name);
	}

	public void addVar(String name, Object value) {
		this.addVar(name, value, true);
	}

	public void addVar(String name, Object value, boolean quoted) {
		this.addVar(name, opts -> {
			if (value instanceof Collection) {
				Collection<Object> collection = new ArrayList<>((Collection<?>) value);
				return collection.stream()
					.filter(Objects::nonNull)
					.map(obj -> quoted ? sqlQuote(obj) : obj.toString())
					.collect(Collectors.joining(","));
			} else {
				return quoted ? sqlQuote(value) : value.toString();
			}
		});
	}

	public void addVar(String name, Function<Map<String, String>, String> operator) {
		this.varMap.put(name, operator);
	}

	public void removeVar(String name) {
		this.varMap.remove(name);
	}

	private String replaceSchema(String schema) {
		try {
			Map<String, String> schemaOptMap = this.getOpts();
			for (Map.Entry<String, String> opt : schemaOptMap.entrySet()) {
				String name = opt.getKey();
				Matcher matcher = schemaOptPattern(name).matcher(schema);
				boolean found = matcher.find() && matcher.groupCount() > 0;
				if (found) schemaOptMap.put(name, matcher.group(1));
			}

			boolean debug = schemaOptMap.getOrDefault(OPT_DSP_DEBUG, "").equalsIgnoreCase("true");

			Map<String, Function<Map<String, String>, String>> schemaVarMap = this.getVars();
			for (Map.Entry<String, Function<Map<String, String>, String>> var : schemaVarMap.entrySet()) {
				String name = var.getKey();
				Matcher matcher = schemaVarPattern(name).matcher(schema);

				boolean found = matcher.find();
				if (debug) {
					System.out.println("[DSP][INFO] ${" + name + "} found: " + found);
				}

				if (found) {
					try {
						String value = var.getValue().apply(schemaOptMap);
						if (debug) {
							System.out.println("[DSP][INFO] ${" + name + "} value: " + value);
						}

						if (value != null) {
							schema = matcher.replaceAll(value);
						}
					} catch (Exception ex) {
						System.err.println("[DSP][ERROR] ${" + name + "} was not processed");
						ex.printStackTrace();
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

	private static Pattern schemaOptPattern(String name) {
		return Pattern.compile(
			"<!\\[CDATA\\[\\s*" + name + "\\s*=\\s*(.*?)\\s*]]>",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
		);
	}

	private static Pattern schemaVarPattern(String name) {
		return Pattern.compile(
			"\\$\\{" + name + "}",
			Pattern.CASE_INSENSITIVE
		);
	}

	// This method does a very simple sanitization, it is NOT safe to use with untrusted data.
	private static String sqlQuote(Object obj) {
		return "'" + obj.toString().replaceAll("'", "''") + "'";
	}

}
