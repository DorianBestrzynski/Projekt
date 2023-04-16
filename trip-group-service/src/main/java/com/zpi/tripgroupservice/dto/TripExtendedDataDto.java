package com.zpi.tripgroupservice.dto;

import com.zpi.tripgroupservice.commons.Currency;
import com.zpi.tripgroupservice.commons.GroupStage;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record TripExtendedDataDto(Long groupId, String name, Currency currency, String description,
                                  String destinationLocation, LocalDate startDate, LocalDate endDate,
                                  Double latitude, Double longitude, GroupStage groupStage, Integer participantsNum) {
}
