/**
 * 
 */
package com.denali.rfid.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.denali.rfid.camel.RFIDCommon;

/**
 * @author zentere
 *
 */
@Component
public class VisitorRouter extends RouteBuilder {

	@Value("${tcp.to.seda.stage2}")
	private String sedaStage2;

	@Value("${visitor.route.name}")
	private String visitorRouteName;
	
	@Value("${elastic.index.search}")
	private String searchElasticHost;

	@Override
	public void configure() throws Exception {
		from(sedaStage2).routeId(visitorRouteName)
		.setProperty("RFID", body())
		// Check reader exists in reader_idx
		.bean(RFIDCommon.class, "onReaderSearchRequest")
			.to(searchElasticHost)
			// if reader available get the reader details if not get from odoo server
			.bean(RFIDCommon.class, "onVerifyReader")
			.log("Reader Information ${body}")
			.choice().when(body().startsWith("RFID"))
				.log("Get from odoo and update reader")
				.to("direct-vm:processReader")
			.otherwise().log("Procceed with ${body}");
	}

}
