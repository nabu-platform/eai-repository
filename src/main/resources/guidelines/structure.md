# Artifact: structure

Fragments:
- `metadata.xml`: repository metadata around the artifact
- `structure.xml`: the canonical structure definition

## Fragment: structure.xml

Use `structure.xml` contains the actual structure definition.

TypeScript shape:
```typescript
export type D=string,J=string,G=string,M=number,S="PRIVATE"|"PROTECTED"|"PUBLIC"|string,Y="NONE"|"IN"|"OUT"|string,C="LIST"|"SET"|"MAP"|string,U="CANONICAL"|"COMPACT"|string;
export interface P{name?:string;namespace?:string;alias?:string;label?:string;comment?:string;foreignName?:string;minOccurs?:number;maxOccurs?:M;qualified?:boolean;elementQualifiedDefault?:boolean;attributeQualifiedDefault?:boolean;nillable?:boolean;defaultValue?:string;pattern?:string;format?:string;timezone?:string;language?:string;country?:string;length?:number;minLength?:number;maxLength?:number;minInclusive?:string|number|boolean;maxInclusive?:string|number|boolean;minExclusive?:string|number|boolean;maxExclusive?:string|number|boolean;totalDigits?:number;fractionDigits?:number;epsilon?:number;generated?:boolean;temporary?:boolean;environmentSpecific?:boolean;raw?:boolean;matrix?:boolean;validate?:boolean;primaryKey?:boolean;foreignKey?:string;unique?:boolean;indexed?:boolean;identifiable?:boolean;translatable?:boolean;secret?:boolean;token?:boolean;collectionName?:string;collectionFormat?:C;collectionCrudProvider?:string;scope?:S;synchronized?:never;synchronization?:Y;actualType?:J;uuidFormat?:U;allow?:string;restrict?:string;duplicate?:string;dynamicName?:string;persister?:string;enricher?:string;period?:string}
export interface GM{name:string}
export interface SB extends P{type:J|D;enumerations?:string[]}
export interface F extends SB{t:"field"}
export interface A extends SB{t:"attribute"}
export interface IB extends P{t:"structure";superType?:D;children?:N[]}
export interface SC extends IB{type?:undefined;definition?:undefined;enumerations?:never}
export interface SS extends IB{type:J;definition?:undefined;enumerations?:string[]}
export interface SR extends P{t:"structure";definition:D;type?:J;superType?:never;children?:never;enumerations?:never}
export type SD=SC|SS|SR;
export type N=A|F|SD;
export interface Doc{root:SC|SS}
```

Example:
```xml
<structure name="customer">
	<field name="id" type="java.lang.String" minOccurs="1"/>
	<field name="email" type="java.lang.String" pattern=".+@.+"/>
	<structure name="address">
		<field name="street" type="java.lang.String"/>
	</structure>
</structure>
```

Special case: Java maps can be represented as referenced structures with a collection handler, for example:
```xml
<structure collectionHandler="stringMap" definition="java.util.Map" minOccurs="0" name="map"/>
```

This is still modeled as a `structure`, but it represents a map-backed container instead of a regular nested object definition. We do not use it often, but it is useful for fast lookup and high-volume scenarios where hashmap-style access matters.

## Extensions

- Types can extend one another (with `superType` property), structures SHOULD NOT extend simple types and simple types MUST NOT extend structures.
- Structures follow the single inheritance model, but they can also use `restrict` (blacklist) and `allow` (whitelist) properties to provide a comma separated list of fields they don't want to inherit.
- Restricted fields can be redefined but shouldn't be.
- Prefer extension over redefinition so object hierarchies dynamically adapt as new fields get added.

## Available simple types

Common simple-type properties:
- Base for all simple types:
	- `defaultValue`: useful for database DDL
	- `environmentSpecific`: boolean for build-time differentation
	- `generated`: uses sequence in database (only numeric)
	- `indexed`: creates index in database
	- `primaryKey`
	- `foreignKey`: points to primary key in another structure `<structureId>:<field>`  
	- `foreignName`: resolve against local foreign key to remote field, e.g. `companyId:name`
	- `translatable`: boolean if value is translatable
	- `uuidFormat`: PLAIN (default) or DASHES. Use dashes only if remote system requires it
- Marshallable simple types add: 
	- `pattern`: java regex
	- `minLength`
	- `maxLength`
	- `length`
	- `collectionFormat`: MULTI (default), CSV, SSV, TSV, PIPES, LABEL, DEEP_OBJECT, MATRIX_IMPLODE, MATRIX_EXPLODE
- Comparable simple types add: `minInclusive`, `maxInclusive`, `minExclusive`, `maxExclusive`

For structures that will be synchronized to a database, add a `collectionName` attribute which should be the plural, this will be used as table name. For example structure "contract" needs a collectionName like "contracts".
Collection names are automatically rewritten to underscores, so a collection name `contractLines` will be appear as table `contract_lines`.

Built-in wrappers from `types-base`:
- `java.lang.String` (`string`): supports `actualType` to validate the string as another simple type without changing runtime representation; also supports `token` for whitespace normalization.
- `java.lang.Boolean` (`boolean`): accepts `true`/`false`; numeric strings are also accepted and map to `>= 1` => true.
- `java.lang.Byte` (`byte`), `java.lang.Short` (`short`), `java.lang.Integer` (`int`), `java.lang.Long` (`long`): integer numeric types with range constraints.
- `java.math.BigInteger` (`integer`): arbitrary precision integer; use when `int`/`long` are too small.
- `java.lang.Float` (`float`), `java.lang.Double` (`double`): decimal numeric types; both support `fractionDigits` and `totalDigits`, and `double` also supports `epsilon`.
- `java.math.BigDecimal` (`decimal`): arbitrary precision decimal; supports `fractionDigits` and `totalDigits`.
- `java.util.Date` (`dateTime` by default): supports `format`, `timezone`, `language`, `country`, `timeBlock`. Built-in XSD formats include `dateTime`, `time`, `date`, `gDay`, `gMonth`, `gMonthDay`, `gYear`, `gYearMonth`.
- `java.util.UUID` (`uuid`): accepts both dashed and compact 32-char values; `uuidFormat` controls dashed vs compact output.
- `java.io.InputStream` (`inputstream`): important optimized transport type for moving binary content in streaming/high-throughput scenarios. Unlike `byte[]`, it can not be cleanly marshalled to XML, JSON or similar text formats, so use it when the payload should stay as a stream rather than structured text content.
- `byte[]` (`base64Binary`): in memory this remains raw bytes. Base64 is only used when the value is marshalled to XML, JSON or another text format. Use this when you need binary data that can still cross text-based serialization boundaries.
- `java.net.URI` (`anyURI`): marshals as URI text and percent-encodes reserved characters on input.
- `be.nabu.libs.types.base.Duration` (`duration`): XSD-style duration text.
- `java.util.TimeZone` (`timezone`): marshals as timezone ID.
- `java.nio.charset.Charset` (`charset`): marshals as charset name.
- `java.lang.Class` (`class`): marshals as fully qualified class name; behaves like a string-backed simple type.
- Java enums: wrapped dynamically as a string-backed simple type with `enumerations` populated from enum constants.
- `java.security.Key` (`key`), `java.security.cert.Certificate` (`certificate`): known simple wrappers, but they are not text-marshallable like the types above.