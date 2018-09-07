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
public class ReaderRouter extends RouteBuilder {

	@Value("${reader.route.name}")
	private String readerRouterName;
	
	@Value("${elastic.bulk.index.host}")
	private String bulkIndexEndpoint;
	
	@Value("${odoo.rest.readers}")
	private String readerAPI;

	@Override
	public void configure() throws Exception {
		
		onException(RuntimeException.class).handled(true).log("Error has occured");
		
		from("direct-vm:processReader")
			.routeId(readerRouterName)
				.log("Reader Details -> ${body}")
					.to(readerAPI)
					.bean(RFIDCommon.class, "onConvertReaderElasticMap")
					.to(bulkIndexEndpoint)
						.log("Reader updated success ${body}");
	}

}
