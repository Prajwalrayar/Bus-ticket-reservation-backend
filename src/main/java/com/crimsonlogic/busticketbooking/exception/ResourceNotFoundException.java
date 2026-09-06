package com.crimsonlogic.busticketbooking.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String entityName;
    private final Object id;

    public ResourceNotFoundException(String message) {
        super(message);
        this.entityName = null;
        this.id = null;
    }

    public ResourceNotFoundException(String entityName, Object id) {
        super(String.format("%s not found with id: %s", entityName, id));
        this.entityName = entityName;
        this.id = id;
    }

    public ResourceNotFoundException(String message, String entityName, Object id) {
        super(message);
        this.entityName = entityName;
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getId() {
        return id;
    }
}
