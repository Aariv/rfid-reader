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
public class TcpRouter extends RouteBuilder {

	@Value("${tcp.server}")
	private String tcpServer;

	@Value("${tcp.to.seda.stage1}")
	private String sedaStage1;

	@Value("${tcp.to.seda.stage2}")
	private String sedaStage2;

	@Value("${tcp.route.name}")
	private String tcpRouteName;

	@Override
	public void configure() throws Exception {
		// Get data from tcp port 4001
		from(tcpServer).routeId(tcpRouteName)
			.log("Payload from TCP is -> ${body}")
			// Process the stream data and extract header, epc etc
			.bean(RFIDCommon.class, "processRfid")
				.log("After process -> ${body}")
					.to(sedaStage1, sedaStage2);
	}

}
