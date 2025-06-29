package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.Address;
import com.uade.bookybe.core.model.User;
import com.uade.bookybe.infraestructure.entity.AddressEntity;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserEntityMapper {
  UserEntityMapper INSTANCE = Mappers.getMapper(UserEntityMapper.class);

  User toModel(UserEntity entity);

  UserEntity toEntity(User model);

  default Address mapAddressEntityToAddress(AddressEntity addressEntity) {
    if (addressEntity == null) return null;

    return Address.builder()
        .id(addressEntity.getId())
        .state(addressEntity.getState())
        .country(addressEntity.getCountry())
        .latitude(addressEntity.getLatitude())
        .longitude(addressEntity.getLongitude())
        .build();
  }

  default AddressEntity mapAddressToAddressEntity(Address address) {
    if (address == null) return null;

    AddressEntity addressEntity = new AddressEntity();
    addressEntity.setId(address.getId());
    addressEntity.setState(address.getState());
    addressEntity.setCountry(address.getCountry());
    addressEntity.setLatitude(address.getLatitude());
    addressEntity.setLongitude(address.getLongitude());
    return addressEntity;
  }
}
