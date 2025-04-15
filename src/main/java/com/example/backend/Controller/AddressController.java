package com.example.backend.Controller;

import com.example.backend.Entity.Address;
import com.example.backend.Service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @PostMapping("/addAddress")
    public ResponseEntity<Map<String, Boolean>> addAddress(@RequestBody Address address) {
        System.out.println("addAddress: " + address);
        boolean success = addressService.addAddress(address);
        Map<String, Boolean> result = new HashMap<>();
        result.put("success", success);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getAddress/{userId}")
    public ResponseEntity<List<Address>> getAddress(@PathVariable int userId) {
        List<Address> address = addressService.getAddress(userId);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/setDefaultAddress")
    public ResponseEntity<Map<String, Boolean>> setDefaultAddress(@RequestBody Address address) {
        System.out.println("setDefaultAddress: " + address);
        boolean success = addressService.setDefaultAddress(address);
        Map<String, Boolean> result = new HashMap<>();
        result.put("success", success);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/deleteAddress/{addressId}")
    public ResponseEntity<Map<String, Boolean>> deleteAddress(@PathVariable int addressId) {
        boolean success = addressService.deleteAddress(addressId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("success", success);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getAddressDetail/{addressId}")
    public ResponseEntity<Address> getAddressDetail(@PathVariable int addressId) {
        Address address = addressService.getAddressDetail(addressId);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/updateAddress")
    public ResponseEntity<Map<String, Boolean>> updateAddress(@RequestBody Address address) {
        boolean success = addressService.updateAddress(address);
        Map<String, Boolean> result = new HashMap<>();
        result.put("success", success);
        return ResponseEntity.ok(result);
    }
}