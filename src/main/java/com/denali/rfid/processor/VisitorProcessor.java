/**
 * 
 */
package com.denali.rfid.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.denali.rfid.dto.RfidDTO;

/**
 * @author zentere
 *
 */
@Component
public class VisitorProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Object rfid = exchange.getProperty("RFID");
		RfidDTO dto = (RfidDTO) rfid;
		exchange.getIn().setBody(dto.getEpc());
	}

}
