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
public class VisitorExpiredAlarmRouter extends RouteBuilder {

	@Value("${alarm.route.name}")
	private String alarmRouterName;
	
	@Value("${elastic.bulk.index.host}")
	private String bulkIndexEndpoint;
	
	@Value("${elastic.index.search_alarm_idx}")
	private String alarmSearchIndexEndpoint;
	
	@Value("${mqtt.url}")
	private String mqttEndpoint;
	
	@Override
	public void configure() throws Exception {
		
		onException(RuntimeException.class).handled(true).log("Error has occured");
		
		from("direct-vm:onVisitorExpiredDate")
		.log("on onVisitorDateExpired ${body}")
		.bean(AlarmCommon.class, "onVisitorDateExpired")
		.setProperty("ALARM", body())
		// Search elastic-search for the same id if alarm is not sent
		.bean(AlarmCommon.class, "onSearchAlarm")
		.to(alarmSearchIndexEndpoint)
		.bean(AlarmCommon.class, "validateAlarm")
		.choice().when(body().contains("NotFound"))
			.bean(AlarmCommon.class, "onConvertAlarmToElastic")
				.log("on Reader onConvertAlarmToElastic ${body}")
			.to(bulkIndexEndpoint)
				.bean(AlarmCommon.class, "onSearchAlarm")
				.to(alarmSearchIndexEndpoint)
				.bean(AlarmCommon.class, "validateAlarm")
				.marshal().json()
				.log("After coversion ${body}").bean(AlarmCommon.class, "onAlarmPayload")
				.to(mqttEndpoint)
		.otherwise().log("Alarm already exists for onVisitorDateExpired");
	
	}
}
