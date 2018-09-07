/**
 * 
 */
package com.denali.rfid.routes;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.denali.rfid.camel.RFIDCommon;
import com.denali.rfid.dto.RfidDTO;

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
		
		onException(RuntimeException.class).handled(true).log("Error has occured");
		// Get from stage 2
		from(sedaStage2).routeId(visitorRouteName)
			.setProperty("RFID", body())
			// Check reader exists in reader_idx
			.bean(RFIDCommon.class, "onReaderSearchRequest")
			.to(searchElasticHost)
			// if reader available get the reader details if not get from odoo server
			.bean(RFIDCommon.class, "onVerifyReader")
				.log("Reader Information ${body}")
			.choice().when(body().contains("NotFound"))
				// No reader found
				.log("Get from odoo and update reader_idx")
					// Go to ReaderRouter.java
					.to("direct-vm:processReader")
						.log("Response from ReaderRouter.java ${body}")
							.process(new Processor() {
								@Override
								public void process(Exchange exchange) throws Exception {
									RfidDTO rfidDTO = (RfidDTO) exchange.getProperty("RFID");
									exchange.getIn().setBody(rfidDTO);
								}
							})
						.to("direct-vm:processGateway")
			// If reader available proceed to check visitor details VisitorGatewayRouter.java
			.otherwise().log("Procceed with reader : ${body}")
					.process(new Processor() {
						@Override
						public void process(Exchange exchange) throws Exception {
							RfidDTO rfidDTO = (RfidDTO) exchange.getProperty("RFID");
							exchange.getIn().setBody(rfidDTO);
						}
					})
			.to("direct-vm:processGateway");
	}

}
