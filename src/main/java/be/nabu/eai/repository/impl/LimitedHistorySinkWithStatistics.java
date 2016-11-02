package be.nabu.eai.repository.impl;

import be.nabu.libs.metrics.core.api.SinkStatistics;
import be.nabu.libs.metrics.core.api.StatisticsContainer;
import be.nabu.libs.metrics.core.sinks.LimitedHistorySink;
import be.nabu.libs.metrics.core.sinks.StatisticsSink;

public class LimitedHistorySinkWithStatistics extends LimitedHistorySink implements StatisticsContainer {

	private static final int WINDOW = 100;

	private StatisticsSink statistics;
	
	public LimitedHistorySinkWithStatistics(int size) {
		super(size);
		this.statistics = new StatisticsSink(WINDOW);
	}
	
	@Override
	public void push(long timestamp, long value) {
		super.push(timestamp, value);
		statistics.push(timestamp, value);
	}

	@Override
	public SinkStatistics getStatistics() {
		return statistics;
	}

}
