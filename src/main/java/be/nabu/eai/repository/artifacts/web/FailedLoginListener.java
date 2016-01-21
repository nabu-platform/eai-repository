package be.nabu.eai.repository.artifacts.web;

import java.util.Date;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.glue.impl.UserMethods;
import be.nabu.libs.metrics.core.api.SinkEvent;

/**
 * When this listener is triggered, it will blacklist the ip
 * It presumes that filters will block the triggering of this listener until such a time that an ip has to be blocked
 */
public class FailedLoginListener implements EventHandler<SinkEvent, Void> {

	private WebArtifact artifact;
	private long duration;

	public FailedLoginListener(WebArtifact artifact, long duration) {
		this.artifact = artifact;
		this.duration = duration;
	}
	
	@Override
	public Void handle(SinkEvent event) {
		// also skip the ":" at the end
		String ip = event.getCategory().substring(UserMethods.METRICS_LOGIN_FAILED.length() + 1);
		artifact.getListener().blacklistLogin(ip, new Date(new Date().getTime() + duration));
		return null;
	}

}
