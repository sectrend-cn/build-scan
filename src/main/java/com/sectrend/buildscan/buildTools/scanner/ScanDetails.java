/**
 * Copyright (C) 2018-2020 SCANOSS LTD
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sectrend.buildscan.buildTools.scanner;

public class ScanDetails {
	private final String wfp;
	private final ScanType scanType;
	private final String sbomPath;
	private final ScanFormat format;
	public ScanDetails(String wfp, ScanType scanType, String sbomPath, ScanFormat format) {
		super();
		this.wfp = wfp;
		this.scanType = scanType;
		this.sbomPath = sbomPath;
		this.format = format;
	}
	
	public String getWfp() {
		return wfp;
	}
	
	public ScanType getScanType() {
		return scanType;
	}
	public String getSbomPath() {
		return sbomPath;
	}
	public ScanFormat getFormat() {
		return format;
	}
	
	

}
