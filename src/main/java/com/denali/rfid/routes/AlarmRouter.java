/**
 * 
 */
package com.denali.rfid.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author zentere
 *
 */
@Component
public class AlarmRouter extends RouteBuilder {

	@Value("${alarm.route.name}")
	private String alarmRouterName;
	
	@Override
	public void configure() throws Exception {
		
		onException(RuntimeException.class).handled(true).log("Error has occured");
		
//		from("direct-vm:processAlarm")
//			.routeId(alarmRouterName)
//				.log("Alarm Details -> ${body}")
//				.setProperty("RFID_ALARM", body())
//					.process(new Processor() {
//						
//						@Override
//						public void process(Exchange exchange) throws Exception {
//							RfidVisitor rfidVisitor = (RfidVisitor) exchange.getProperty("RFID_ALARM");
//							exchange.getIn().setBody(rfidVisitor);
//						}
//					})
//					.to("direct-vm:onReaderMismatch")
//					.process(new Processor() {
//						
//						@Override
//						public void process(Exchange exchange) throws Exception {
//							RfidVisitor rfidVisitor = (RfidVisitor) exchange.getProperty("RFID_ALARM");
//							exchange.getIn().setBody(rfidVisitor);
//						}
//					})
//					.to("direct-vm:onReaderNotFound")
//					.process(new Processor() {
//						
//						@Override
//						public void process(Exchange exchange) throws Exception {
//							RfidVisitor rfidVisitor = (RfidVisitor) exchange.getProperty("RFID_ALARM");
//							exchange.getIn().setBody(rfidVisitor);
//						}
//					})
//					.to("direct-vm:onVisitorExpiredDate");
		
		from("direct-vm:processAlarm")
		.routeId(alarmRouterName)
			.log("Alarm Details -> ${body}")
			.pipeline()
				.to("direct-vm:onReaderMismatch")
				.to("direct-vm:onReaderNotFound")
				.to("direct-vm:onVisitorExpiredDate");
	}
}
