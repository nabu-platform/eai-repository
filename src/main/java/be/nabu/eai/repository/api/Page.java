package be.nabu.eai.repository.api;

public interface Page {
	// the current page you have retrieved
	public int getCurrent();
	// the total amount of pages available
	public int getTotal();
	// the amount of entries on each page (= limit)
	public long getPageSize();
	// the total amount of entries available
	public long getTotalRowCount();
	// the offset in rows (= offset)
	public long getRowOffset();
}
