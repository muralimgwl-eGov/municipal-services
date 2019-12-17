package org.egov.wsCalculation.model;

import javax.validation.constraints.NotNull;



import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Document {
	
        @JsonProperty("id")
        private String id ;
        
        @JsonProperty("tenantId")
        private String tenantId ;
        
        @JsonProperty("documentType")
        @NotNull
        private String documentType ;

        @JsonProperty("fileStore")
        @NotNull
        private String fileStore ;
        
        @JsonProperty("documentUid")
        @NotNull
        private String documentUid ;
        
        @JsonProperty("auditDetails")
        private AuditDetails auditDetails;

        @JsonProperty("status")
    	private Status status;
}

