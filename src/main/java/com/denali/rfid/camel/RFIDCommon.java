/**
 * 
 */
package com.denali.rfid.camel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.camel.Exchange;
import org.elasticsearch.ElasticsearchGenerationException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.denali.rfid.dto.ReaderDTO;
import com.denali.rfid.dto.RfidDTO;
import com.denali.rfid.utils.DateUtils;
import com.denali.rfid.utils.RFIDParserUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author zentere
 *
 */
@Component
public class RFIDCommon {

	public void processRfid(Exchange exchange) {
		String data = exchange.getIn().getBody(String.class);
		exchange.getIn().setBody(RFIDParserUtils.onParse(data));
	}
	
	public void onConvertToElasticMap(Exchange exchange) {
		RfidDTO data = exchange.getIn().getBody(RfidDTO.class);
		Map<String, Object> rfid = new HashMap<String, Object>();
		rfid.put("reader", data.getReader());
		rfid.put("antenna", data.getAntenna());
		rfid.put("cssRssi", data.getCssRssi());
		rfid.put("epc", data.getEpc());
		rfid.put("lastUpdatedTime", DateUtils.dateFormattedWith24Hours());
		IndexRequest indexRequest = new IndexRequest("rfid_idx", "rfid", generateId(data));
		indexRequest.source(rfid);
		exchange.getIn().setBody(indexRequest);
		exchange.getIn().setHeader("indexName", "rfid_idx");
	}
	
	public void onConvertReaderElasticMap(Exchange exchange) {
		try {
			String dataFromOdoo = exchange.getIn().getBody(String.class);
			JSONArray jsonArray = new JSONArray(dataFromOdoo);
			List<ReaderDTO> bulkReaders = new ArrayList<>();
			BulkRequest bulkRequest = new BulkRequest();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject reader = jsonArray.getJSONObject(i);
				ObjectMapper mapper = new ObjectMapper();
				ReaderDTO readerDto = mapper.readValue(reader.toString(), ReaderDTO.class);
				bulkReaders.add(readerDto);
				ObjectMapper oMapper = new ObjectMapper();
				@SuppressWarnings("unchecked")
				Map<String, Object> map = oMapper.convertValue(readerDto, Map.class);
				IndexRequest indexRequest = new IndexRequest("reader_idx", "reader");
				indexRequest.source(map);
				bulkRequest.add(indexRequest);
			}
			exchange.getIn().setBody(bulkRequest);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (ElasticsearchGenerationException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String generateId(RfidDTO rfid) {
		return rfid.getReader() + "-" + rfid.getAntenna() + "-" + rfid.getEpc() + "-" + rfid.getCssRssi() +"-" + generateRandomString();
	}
	
	private String generateRandomString() {
		int leftLimit = 97; // letter 'a'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 10;
		Random random = new Random();
		StringBuilder buffer = new StringBuilder(targetStringLength);
		for (int i = 0; i < targetStringLength; i++) {
			int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
			buffer.append((char) randomLimitedInt);
		}
		String generatedString = buffer.toString();
		return generatedString.substring(0, 4);
	}

	public void onReaderSearchRequest(Exchange exchange) {
		RfidDTO data = exchange.getIn().getBody(RfidDTO.class);
		SearchRequest search = new SearchRequest();
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.should(QueryBuilders.termQuery("reader_id", data.getReader()));
		sourceBuilder.query(boolQuery);
		search.source(sourceBuilder);
		exchange.getIn().setBody(search);
	}
	
	public void onVerifyReader(Exchange exchange) {
		String dataFromElastic = exchange.getIn().getBody(String.class);
		try {
			JSONObject fromEs = new JSONObject(dataFromElastic);
			JSONObject hits = fromEs.getJSONObject("hits");
			JSONArray hitsResult = hits.getJSONArray("hits");
			if (hitsResult != null && hitsResult.length() > 0) {
				JSONObject jsonObject = hitsResult.getJSONObject(0);
				JSONObject rfid = jsonObject.getJSONObject("_source");
				ObjectMapper mapper = new ObjectMapper();
				RfidDTO rfidDTO = mapper.readValue(rfid.toString(), RfidDTO.class);
				exchange.getIn().setBody(rfidDTO);
			} else {
				exchange.getIn().setBody("RFID");
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
}
