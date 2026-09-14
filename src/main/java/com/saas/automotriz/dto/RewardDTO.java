package com.saas.automotriz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RewardDTO {
    private int points;
    private int activeDiscountPercent;
}
