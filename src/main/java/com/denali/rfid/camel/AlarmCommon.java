/**
 * 
 */
package com.denali.rfid.camel;

import java.util.Date;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import com.denali.rfid.dto.AlarmDTO;
import com.denali.rfid.dto.AlarmType;
import com.denali.rfid.dto.ReaderDTO;
import com.denali.rfid.dto.RfidDTO;
import com.denali.rfid.dto.RfidVisitor;
import com.denali.rfid.dto.VisitorDetailsDTO;
import com.denali.rfid.utils.DateUtils;

/**
 * @author zentere
 *
 */
@Component
public class AlarmCommon {

	public void onReaderMismatch(Exchange exchange) {
		RfidVisitor rfidVisitor = exchange.getIn().getBody(RfidVisitor.class);
		if (rfidVisitor != null) {
			VisitorDetailsDTO visitorDTO = rfidVisitor.getVisitorDetailsDTO();
			RfidDTO rfid = rfidVisitor.getRfidDTO();
			if (visitorDTO.getReaders() != null && !visitorDTO.getReaders().isEmpty()) {
				for (ReaderDTO readerDTO : visitorDTO.getReaders()) {
					String readerId = readerDTO.getReaderId();
					/**
					 * If the reader from RFID and visitor's reader mismatch generate Intrusion
					 * Alarm
					 */
					if (!readerId.equalsIgnoreCase(rfid.getReader())) {
						exchange.getIn().setBody(prepareAlarmPayload(rfidVisitor, AlarmType.INTRUSION.getType()));
					}
				}
			}
		}
	}

	public void onReaderNotFound(Exchange exchange) {
		RfidVisitor rfidVisitor = exchange.getIn().getBody(RfidVisitor.class);
		if (rfidVisitor != null) {
			VisitorDetailsDTO visitorDTO = rfidVisitor.getVisitorDetailsDTO();
			if (visitorDTO.getReaders() != null && !visitorDTO.getReaders().isEmpty()) {
				exchange.getIn().setBody(prepareAlarmPayload(rfidVisitor, AlarmType.INTRUSION.getType()));
			}
		}
	}

	public void onVisitorDateExpired(Exchange exchange) {
		RfidVisitor rfidVisitor = exchange.getIn().getBody(RfidVisitor.class);
		if (rfidVisitor != null) {
			VisitorDetailsDTO visitorDTO = rfidVisitor.getVisitorDetailsDTO();
			String dateTimeFrom = visitorDTO.getDateFrom();
			String dateTimeTo = visitorDTO.getDateTo();
			Boolean timeElapsedAlarm = DateUtils.isDateInBetweenIncludingEndPoints(
					DateUtils.formattedDateWith24Hours(dateTimeFrom), DateUtils.formattedDateWith24Hours(dateTimeTo),
					new Date());
			if (!timeElapsedAlarm) {
				exchange.getIn().setBody(prepareAlarmPayload(rfidVisitor, AlarmType.TIME_ELAPSED.getType()));
			}
		}
	}

	public AlarmDTO prepareAlarmPayload(RfidVisitor rfidVisitor, String type) {
		AlarmDTO alarmDTO = new AlarmDTO();
		alarmDTO.setAlarmType(type.toUpperCase());
		alarmDTO.setReaderId(rfidVisitor.getRfidDTO().getReader());
		alarmDTO.setVrlId(rfidVisitor.getVisitorDetailsDTO().getEpc());
		alarmDTO.setStatus("Active");
		alarmDTO.setLastUpdatedTime(DateUtils.dateFormattedWith24Hours());
		alarmDTO.setCreatedDateTime(DateUtils.dateFormattedWith24Hours());
		return alarmDTO;
	}
	
	public void onAlarmPayload(Exchange exchange) {
		String json = exchange.getIn().getBody(String.class);
		exchange.getIn().setBody(json);
	}
}
