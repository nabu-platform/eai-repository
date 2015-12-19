package be.nabu.eai.repository.artifacts.web.rest;

public enum WebResponseType {
	XML("application/xml"), JSON("application/json"), FORM_ENCODED("application/x-www-form-urlencoded");
	
	private String mimeType;

	private WebResponseType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMimeType() {
		return mimeType;
	}
}