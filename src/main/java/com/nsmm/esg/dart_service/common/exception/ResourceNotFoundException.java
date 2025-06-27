/**
 * @file ResourceNotFoundException.java
 * @description 요청한 리소스를 찾을 수 없을 때 발생하는 예외입니다.
 */
package com.nsmm.esg.dart_service.common.exception;

import java.io.Serial;

public class ResourceNotFoundException extends RuntimeException {
    
    @Serial
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
} 