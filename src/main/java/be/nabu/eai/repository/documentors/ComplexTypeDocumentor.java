package be.nabu.eai.repository.documentors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.repository.api.Documented;
import be.nabu.eai.repository.api.Documentor;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.properties.CommentProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.NameProperty;
import be.nabu.libs.types.properties.NamespaceProperty;

public class ComplexTypeDocumentor implements Documentor<ComplexType> {

	public final class DocumentedImplementation implements Documented {
		
		private ComplexType type;
		private String content;

		public DocumentedImplementation(ComplexType type, String content) {
			this.type = type;
			this.content = content;
		}
		
		@Override
		public String getTitle() {
			return type instanceof DefinedType ? ((DefinedType) type).getId() : null;
		}

		@Override
		public String getDescription() {
			return content;
		}

		@Override
		public Collection<String> getTags() {
			return new ArrayList<String>();
		}

		@Override
		public String getMimeType() {
			return "text/html";
		}
	}

	@Override
	public Class<ComplexType> getDocumentedClass() {
		return ComplexType.class;
	}

	@Override
	public Documented getDocumentation(Repository repository, ComplexType type) {
		StringBuilder builder = new StringBuilder();
		builder.append("<section class='complex-type");
		if (type instanceof DefinedType) {
			builder.append(" " + ((DefinedType) type).getId());
		}
		builder.append("'>");
		getDocumentationAsHtml(type, builder);
		builder.append("</section>");
		return new DocumentedImplementation(type, builder.toString());
	}

	private void getDocumentationAsHtml(ComplexType type, StringBuilder builder) {
		builder.append("<ul>");
		for (Element<?> element : TypeUtils.getAllChildren(type)) {
			builder.append("<li class='");
			Value<Integer> minOccurs = element.getProperty(MinOccursProperty.getInstance());
			Value<Integer> maxOccurs = element.getProperty(MaxOccursProperty.getInstance());
			if (minOccurs == null || minOccurs.getValue() > 0) {
				builder.append("required ");
			}
			else {
				builder.append("optional ");
			}
			if (maxOccurs == null || maxOccurs.getValue() == 1) {
				builder.append("single");
			}
			else {
				builder.append("list");
			}
			builder.append("'>");
			builder.append("<span class='name'>").append(element.getName()).append("</span>");
			if (element.getNamespace() != null) {
				builder.append("<span class='namespace'>").append(element.getNamespace()).append("</span>");
			}
			if (element.getType() instanceof SimpleType) {
				builder.append("<span class='type simple-type'>").append(((SimpleType<?>) element.getType()).getName(element.getProperties())).append("</span>");
			}
			else if (element.getType() instanceof DefinedType) {
				builder.append("<span class='type complex-type'>").append(((DefinedType) element.getType()).getId()).append("</span>");
			}
			Value<String> comment = element.getProperty(CommentProperty.getInstance());
			if (comment != null) {
				builder.append("<span class='description'>").append(comment.getValue()).append("</span>");
			}
			
			// don't repeat the dedicated properties
			List<Property<?>> coveredProperties = Arrays.asList(new Property[] {
				NameProperty.getInstance(),
				NamespaceProperty.getInstance(),
				MinOccursProperty.getInstance(),
				MaxOccursProperty.getInstance()
			});
			StringBuilder properties = new StringBuilder();
			for (Value<?> value : element.getProperties()) {
				if (!coveredProperties.contains(value.getProperty()) && value.getValue() != null) {
					properties.append("<span class='property'><span class='key'>").append(value.getProperty().getName()).append("</span><span class='value'>").append(value.getValue()).append("</span></span>");
				}
			}
			if (!properties.toString().isEmpty()) {
				builder.append("<span class='properties'>").append(properties.toString()).append("</span>");
			}
			builder.append("</li>");
			if (element.getType() instanceof ComplexType) {
				getDocumentationAsHtml((ComplexType) element.getType(), builder);
			}
		}
		builder.append("</ul>");
	}
}
