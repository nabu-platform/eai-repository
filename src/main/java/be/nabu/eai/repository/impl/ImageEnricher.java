package be.nabu.eai.repository.impl;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.services.api.ImagedServiceRunner;

public class ImageEnricher implements EventEnricher {

	@SuppressWarnings("unchecked")
	@Override
	public Object enrich(Object object) {
		ServiceRunner serviceRunner = EAIResourceRepository.getInstance().getServiceRunner();
		if (serviceRunner instanceof ImagedServiceRunner) {
			if (!(object instanceof ComplexContent)) {
				object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
			}
			if (object != null) {
				// if we have a field called "sessionId", we enrich it
				if (((ComplexContent) object).getType().get("imageVersion") != null) {
					((ComplexContent) object).set("imageVersion", ((ImagedServiceRunner) serviceRunner).getImageVersion());
				}
				if (((ComplexContent) object).getType().get("imageEnvironment") != null) {
					((ComplexContent) object).set("imageEnvironment", ((ImagedServiceRunner) serviceRunner).getImageEnvironment());
				}
				if (((ComplexContent) object).getType().get("imageName") != null) {
					((ComplexContent) object).set("imageName", ((ImagedServiceRunner) serviceRunner).getImageName());
				}
				if (((ComplexContent) object).getType().get("imageDate") != null) {
					((ComplexContent) object).set("imageDate", ((ImagedServiceRunner) serviceRunner).getImageDate());
				}
			}
		}
		return null;
	}
}
