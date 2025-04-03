package jhi.fab.dto;

import jhi.fab.codegen.tables.pojos.*;

public class SubsamplesDTO extends Subsamples
{
	private String genotypeName;
	private String varietyName;

	public String getGenotypeName() {
		return genotypeName;
	}

	public void setGenotypeName(String genotypeName) {
		this.genotypeName = genotypeName;
	}

	public String getVarietyName() {
		return varietyName;
	}

	public void setVarietyName(String varietyName) {
		this.varietyName = varietyName;
	}
}