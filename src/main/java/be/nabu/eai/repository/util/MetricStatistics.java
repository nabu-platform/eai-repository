package be.nabu.eai.repository.util;

import be.nabu.libs.metrics.core.api.SinkStatistics;

public class MetricStatistics {
	private long windowStart, windowStop;
	private SinkStatistics statistics;
	private String id, category;
	public long getWindowStart() {
		return windowStart;
	}
	public void setWindowStart(long windowStart) {
		this.windowStart = windowStart;
	}
	public long getWindowStop() {
		return windowStop;
	}
	public void setWindowStop(long windowStop) {
		this.windowStop = windowStop;
	}
	public SinkStatistics getStatistics() {
		return statistics;
	}
	public void setStatistics(SinkStatistics statistics) {
		this.statistics = statistics;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	@Override
	public String toString() {
		return id + ":" + category + " [duration: " + (windowStop - windowStart) + ", #" + statistics.getAmountOfDataPoints() + ", range: " + statistics.getMinimum().getValue() + "-" + statistics.getMaximum().getValue() + ", cma: " + statistics.getCumulativeAverage() + ", ema: " + statistics.getExponentialAverage() + "]";
	}
}
