package com.example.backend.Impl;

import com.example.backend.Dao.AddressMapper;
import com.example.backend.Entity.Address;
import com.example.backend.Service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public boolean addAddress(Address address) {
        int countAddress = addressMapper.countAddress(address.getUser_id());
        if( countAddress > 10){
            return false;
        }
        if (address.getIs_default() != null && address.getIs_default()) {
            // 将该用户的所有地址设为非默认地址
            addressMapper.updateDefaultFalse(address.getUser_id());
        }
        int result = addressMapper.insertAddress(address);
        return result > 0;
    }

    @Override
    public List<Address> getAddress(int userId) {
        List<Address> addressList = addressMapper.selectAddress(userId);
        List<Address> resultList = new ArrayList<>();
        Address defaultAddress = null;

        // 找出默认地址
        for (Address address : addressList) {
            if (address.getIs_default() != null && address.getIs_default()) {
                defaultAddress = address;
                break;
            }
        }

        // 如果存在默认地址，将其添加到结果列表首位
        if (defaultAddress != null) {
            resultList.add(defaultAddress);
            // 从原列表中移除默认地址
            addressList.remove(defaultAddress);
        }

        // 将剩余地址添加到结果列表
        resultList.addAll(addressList);

        return resultList;
    }

    public boolean setDefaultAddress(Address address) {
        addressMapper.updateDefaultFalse(address.getUser_id());
        int result = addressMapper.updateDefaultTrue(address.getAddress_id());
        return result > 0;
    }

    public boolean deleteAddress(int addressId) {
        // 获取要删除的地址信息
        Address addressToDelete = addressMapper.selectAddressById(addressId);
        if (addressToDelete == null) {
            return false;
        }

        // 执行删除操作
        int deleteResult = addressMapper.deleteAddress(addressId);
        if (deleteResult <= 0) {
            return false;
        }

        // 如果删除的是默认地址且该用户还有其他地址
        if (addressToDelete.getIs_default() != null && addressToDelete.getIs_default()) {
            List<Address> remainingAddresses = addressMapper.selectAddress(addressToDelete.getUser_id());
            if (!remainingAddresses.isEmpty()) {
                // 选择第一个剩余地址设为默认地址
                Address newDefaultAddress = remainingAddresses.get(0);
                addressMapper.updateDefaultFalse(newDefaultAddress.getUser_id());
                int setDefaultResult = addressMapper.updateDefaultTrue(newDefaultAddress.getAddress_id());
                return setDefaultResult > 0;
            }
        }
        return true;
    }

    public Address getAddressDetail(int addressId) {
        return addressMapper.selectAddressById(addressId);
    }

    public boolean updateAddress(Address address) {
        if (address.getIs_default() != null && address.getIs_default()) {
            // 将该用户的所有地址设为非默认地址
            addressMapper.updateDefaultFalse(address.getUser_id());
        }
        int result = addressMapper.updateAddress(address);
        return result > 0;
    }
}