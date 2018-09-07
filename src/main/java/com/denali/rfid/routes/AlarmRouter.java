/**
 * 
 */
package com.denali.rfid.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.denali.rfid.camel.AlarmCommon;

/**
 * @author zentere
 *
 */
@Component
public class AlarmRouter extends RouteBuilder {

	@Value("${alarm.route.name}")
	private String alarmRouterName;
	
	@Value("${elastic.bulk.index.host}")
	private String bulkIndexEndpoint;
	
	@Value("${mqtt.url}")
	private String mqttEndpoint;
	
	
	
	@Override
	public void configure() throws Exception {
		
		onException(RuntimeException.class).handled(true).log("Error has occured");
		
		from("direct-vm:processAlarm")
			.routeId(alarmRouterName)
				.log("Alarm Details -> ${body}")
					.bean(AlarmCommon.class, "onReaderMismatch")
						.log("on Reader Mismath ${body}")
						.marshal().json()
						.log("After coversion ${body}")
					.bean(AlarmCommon.class, "onAlarmPayload")
							.to(mqttEndpoint)
					.bean(AlarmCommon.class, "onReaderNotFound")
						.log("on Reader Not found ${body}")
						.marshal().json()
					.bean(AlarmCommon.class, "onAlarmPayload")
							.to(mqttEndpoint)
					.bean(AlarmCommon.class, "onVisitorDateExpired")
						.log("on Visiror date expired ${body}")
						.marshal().json()
						.bean(AlarmCommon.class, "onAlarmPayload")
							.to(mqttEndpoint);
	}

}
