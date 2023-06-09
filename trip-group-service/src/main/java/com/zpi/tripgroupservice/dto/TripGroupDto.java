package com.zpi.tripgroupservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zpi.tripgroupservice.commons.Currency;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public record TripGroupDto(
        @NotEmpty
        @Length(max = 50)
        @JsonProperty("name")
        String name,
        @NotNull
        @JsonProperty("currency")
        Currency currency,
        @Length(max = 120)
        @JsonProperty("description")
        String description,
        @NotEmpty
        @Length(max = 100)
        @JsonProperty("destinationLocation")
        String destinationLocation
)
{
}
