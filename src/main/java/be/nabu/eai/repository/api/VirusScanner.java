package be.nabu.eai.repository.api;

import java.io.InputStream;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.http.api.HTTPEntity;

public interface VirusScanner extends Artifact {
	// icap has specific support for http entities, streams are wrapped into entities...
	public VirusInfection scan(HTTPEntity entity);
	// others may only implement the stream-based scanning, at that point it needs to format the http entities in order to scan them...
	public VirusInfection scan(InputStream input);
}
