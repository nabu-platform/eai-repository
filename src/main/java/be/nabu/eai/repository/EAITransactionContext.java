package be.nabu.eai.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.services.SimpleTransactionContext;

public class EAITransactionContext extends SimpleTransactionContext {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void commit(String id) {
		logger.debug("Committing transaction: " + id);
		super.commit(id);
	}

	@Override
	public void rollback(String id) {
		logger.warn("Rolling back transaction: " + id);
		super.rollback(id);
	}

	@Override
	public String start() {
		String id = super.start();
		logger.debug("Starting transaction: " + id);
		return id;
	}
}
