package com.example.tokenapijava.Schemas;

import org.springframework.data.relational.core.mapping.Column;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TokenRegenerationSchema(
    @Schema(defaultValue = "1")
    @Column("REGENDAYS")
    Integer days, 

    @Schema(defaultValue = "15", minimum="0", maximum="23")
    @Max(value = 23, message = "Hours must be between 0 and 23")
    @Min(value = 0, message = "Hours must be between 0 and 23")
    @Column("REGENHOURS")
    Integer hours, 

    @Schema(defaultValue = "0", minimum="0", maximum="59")
    @Max(value = 59, message = "Minutes must be between 0 and 59")
    @Min(value = 0, message = "Minutes must be between 0 and 59")
    @Column("REGENMINS")
    Integer mins
){}