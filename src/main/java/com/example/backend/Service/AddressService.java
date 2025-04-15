package com.example.backend.Service;

import com.example.backend.Entity.Address;

import java.util.List;

public interface AddressService {
    boolean addAddress(Address address);

    List<Address> getAddress(int userId);

    boolean setDefaultAddress(Address address);

    boolean deleteAddress(int addressId);

    Address getAddressDetail(int addressId);

    boolean updateAddress(Address address);
}