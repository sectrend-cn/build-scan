package com.sectrend.buildscan.buildTools.scanner;

import java.net.URI;

public class ScannerConf {
	
	public static String OSSKB_URL = "https://127.0.0.1:8088/api/scan/direct";
	
	private final URI apiURI;
	private final String apiKey;
	public ScannerConf(String apiURL, String apiKey) {
		super();
		this.apiURI = URI.create(apiURL);
		this.apiKey = apiKey;
	}
	public URI getApiURL() {
		return apiURI;
	}
	public String getApiKey() {
		return apiKey;
	}
	
	
	public static ScannerConf defaultConf() {
		return new ScannerConf(OSSKB_URL, "");
	}
	
	

}
