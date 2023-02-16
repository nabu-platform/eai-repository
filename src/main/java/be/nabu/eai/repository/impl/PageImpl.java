package be.nabu.eai.repository.impl;

import be.nabu.eai.repository.api.Page;

public class PageImpl implements Page {
	private int current, total;
	private long totalRowCount, pageSize, rowOffset;
	
	public static Page build(long totalAmount, Long offset, Integer pageSize) {
		if (offset == null) {
			offset = 0l;
		}
		PageImpl page = new PageImpl();
		page.setRowOffset(offset);
		page.setPageSize(pageSize == null ? totalAmount : pageSize);
		page.setTotalRowCount(totalAmount);
		page.setTotal(pageSize == null ? 1 : (int) Math.ceil(((double) totalAmount) / pageSize));
		page.setCurrent(pageSize == null ? 0 : (int) Math.floor(((double) offset) / pageSize));
		return page;
	}
	
	@Override
	public int getCurrent() {
		return current;
	}
	public void setCurrent(int current) {
		this.current = current;
	}
	@Override
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	@Override
	public long getTotalRowCount() {
		return totalRowCount;
	}
	public void setTotalRowCount(long totalRowCount) {
		this.totalRowCount = totalRowCount;
	}
	@Override
	public long getPageSize() {
		return pageSize;
	}
	public void setPageSize(long pageSize) {
		this.pageSize = pageSize;
	}
	@Override
	public long getRowOffset() {
		return rowOffset;
	}
	public void setRowOffset(long rowOffset) {
		this.rowOffset = rowOffset;
	}
	
}
