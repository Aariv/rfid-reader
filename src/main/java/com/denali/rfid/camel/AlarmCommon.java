/**
 * 
 */
package com.denali.rfid.camel;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.denali.rfid.dto.AlarmDTO;
import com.denali.rfid.dto.AlarmType;
import com.denali.rfid.dto.ReaderDTO;
import com.denali.rfid.dto.RfidDTO;
import com.denali.rfid.dto.RfidVisitor;
import com.denali.rfid.dto.VisitorDetailsDTO;
import com.denali.rfid.utils.DateUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author zentere
 *
 */
@Component
public class AlarmCommon {
	
	@Value("${alarm.index}")
	private String alarmIndex;
	
	@Value("${alarm.index.type}")
	private String alarmType;
	
	public void onConvertAlarmToElastic(Exchange exchange) {
		AlarmDTO data = (AlarmDTO) exchange.getProperty("ALARM");
		Map<String, Object> alarm = new HashMap<String, Object>();
		alarm.put("alarmType", data.getAlarmType());
		alarm.put("readerId", data.getReaderId());
		alarm.put("status", data.getStatus());
		alarm.put("lastUpdatedTime", DateUtils.dateFormattedWith24Hours());
		alarm.put("createdDateTime", DateUtils.dateFormattedWith24Hours());
		alarm.put("vrlId", data.getVrlId());
		alarm.put("alarmSent", true);
		IndexRequest indexRequest = new IndexRequest(alarmIndex, "alarm", generateId(data));
		indexRequest.source(alarm);
		exchange.getIn().setBody(indexRequest);
		exchange.getIn().setHeader("indexName", alarmIndex);
	}
	
	public void onSearchAlarm(Exchange exchange) {
		AlarmDTO data = (AlarmDTO) exchange.getProperty("ALARM");
		SearchRequest search = new SearchRequest();
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.should(QueryBuilders.termQuery("_id", generateId(data)));
		boolQuery.should(QueryBuilders.termQuery("alarmSent", false));
		sourceBuilder.query(boolQuery);
		search.source(sourceBuilder);
		exchange.getIn().setBody(search);
	}
	
	public void validateAlarm(Exchange exchange) {
		String dataFromElastic = exchange.getIn().getBody(String.class);
		try {
			JSONObject fromEs = new JSONObject(dataFromElastic);
			JSONObject hits = fromEs.getJSONObject("hits");
			JSONArray hitsResult = hits.getJSONArray("hits");
			if (hitsResult != null && hitsResult.length() > 0) {
				JSONObject jsonObject = hitsResult.getJSONObject(0);
				JSONObject alarm = jsonObject.getJSONObject("_source");
				ObjectMapper mapper = new ObjectMapper();
				AlarmDTO alarmDto = mapper.readValue(alarm.toString(), AlarmDTO.class);
				exchange.getIn().setBody(alarmDto);
			} else {
				exchange.getIn().setBody("NotFound");
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String generateId(AlarmDTO alarmDTO) {
		return alarmDTO.getReaderId() + "-"+ alarmDTO.getVrlId() +"-"+ alarmDTO.getAlarmType().replaceAll(" ", "-");
	}

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
