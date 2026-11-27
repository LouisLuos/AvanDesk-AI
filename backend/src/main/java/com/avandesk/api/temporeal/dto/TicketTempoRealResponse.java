package com.avandesk.api.temporeal.dto;

import java.time.Instant;

public record TicketTempoRealResponse(
    String ticket,
    Instant expiraEm
) {
}
