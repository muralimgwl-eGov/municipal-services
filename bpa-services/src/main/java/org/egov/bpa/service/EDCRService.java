package org.egov.bpa.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.validator.MDMSValidator;
import org.egov.bpa.web.models.BPA;
import org.egov.bpa.web.models.BPA.RiskTypeEnum;
import org.egov.bpa.web.models.BPARequest;
import org.egov.bpa.web.models.edcr.RequestInfoWrapper;
import org.egov.bpa.web.models.edcr.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

import net.minidev.json.JSONArray;

@Service
public class EDCRService {

	private ServiceRequestRepository serviceRequestRepository;

	private ObjectMapper mapper;

	private BPAConfiguration config;
	
	private String edcrTenantId="jupiter";
	
	@Autowired
	private MDMSValidator mdmsValidator;

	@Autowired
	public EDCRService(ServiceRequestRepository serviceRequestRepository,
			ObjectMapper mapper, BPAConfiguration config) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mapper = mapper;
		this.config = config;
	}

	/**
	 * Validates the EDCR Plan based on the edcr Number and the RiskType
	 * 
	 * @param request
	 *            BPARequest for create
	 * 
	 */
	public void validateEdcrPlan(BPARequest request, Object mdmsData) {

		String edcrNo = request.getBPA().getEdcrNumber();
		RiskTypeEnum riskType = request.getBPA().getRiskType();
		StringBuilder uri = new StringBuilder(config.getEdcrHost());
		BPA bpa = request.getBPA();
		
		uri.append(config.getGetPlanEndPoint());
		uri.append("?").append("tenantId=").append(this.edcrTenantId);
		uri.append("&").append("edcrNumber=").append(edcrNo);
		RequestInfo edcrRequestInfo = new RequestInfo();
		BeanUtils.copyProperties(request.getRequestInfo(), edcrRequestInfo);
		Map<String, List<String>> masterData = mdmsValidator.getAttributeValues(mdmsData);
		LinkedHashMap responseMap = null;
		try {
			 responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(uri,
					new RequestInfoWrapper(edcrRequestInfo));
		}catch( ServiceCallException se) {
			throw new CustomException("EDCR ERROR", " EDCR Number is Invalid");
		}
		
		if (CollectionUtils.isEmpty(responseMap))
			throw new CustomException("EDCR ERROR", "The response from EDCR service is empty or null");

		String jsonString = new JSONObject(responseMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		List<String> edcrStatus = context.read("edcrDetail.*.status");
		List<String> OccupancyTypes = context.read("edcrDetail.*.planDetail.virtualBuilding.occupancyTypes.*.type.code");
		TypeRef<List<Double>> typeRef = new TypeRef<List<Double>>() {};
		List<Double> plotAreas = context.read("edcrDetail.*.planDetail.plot.area",typeRef);
		List<Double> buildingHeights = context.read("edcrDetail.*.planDetail.blocks.*.building.buildingHeight",typeRef);

		if (CollectionUtils.isEmpty(edcrStatus) || !edcrStatus.get(0).equalsIgnoreCase("Accepted")) {
			throw new CustomException("INVALID EDCR NUMBER",
					"The EDCR Number is not Accepted " + edcrNo);
		}
		RiskTypeEnum expectedRiskType ;
		if ( !CollectionUtils.isEmpty(OccupancyTypes) &&  !CollectionUtils.isEmpty(plotAreas) && !CollectionUtils.isEmpty(buildingHeights)) {
			Double buildingHeight = Collections.max(buildingHeights);
			String OccupancyType = OccupancyTypes.get(0); // Assuming OccupancyType would be same in the list
			Double plotArea = plotAreas.get(0);
//			
//			String filterExp = "$.[?(@.fromPlotArea > '"+plotArea+"' && @.toPlotArea <= '"+plotArea+"' ) || ( @.fromBuildingHeight>'"+buildingHeight+"'  && @.toBuildingHeight <= '"+buildingHeight+"' )].RiskType";
//			List<String> riskTypes = JsonPath.read(masterData.get(BPAConstants.RISKTYPE_COMPUTATION), filterExp);
//			
//			
//			if( !CollectionUtils.isEmpty(riskTypes) && 
//					OccupancyType.equals(BPAConstants.RESIDENTIAL_OCCUPANCY) ) {
//				expectedRiskType = (RiskTypeEnum) riskTypes.get(0);
//				
//				if(expectedRiskType == null || !expectedRiskType.equals(riskType) ) {
//					throw new CustomException("INVALID RISK TYPE",
//							"The Risk Type is not valid " + riskType);
//				}
//			}else {
//				throw new CustomException("INVALID OccupancyType",
//						"The OccupancyType " + OccupancyType + " is not supported! " );
//			}
		}
	}
	
		
	
	
	
}
