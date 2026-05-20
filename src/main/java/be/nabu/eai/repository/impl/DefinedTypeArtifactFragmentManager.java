package be.nabu.eai.repository.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.validator.api.Validation;

public class DefinedTypeArtifactFragmentManager extends BaseNodeMetadataArtifactFragmentManager<DefinedType> {

	private static final String STRUCTURE_PATH = "structure.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String EXTENSION_HIERARCHY = "extension-hierarchy";
	private static final String ARTIFACT_CATEGORY = "type";
	private static final String COMPLEX_ARTIFACT_TYPE = "structure";

	@Override
	public List<ArtifactFragment> listFragments(DefinedType artifact) {
		if (!(artifact instanceof ComplexType)) {
			return getSharedFragments(artifact);
		}
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>(getSharedFragments(artifact));
		fragments.add(new ArtifactFragment() {
			@Override
			public boolean isEditable() {
				return false;
			}

			@Override
			public boolean isRemovable() {
				return false;
			}

			@Override
			public String getPath() {
				return STRUCTURE_PATH;
			}

			@Override
			public String getContent() {
				XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
				marshaller.setIgnoreUnknownSuperTypes(true);
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				try {
					marshaller.marshal(output, (ComplexType) artifact);
					return new String(output.toByteArray(), "UTF-8");
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String getContentType() {
				return CONTENT_TYPE;
			}

			@Override
			public String getArtifactId() {
				return artifact.getId();
			}

			@Override
			public String getFragmentType() {
				return "structure";
			}

			@Override
			public Map<String, String> getProperties() {
				Map<String, String> properties = new LinkedHashMap<String, String>();
				String extensionHierarchy = getExtensionHierarchy(artifact);
				if (!extensionHierarchy.isEmpty()) {
					properties.put(EXTENSION_HIERARCHY, extensionHierarchy);
				}
				return properties;
			}
		});
		return fragments;
	}

	private String getExtensionHierarchy(DefinedType artifact) {
		List<String> hierarchy = new ArrayList<String>();
		Type parent = artifact.getSuperType();
		while (parent != null) {
			if (parent instanceof DefinedType) {
				String id = ((DefinedType) parent).getId();
				if (id != null && !id.isEmpty()) {
					hierarchy.add(id);
				}
			}
			parent = parent.getSuperType();
		}
		return String.join(",", hierarchy);
	}

	@Override
	public List<Validation<?>> updateFragment(DefinedType artifact, String path, String oldContent, String newContent) {
		throw new UnsupportedOperationException("Updating fragments is not supported for defined types");
	}

	@Override
	public List<Validation<?>> deleteFragment(DefinedType artifact, String path) {
		throw new UnsupportedOperationException("Deleting fragments is not supported for defined types");
	}

	@Override
	public List<Validation<?>> createFragment(DefinedType artifact, String path, String content) {
		throw new UnsupportedOperationException("Creating fragments is not supported for defined types");
	}

	@Override
	public String getGuidelines(List<String> fragmentTypes) {
		List<String> filtered = new ArrayList<String>();
		if (fragmentTypes == null || fragmentTypes.isEmpty() || fragmentTypes.contains("structure") || fragmentTypes.contains("structure.xml")) {
			filtered.add("# Artifact: structure\n\nFragments:\n- `metadata.xml`: repository metadata around the artifact\n- `structure.xml`: the canonical structure definition\n");
			filtered.add(super.getGuidelines(Arrays.asList("metadata")));
			filtered.add("## Fragment: structure.xml\n\n"
				+ "Use `structure.xml` contains the actual structure definition.\n\n"
				+ "TypeScript shape:\n"
				+ "```typescript\n"
				+ "export type D=string,J=string,G=string,M=number,S=\"PRIVATE\"|\"PROTECTED\"|\"PUBLIC\"|string,Y=\"NONE\"|\"IN\"|\"OUT\"|string,C=\"LIST\"|\"SET\"|\"MAP\"|string,U=\"CANONICAL\"|\"COMPACT\"|string;\n"
				+ "export interface P{name?:string;namespace?:string;alias?:string;label?:string;comment?:string;foreignName?:string;minOccurs?:number;maxOccurs?:M;qualified?:boolean;elementQualifiedDefault?:boolean;attributeQualifiedDefault?:boolean;nillable?:boolean;defaultValue?:string;pattern?:string;format?:string;timezone?:string;language?:string;country?:string;length?:number;minLength?:number;maxLength?:number;minInclusive?:string|number|boolean;maxInclusive?:string|number|boolean;minExclusive?:string|number|boolean;maxExclusive?:string|number|boolean;totalDigits?:number;fractionDigits?:number;epsilon?:number;generated?:boolean;temporary?:boolean;environmentSpecific?:boolean;raw?:boolean;matrix?:boolean;validate?:boolean;primaryKey?:boolean;foreignKey?:string;unique?:boolean;indexed?:boolean;identifiable?:boolean;translatable?:boolean;secret?:boolean;token?:boolean;collectionName?:string;collectionFormat?:C;collectionCrudProvider?:string;scope?:S;synchronized?:never;synchronization?:Y;actualType?:J;uuidFormat?:U;allow?:string;restrict?:string;duplicate?:string;dynamicName?:string;persister?:string;enricher?:string;period?:string}\n"
				+ "export interface GM{name:string}\n"
				+ "export interface SB extends P{type:J|D;enumerations?:string[]}\n"
				+ "export interface F extends SB{t:\"field\"}\n"
				+ "export interface A extends SB{t:\"attribute\"}\n"
				+ "export interface IB extends P{t:\"structure\";superType?:D;children?:N[]}\n"
				+ "export interface SC extends IB{type?:undefined;definition?:undefined;enumerations?:never}\n"
				+ "export interface SS extends IB{type:J;definition?:undefined;enumerations?:string[]}\n"
				+ "export interface SR extends P{t:\"structure\";definition:D;type?:J;superType?:never;children?:never;enumerations?:never}\n"
				+ "export type SD=SC|SS|SR;\n"
				+ "export type N=A|F|SD;\n"
				+ "export interface Doc{root:SC|SS}\n"
				+ "```\n\n"
				+ "Example:\n"
				+ "```xml\n"
				+ "<structure name=\"customer\">\n"
				+ "\t<field name=\"id\" type=\"java.lang.String\" minOccurs=\"1\"/>\n"
				+ "\t<field name=\"email\" type=\"java.lang.String\" pattern=\".+@.+\"/>\n"
				+ "\t<structure name=\"address\">\n"
				+ "\t\t<field name=\"street\" type=\"java.lang.String\"/>\n"
				+ "\t</structure>\n"
				+ "</structure>\n"
				+ "```\n\n"
				+ "Special case: Java maps can be represented as referenced structures with a collection handler, for example:\n"
				+ "```xml\n"
				+ "<structure collectionHandler=\"stringMap\" definition=\"java.util.Map\" minOccurs=\"0\" name=\"map\"/>\n"
				+ "```\n\n"
				+ "This is still modeled as a `structure`, but it represents a map-backed container instead of a regular nested object definition. We do not use it often, but it is useful for fast lookup and high-volume scenarios where hashmap-style access matters.\n\n"
				+ buildSimpleTypeGuidelines());
		}
		return filtered.isEmpty() ? null : String.join("\n\n", filtered);
	}

	private String buildSimpleTypeGuidelines() {
		List<String> lines = new ArrayList<String>();
		lines.add("## Available simple types");
		lines.add("");
		lines.add("Common simple-type properties:");
		lines.add("- Base for all simple types: `defaultValue`, `environmentSpecific`, `generated`, `indexed`, `primaryKey`, `foreignKey`, `foreignName`, `dynamicForeignKey`, `aggregate`, `calculation`, `translatable`, `uuidFormat`");
		lines.add("- Marshallable simple types add: `pattern`, `minLength`, `maxLength`, `length`, `collectionFormat`, `synchronization`");
		lines.add("- Comparable simple types add: `minInclusive`, `maxInclusive`, `minExclusive`, `maxExclusive`");
		lines.add("");
		lines.add("Built-in wrappers from `types-base`:");
		lines.add("- `java.lang.String` (`string`): supports `actualType` to validate the string as another simple type without changing runtime representation; also supports `token` for whitespace normalization.");
		lines.add("- `java.lang.Boolean` (`boolean`): accepts `true`/`false`; numeric strings are also accepted and map to `>= 1` => true.");
		lines.add("- `java.lang.Byte` (`byte`), `java.lang.Short` (`short`), `java.lang.Integer` (`int`), `java.lang.Long` (`long`): integer numeric types with range constraints.");
		lines.add("- `java.math.BigInteger` (`integer`): arbitrary precision integer; use when `int`/`long` are too small.");
		lines.add("- `java.lang.Float` (`float`), `java.lang.Double` (`double`): decimal numeric types; both support `fractionDigits` and `totalDigits`, and `double` also supports `epsilon`.");
		lines.add("- `java.math.BigDecimal` (`decimal`): arbitrary precision decimal; supports `fractionDigits` and `totalDigits`.");
		lines.add("- `java.util.Date` (`dateTime` by default): supports `format`, `timezone`, `language`, `country`, `timeBlock`. Built-in XSD formats include `dateTime`, `time`, `date`, `gDay`, `gMonth`, `gMonthDay`, `gYear`, `gYearMonth`.");
		lines.add("- `java.util.UUID` (`uuid`): accepts both dashed and compact 32-char values; `uuidFormat` controls dashed vs compact output.");
		lines.add("- `java.io.InputStream` (`inputstream`): important optimized transport type for moving binary content in streaming/high-throughput scenarios. Unlike `byte[]`, it can not be cleanly marshalled to XML, JSON or similar text formats, so use it when the payload should stay as a stream rather than structured text content.");
		lines.add("- `byte[]` (`base64Binary`): in memory this remains raw bytes. Base64 is only used when the value is marshalled to XML, JSON or another text format. Use this when you need binary data that can still cross text-based serialization boundaries.");
		lines.add("- `java.net.URI` (`anyURI`): marshals as URI text and percent-encodes reserved characters on input.");
		lines.add("- `be.nabu.libs.types.base.Duration` (`duration`): XSD-style duration text.");
		lines.add("- `java.util.TimeZone` (`timezone`): marshals as timezone ID.");
		lines.add("- `java.nio.charset.Charset` (`charset`): marshals as charset name.");
		lines.add("- `java.lang.Class` (`class`): marshals as fully qualified class name; behaves like a string-backed simple type.");
		lines.add("- Java enums: wrapped dynamically as a string-backed simple type with `enumerations` populated from enum constants.");
		lines.add("- `java.security.Key` (`key`), `java.security.cert.Certificate` (`certificate`): known simple wrappers, but they are not text-marshallable like the types above.");
		appendRepositorySimpleTypes(lines);
		return String.join("\n", lines);
	}

	private void appendRepositorySimpleTypes(List<String> lines) {
		EAIResourceRepository repository = EAIResourceRepository.getInstance();
		if (repository == null) {
			return;
		}
		List<DefinedSimpleType> simpleTypes = repository.getArtifacts(DefinedSimpleType.class);
		if (simpleTypes == null || simpleTypes.isEmpty()) {
			return;
		}
		List<String> entries = new ArrayList<String>();
		for (DefinedSimpleType simpleType : simpleTypes) {
			if (simpleType == null || simpleType.getId() == null || simpleType.getId().trim().isEmpty()) {
				continue;
			}
			entries.add("- `" + simpleType.getId() + "` (name: `" + simpleType.getName() + "`, instance: `" + (simpleType.getInstanceClass() == null ? "unknown" : simpleType.getInstanceClass().getName()) + "`, superType: `" + describeType(simpleType.getSuperType()) + "`, properties: " + describeProperties(simpleType) + ")");
		}
		if (entries.isEmpty()) {
			return;
		}
		Collections.sort(entries);
		lines.add("");
		lines.add("Repository-defined simple types currently available:");
		lines.addAll(entries);
	}

	private String describeType(Type type) {
		if (type == null) {
			return "none";
		}
		if (type instanceof DefinedType) {
			String id = ((DefinedType) type).getId();
			if (id != null && !id.trim().isEmpty()) {
				return id;
			}
		}
		String name = type.getName();
		return name == null || name.trim().isEmpty() ? type.getClass().getName() : name;
	}

	private String describeProperties(DefinedSimpleType simpleType) {
		List<String> properties = new ArrayList<String>();
		for (Value<?> value : simpleType.getProperties()) {
			if (value == null || value.getProperty() == null || value.getProperty().getName() == null) {
				continue;
			}
			StringBuilder builder = new StringBuilder();
			builder.append(value.getProperty().getName());
			if (value.getValue() != null) {
				builder.append("=").append(String.valueOf(value.getValue()));
			}
			properties.add(builder.toString());
		}
		return properties.isEmpty() ? "none" : String.join(", ", properties);
	}

	@Override
	public Class<DefinedType> getArtifactClass() {
		return DefinedType.class;
	}
	
	@Override
	public String getArtifactType() {
		return COMPLEX_ARTIFACT_TYPE;
	}

	@Override
	public String getArtifactCategory() {
		return ARTIFACT_CATEGORY;
	}

}
