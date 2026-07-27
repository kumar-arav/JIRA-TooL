package com.flowsync.dto.request;
import com.flowsync.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class UpdateTicketStatusRequest {
    @NotNull private TicketStatus status;
    private String closureNotes;
    private String closureProofUrl;
}
