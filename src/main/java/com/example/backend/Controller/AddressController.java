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
    public String addAddress(@RequestBody Address address) {
        System.out.println("addAddress: " + address);
        boolean success = addressService.addAddress(address);
        if (success) {
            return "添加成功";
        } else {
            return "添加失败";
        }
    }

    @GetMapping("/getAddress/{userId}")
    public List<Address> getAddress(@PathVariable int userId) {
        return addressService.getAddress(userId);
    }

    @PutMapping("/setDefaultAddress")
    public String setDefaultAddress(@RequestBody Address address) {
        System.out.println("setDefaultAddress: " + address);
        boolean success = addressService.setDefaultAddress(address);
        if (success) {
            return "设置成功";
        } else {
            return "设置失败";
        }
    }

    @DeleteMapping("/deleteAddress/{addressId}")
    public String deleteAddress(@PathVariable int addressId) {
        boolean success = addressService.deleteAddress(addressId);
        if (success) {
            return "删除成功";
        } else {
            return "删除失败";
        }
    }

    @GetMapping("/getAddressDetail/{addressId}")
    public Address getAddressDetail(@PathVariable int addressId) {
        Address address = addressService.getAddressDetail(addressId);
        return address;
    }

    @PutMapping("/updateAddress")
    public String updateAddress(@RequestBody Address address) {
        boolean success = addressService.updateAddress(address);
        if (success) {
            return "更新成功";
        } else {
            return "更新失败";
        }
    }
}