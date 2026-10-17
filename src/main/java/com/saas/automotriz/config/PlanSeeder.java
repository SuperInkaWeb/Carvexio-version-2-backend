package com.saas.automotriz.config;

import com.saas.automotriz.model.Plan;
import com.saas.automotriz.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Asegura que existan los planes base y mantiene su descripcion (limites) al dia.
// No toca el precio ni las features de un plan que ya existe, para no afectar
// negocios que ya se suscribieron; solo agrega los que falten y actualiza la descripcion.
@Component
@Profile({"local","dev"})
public class PlanSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;

    public PlanSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) {
        List<Plan> defaults = List.of(
                plan("Arranque", "Mecanicos independientes y micro-talleres. Incluye hasta 1 usuario, 1 local, 5 productos y 5 servicios.",
                        0.0, 0.0, true, false, false, false, false),
                plan("Taller Pro", "Talleres pequenos y medianos. Incluye hasta 5 usuarios, 1 local, 100 productos y 50 servicios.",
                        149.0, 0.0, true, true, true, true, true),
                plan("Lubricentro & Grifo", "Lubricentros y centros de cambio rapido. Incluye hasta 5 usuarios, 1 local, 200 productos y 50 servicios.",
                        199.0, 0.0, true, true, true, true, true),
                plan("Multi-Sucursal", "Cadenas de talleres o grifos con 2+ locales. Incluye hasta 10 usuarios, 5 locales, 500 productos y 100 servicios.",
                        349.0, 0.0, true, true, true, true, true),
                plan("Enterprise / Flotas", "Cadenas grandes, distribuidores y aliados estrategicos. Usuarios, locales, productos y servicios ilimitados.",
                        699.0, 0.0, true, true, true, true, true)
        );

        Map<String, Plan> existing = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getName, p -> p, (a, b) -> a));

        for (Plan def : defaults) {
            Plan current = existing.get(def.getName());
            if (current == null) {
                planRepository.save(def);
            } else if (!def.getDescription().equals(current.getDescription())) {
                current.setDescription(def.getDescription());
                planRepository.save(current);
            }
        }
    }

    private Plan plan(String name, String description, Double price, Double commission,
                       boolean marketplace, boolean crm, boolean inventory, boolean reports, boolean whatsapp) {
        Plan p = new Plan();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setCommission(commission);
        p.setHasMarketplace(marketplace);
        p.setHasCrm(crm);
        p.setHasInventory(inventory);
        p.setHasReports(reports);
        p.setHasWhatsapp(whatsapp);
        p.setActive(true);
        return p;
    }
}


