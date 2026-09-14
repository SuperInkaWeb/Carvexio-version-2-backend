package com.saas.automotriz.service;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Plan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Limites del plan gratuito (Arranque). Un negocio sin plan asignado (plan == null,
// nunca eligio ninguno) se trata igual que el plan gratuito, para no dejar limites
// sin aplicar solo porque el negocio nunca entro a "Mi Plan".
@Service
public class PlanLimitService {

    private static final String FEATURE_UPGRADE_MESSAGE =
            "Necesitas mejorar tu plan para adquirir esta funcionalidad.";
    private static final String CAPACITY_UPGRADE_MESSAGE =
            "Necesitas adquirir este plan para tener más alcance de esta funcionalidad.";

    public static final int FREE_MAX_BRANCHES = 1;
    public static final int FREE_MAX_PRODUCTS = 5;
    public static final int FREE_MAX_SERVICES = 5;
    public static final int FREE_MAX_BOOKINGS_PER_DAY = 2;

    public boolean isFreePlan(Business business) {
        Plan plan = business.getPlan();
        return plan == null || plan.getPrice() == null || plan.getPrice() <= 0;
    }

    public void checkBranchLimit(Business business, long currentActiveCount) {
        if (isFreePlan(business) && currentActiveCount >= FREE_MAX_BRANCHES) {
            throw limitReached();
        }
    }

    public void checkProductLimit(Business business, long currentActiveCount, int amountToAdd) {
        if (isFreePlan(business) && currentActiveCount + amountToAdd > FREE_MAX_PRODUCTS) {
            throw limitReached();
        }
    }

    public void checkServiceLimit(Business business, long currentActiveCount, int amountToAdd) {
        if (isFreePlan(business) && currentActiveCount + amountToAdd > FREE_MAX_SERVICES) {
            throw limitReached();
        }
    }

    public void checkBookingDailyLimit(Business business, long currentCountForDay) {
        if (isFreePlan(business) && currentCountForDay >= FREE_MAX_BOOKINGS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, CAPACITY_UPGRADE_MESSAGE);
        }
    }

    public void checkReportsAccess(Business business) {
        if (isFreePlan(business)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, FEATURE_UPGRADE_MESSAGE);
        }
    }

    public boolean hasCrmAccess(Business business) {
        Plan plan = business.getPlan();
        return plan != null && Boolean.TRUE.equals(plan.getHasCrm());
    }

    public void checkCrmDetailAccess(Business business) {
        if (!hasCrmAccess(business)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, FEATURE_UPGRADE_MESSAGE);
        }
    }

    private ResponseStatusException limitReached() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, CAPACITY_UPGRADE_MESSAGE);
    }
}
