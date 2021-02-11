package com.stratebi.lincebi.dsp;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import mondrian.i18n.LocalizingDynamicSchemaProcessor;
import mondrian.olap.Util.PropertyList;

public class BaseDynamicSchemaProcessor extends LocalizingDynamicSchemaProcessor {

	private static final String OPT_DSP_DEBUG = "DSP_DEBUG";
	private static final String SQL_STR_QUOTE = "'";

	private final List<String> optList = Arrays.asList(OPT_DSP_DEBUG);
	private final Map<String, Function<Map<String, String>, Object>> varMap = new HashMap<String, Function<Map<String, String>, Object>>();

	@Override
	public String filter(String schemaUrl, PropertyList connectInfo, InputStream stream) throws Exception {
		String schema = super.filter(schemaUrl, connectInfo, stream);

		try {
			Map<String, String> opts = new HashMap<String, String>();
			for (String name : this.optList) {
				Pattern pattern = schemaOptPattern(name);
				String value = matcherGroup(pattern.matcher(schema), 1);
				opts.put(name, value);
			}

			boolean debug = opts.getOrDefault(OPT_DSP_DEBUG, "").equalsIgnoreCase("true");

			for (Map.Entry<String, Function<Map<String, String>, Object>> var : this.varMap.entrySet()) {
				String name = var.getKey();
				Pattern pattern = schemaVarPattern(name);
				boolean found = pattern.matcher(schema).find();
				Object value = var.getValue().apply(opts);

				if (debug) {
					System.out.println("[DSP] ${" + name + "} value: " + value);
					System.out.println("[DSP] ${" + name + "} found: " + found);
				}

				if (found) {
					if (value instanceof Collection) {
						Collection<Object> collection = new ArrayList<>((Collection<?>) value);
						schema = replaceSchemaCollection(pattern, collection, schema);
					} else {
						schema = replaceSchemaObject(pattern, value, schema);
					}
				}
			}

			if (debug) {
				System.out.println("[DSP] Replaced Schema:\n" + schema);
			}
		} catch (Exception ex) {
			System.err.println("[DSP] Error. Schema was not processed:\n" + schema);
			ex.printStackTrace();
		}

		return schema;
	}

	public void addOpt(String name) {
		this.optList.add(name);
	}

	public void removeOpt(String name) {
		this.optList.remove(name);
	}

	public void addVar(String name, Function<Map<String, String>, Object> operator) {
		this.varMap.put(name, operator);
	}

	public void removeVar(String name) {
		this.varMap.remove(name);
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

	public static String replaceSchemaObject(Pattern pattern, Object obj, String schema) {
		return obj != null
			? pattern.matcher(schema).replaceAll(sqlQuote(obj))
			: schema;
	}

	public static String replaceSchemaCollection(Pattern pattern, Collection<Object> collection, String schema) {
		return pattern.matcher(schema).replaceAll(
			String.join(",", collection.stream()
				.filter(obj -> obj != null)
				.map(obj -> sqlQuote(obj))
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
	public static String sqlQuote(Object obj) {
		return SQL_STR_QUOTE + obj.toString().replaceAll(SQL_STR_QUOTE, "") + SQL_STR_QUOTE;
	}

}
