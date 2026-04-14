package com.example.tokenapijava.Schemas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Table(name = "TOKEN_REGENERATION_TIME")
@Access(AccessType.FIELD)
public class TokenRegenerationSchema{
    @Schema(defaultValue = "1")
    @Column(name = "REGENDAYS")
    Integer days;

    @Schema(defaultValue = "15", minimum="0", maximum="23")
    @Max(value = 23, message = "Hours must be between 0 and 23")
    @Min(value = 0, message = "Hours must be between 0 and 23")
    @Column(name = "REGENHOURS")
    Integer hours;

    @Schema(defaultValue = "0", minimum="0", maximum="59")
    @Max(value = 59, message = "Minutes must be between 0 and 59")
    @Min(value = 0, message = "Minutes must be between 0 and 59")
    @Column(name = "REGENMINS")
    Integer mins;

}