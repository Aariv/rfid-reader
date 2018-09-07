/**
 * 
 */
package com.denali.rfid.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.denali.rfid.camel.ListAggregationStrategy;
import com.denali.rfid.camel.RFIDCommon;

/**
 * @author zentere
 *
 */
@Component
public class RfidRouter extends RouteBuilder {

	@Value("${tcp.to.seda.stage1}")
	private String sedaStage1;

	@Value("${rfid.route.name}")
	private String rfidRouteName;
	
	@Value("${elastic.bulk.index.host}")
	private String elasticBulkIndexHost;

	@Override
	public void configure() throws Exception {
		// Read from seda:stage1 with RFID router
		from(sedaStage1).routeId(rfidRouteName)
			.log("Payload from stage 1 -> ${body}")
			// Convert data to hashmap for elasticsearch
			.bean(RFIDCommon.class, "onConvertToElasticMap")
				.aggregate(constant(true), new ListAggregationStrategy())
					// creates new batches every 10 seconds
					.completionInterval(3000)
						// makes sure the last batch will be processed before application shuts down:
						.forceCompletionOnStop().to(elasticBulkIndexHost)
							.log("Uploaded documents on ${headers.indexName}: ${body.size()}");
	}

}
