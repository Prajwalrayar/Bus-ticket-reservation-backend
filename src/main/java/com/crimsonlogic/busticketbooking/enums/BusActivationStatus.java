package com.crimsonlogic.busticketbooking.enums;

public enum BusActivationStatus {
    NONE,       // default - no pending request
    PENDING,    // operator submitted a request, awaiting admin
    APPROVED,   // admin approved, awaiting operator payment
    REJECTED    // admin rejected
}
