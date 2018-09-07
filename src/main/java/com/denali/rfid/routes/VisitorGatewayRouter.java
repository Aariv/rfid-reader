/**
 * 
 */
package com.denali.rfid.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.denali.rfid.camel.RFIDCommon;
import com.denali.rfid.processor.AlarmProcessor;
import com.denali.rfid.processor.VisitorProcessor;

/**
 * @author zentere
 *
 */
@Component
public class VisitorGatewayRouter extends RouteBuilder {

	@Value("${visitor.route.gateway.name}")
	private String visitorGateWayRouterName;
	
	@Value("${elastic.bulk.index.host}")
	private String bulkIndexEndpoint;
	
	@Value("${elastic.index.search_vrl_idx}")
	private String searchVrlElasticHost;
	
	@Autowired
	private VisitorProcessor visitorProcessor;
	
	@Autowired
	private AlarmProcessor alarmProcessor;
	
	@Override
	public void configure() throws Exception {
		
		onException(RuntimeException.class).handled(true).log("Error has occured");
		
		from("direct-vm:processGateway")
			.routeId(visitorGateWayRouterName)
			.log("Procceed visitorGateWayRouterName with ${body}")
			.bean(RFIDCommon.class, "onVisitorSearchsRequest")
				.to(searchVrlElasticHost)
					.bean(RFIDCommon.class, "onVerifyVisitor")
					.log("Result for epc is -> ${body}")
					.choice().when(body().contains("NotFound"))
						.log("Get from odoo and update visitor")
							.process(visitorProcessor)
								// Go to VisitorDetailsRouter.java to get from odoo and update in vrl_idx
								.to("direct-vm:processVisitor")
							.log("Response from ${body}")
							.bean(RFIDCommon.class, "onVisitorSearch")
								.to(searchVrlElasticHost)
							.bean(RFIDCommon.class, "onVerifyVisitor")
							.log("Result from Elastic on epc ${body} to Generate Alarm")
							.process(alarmProcessor)
							// Go to AlarmRouter.java
							.to("direct-vm:processAlarm")
					// If Visitor is available 
					.otherwise()
						.process(alarmProcessor)
							.log("Procceed with Visitor ${body} and Generate Alarm")
							// Go to AlarmRouter.java
								.to("direct-vm:processAlarm");
	}

}
