package jhi.fab.dto;

import jhi.fab.codegen.tables.pojos.*;

public class OutbreaksDTO extends Outbreaks
{
	private String severityName;
	private String sourceName;

	public String getSeverityName() {
		return severityName;
	}

	public void setSeverityName(String severityName) {
		this.severityName = severityName;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
}