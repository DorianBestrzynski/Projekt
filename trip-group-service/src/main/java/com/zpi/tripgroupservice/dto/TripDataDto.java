package com.zpi.tripgroupservice.dto;

import java.time.LocalDate;

public record TripDataDto(String destinationLocation, LocalDate startDate, LocalDate endDate, Double latitude, Double longitude) { }
