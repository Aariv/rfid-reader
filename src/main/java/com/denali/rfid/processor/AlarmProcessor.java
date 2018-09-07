/**
 * 
 */
package com.denali.rfid.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.denali.rfid.dto.RfidDTO;
import com.denali.rfid.dto.RfidVisitor;
import com.denali.rfid.dto.VisitorDetailsDTO;

/**
 * @author zentere
 *
 */
@Component
public class AlarmProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		VisitorDetailsDTO visitorDetails = exchange.getIn().getBody(VisitorDetailsDTO.class);
		Object rfidData = exchange.getProperty("RFID");
		RfidDTO rfidDTO = (RfidDTO) rfidData;
		RfidVisitor rfidVisitor = new RfidVisitor(rfidDTO, visitorDetails);
		exchange.getIn().setBody(rfidVisitor);
	}

}
