package com.example.backend.Service;

import com.example.backend.Entity.ExpressRequest;
import org.springframework.http.ResponseEntity;

public interface ExpressService {
    ResponseEntity<?>  queryExpress(ExpressRequest request);
}
