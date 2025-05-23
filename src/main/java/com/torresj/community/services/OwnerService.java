package com.torresj.community.services;

import com.torresj.community.dtos.OwnerDto;

import java.util.List;

public interface OwnerService {
    OwnerDto create(String name, String surname);
    OwnerDto get(long ownerId);
    List<OwnerDto> get();
    void update(String name, String surname);
    void delete(long ownerId);
}
