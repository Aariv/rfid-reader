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
public class VisitorDetailsRouter extends RouteBuilder {

	@Value("${visitor.details.route.name}")
	private String visitorDetailsRouterName;
	
	@Value("${elastic.index.host}")
	private String indexEndpoint;
	
	@Value("${odoo.rest.visitor}")
	private String visitorAPI;

	@Override
	public void configure() throws Exception {
		
		onException(RuntimeException.class).handled(true).log("Error has occured");
		
		from("direct-vm:processVisitor")
			.routeId(visitorDetailsRouterName)
				.log("Visitor Details -> ${body}")
					.setProperty("EPC", body())
					//.to(visitorAPI+ simple("${body}"))
					.recipientList(simple(visitorAPI + "${body}"))
					.log("Result for Visitor API ${body}")
					.bean(RFIDCommon.class, "onConvertVisitorElasticMap")
					//.bean(RFIDCommon.class, "validateVisitor")
					.choice().when(body().isEqualTo(null))
						.log("No Visitor found")
					.otherwise()
						// Index in vrl_idx
						.to(indexEndpoint).log("Visitor details updated success ${body}");
	}

}
