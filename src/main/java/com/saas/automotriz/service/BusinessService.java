package com.saas.automotriz.service;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;

    public Business getCurrentBusiness() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
    }
}