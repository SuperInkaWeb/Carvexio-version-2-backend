package com.saas.automotriz.controller;

import com.saas.automotriz.dto.RewardDTO;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.UserRepository;
import com.saas.automotriz.request.RewardRedeemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {
    private static final int POINTS_FOR_5_PERCENT = 400;
    private static final int POINTS_FOR_10_PERCENT = 1100;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public RewardDTO getRewards(@AuthenticationPrincipal User user) {
        return new RewardDTO(points(user), activeDiscount(user));
    }

    @PostMapping("/redeem")
    public ResponseEntity<RewardDTO> redeem(@AuthenticationPrincipal User user, @RequestBody RewardRedeemRequest request) {
        int discount = request.getDiscountPercent() == null ? 0 : request.getDiscountPercent();
        int cost = discount == 5 ? POINTS_FOR_5_PERCENT : discount == 10 ? POINTS_FOR_10_PERCENT : 0;
        if (cost == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recompensa no válida.");
        if (activeDiscount(user) > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya tienes un descuento activo para tu próxima compra.");
        if (points(user) < cost) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tienes puntos suficientes para reclamar esta recompensa.");
        user.setRewardPoints(points(user) - cost);
        user.setActiveRewardDiscount(discount);
        userRepository.save(user);
        return ResponseEntity.ok(new RewardDTO(points(user), discount));
    }

    private int points(User user) { return user.getRewardPoints() == null ? 0 : user.getRewardPoints(); }
    private int activeDiscount(User user) { return user.getActiveRewardDiscount() == null ? 0 : user.getActiveRewardDiscount(); }
}
