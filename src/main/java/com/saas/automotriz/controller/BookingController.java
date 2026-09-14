package com.saas.automotriz.controller;

import com.saas.automotriz.dto.BookingDTO;
import com.saas.automotriz.dto.VehicleDTO;
import com.saas.automotriz.repository.*;
import com.saas.automotriz.request.BookingRequest;
import com.saas.automotriz.model.*;
import com.saas.automotriz.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final ZoneId PERU_ZONE = ZoneId.of("America/Lima");

    private final BookingRepository bookingRepository;
    private final BranchAvailabilityRepository branchAvailabilityRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final com.saas.automotriz.repository.BranchRepository branchRepository;
    private final PlanLimitService planLimitService;
    private final VehicleRepository vehicleRepository;

    // cliente crea reserva
    @PostMapping
    @Transactional
    public ResponseEntity<?> createBooking(@AuthenticationPrincipal User user,
                                           @RequestBody BookingRequest request) {
        if (request.getDate() == null || request.getTime() == null) {
            return ResponseEntity.badRequest().body("Debes seleccionar una fecha y un horario");
        }
        if (!isFutureSlot(request.getDate(), request.getTime())) {
            return ResponseEntity.badRequest().body("No puedes reservar fechas pasadas ni horarios que ya transcurrieron en Perú");
        }
        AutoService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        Branch branch = resolveBranch(service, request.getLocalId());

        if (request.getDate() != null) {
            planLimitService.checkBookingDailyLimit(service.getBusiness(),
                    bookingRepository.countByBusinessAndDate(service.getBusiness(), request.getDate()));
        }

        if (request.getDate() != null && request.getTime() != null) {
            boolean slotAvailable = branch != null
                    ? isSlotAvailable(branch, service.getBusiness(), request.getDate(), request.getTime())
                    : isBusinessSlotAvailable(service.getBusiness(), request.getDate(), request.getTime());
            if (!slotAvailable) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("El horario seleccionado ya no está disponible");
            }
        }

        Booking booking = new Booking();
        booking.setClient(user);
        booking.setService(service);
        booking.setBusiness(service.getBusiness());
        booking.setDate(request.getDate());
        booking.setTime(request.getTime());
        booking.setNotes(request.getNotes());
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findByIdAndClient(request.getVehicleId(), user)
                    .orElseThrow(() -> new RuntimeException("Auto no encontrado"));
            booking.setVehicle(vehicle);
        }
        if (branch != null) {
            booking.setBranch(branch);
        }

        return ResponseEntity.ok(toDTO(bookingRepository.save(booking)));
    }

    // cliente ve sus reservas
    @GetMapping("/my")
    public ResponseEntity<List<BookingDTO>> getMyBookings(@AuthenticationPrincipal User user) {
        List<BookingDTO> bookings = bookingRepository.findByClient(user)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(bookings);
    }

    // cliente cancela su reserva
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingDTO> cancelBooking(@AuthenticationPrincipal User user,
                                                    @PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!booking.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return ResponseEntity.badRequest().build();
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return ResponseEntity.ok(toDTO(bookingRepository.save(booking)));
    }

    // negocio ve todas sus reservas
    @GetMapping("/business")
    public ResponseEntity<List<BookingDTO>> getBusinessBookings(@AuthenticationPrincipal User user,
                                                                @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        List<Booking> list;
        if (localId != null) {
            list = bookingRepository.findByBusinessAndBranchId(business, localId);
        } else {
            list = bookingRepository.findByBusiness(business);
        }
        List<BookingDTO> bookings = list.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(bookings);
    }

    // negocio ve reservas por fecha
    @GetMapping("/business/date")
    public ResponseEntity<List<BookingDTO>> getBookingsByDate(@AuthenticationPrincipal User user,
                                                              @RequestParam LocalDate date,
                                                              @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        List<Booking> list;
        if (localId != null) {
            list = bookingRepository.findByBusinessAndBranchIdAndDate(business, localId, date);
        } else {
            list = bookingRepository.findByBusinessAndDate(business, date);
        }
        List<BookingDTO> bookings = list.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<String>> getAvailableSlots(@RequestParam(required = false) Long branchId,
                                                          @RequestParam(required = false) Long businessId,
                                                          @RequestParam LocalDate date) {
        if (date.isBefore(LocalDate.now(PERU_ZONE))) {
            return ResponseEntity.ok(List.of());
        }
        Branch branch = null;
        Business business = null;
        List<BranchAvailability> availabilities;

        if (branchId != null) {
            branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
            availabilities = branchAvailabilityRepository.findByBranchAndDayOfWeek(branch, date.getDayOfWeek().name());
        } else if (businessId != null) {
            business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
            availabilities = branchAvailabilityRepository.findByBusinessAndDayOfWeek(business, date.getDayOfWeek().name());
        } else {
            return ResponseEntity.badRequest().build();
        }

        if (availabilities.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        Set<String> slots = new LinkedHashSet<>();
        List<BookingStatus> occupiedStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

        for (BranchAvailability availability : availabilities.stream()
                .sorted(Comparator.comparing(BranchAvailability::getStartTime))
                .toList()) {
            if (availability.getStartTime() == null || availability.getEndTime() == null) {
                continue;
            }

            LocalTime cursor = availability.getStartTime();
            while (cursor.plusMinutes(30).compareTo(availability.getEndTime()) <= 0) {
                if (!isFutureSlot(date, cursor)) {
                    cursor = cursor.plusMinutes(30);
                    continue;
                }
                Long occupied = branch != null
                        ? bookingRepository.countByBusinessAndBranchIdAndDateAndTimeAndStatusIn(
                        branch.getBusiness(),
                        branch.getId(),
                        date,
                        cursor,
                        occupiedStatuses
                )
                        : bookingRepository.countByBusinessAndDateAndTimeAndStatusInAndBranchIsNull(
                        business,
                        date,
                        cursor,
                        occupiedStatuses
                );
                if (occupied < (availability.getCapacity() == null || availability.getCapacity() < 1 ? 1 : availability.getCapacity())) {
                    slots.add(cursor.toString().substring(0, 5));
                }
                cursor = cursor.plusMinutes(30);
            }
        }

        return ResponseEntity.ok(new ArrayList<>(slots));
    }

    // negocio confirma reserva
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingDTO> confirmBooking(@AuthenticationPrincipal User user,
                                                     @PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!booking.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return ResponseEntity.ok(toDTO(bookingRepository.save(booking)));
    }

    // negocio cancela reserva
    @PutMapping("/{id}/cancel-business")
    public ResponseEntity<BookingDTO> cancelByBusiness(@AuthenticationPrincipal User user,
                                                       @PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!booking.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return ResponseEntity.ok(toDTO(bookingRepository.save(booking)));
    }

    // negocio marca reserva como completada
    @PutMapping("/{id}/complete")
    public ResponseEntity<BookingDTO> completeBooking(@AuthenticationPrincipal User user,
                                                      @PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!booking.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        booking.setStatus(BookingStatus.COMPLETED);
        return ResponseEntity.ok(toDTO(bookingRepository.save(booking)));
    }

    private BookingDTO toDTO(Booking b) {
        BookingDTO dto = new BookingDTO();
        dto.setId(b.getId());
        dto.setServiceName(b.getService().getName());
        dto.setBusinessName(b.getBusiness().getName());
        dto.setDate(b.getDate());
        dto.setTime(b.getTime());
        dto.setStatus(b.getStatus().name());
        dto.setNotes(b.getNotes());
        if (b.getBranch() != null) dto.setLocalId(b.getBranch().getId());
        if (b.getService() != null) dto.setServicePrice(b.getService().getPrice());
        if (b.getClient() != null) dto.setClientName(b.getClient().getName());
        dto.setCreatedAt(b.getCreatedAt());
        if (b.getVehicle() != null) {
            VehicleDTO vehicle = new VehicleDTO();
            vehicle.setId(b.getVehicle().getId());
            vehicle.setVehicleType(b.getVehicle().getVehicleType());
            vehicle.setPlate(b.getVehicle().getPlate());
            vehicle.setVin(b.getVehicle().getVin());
            vehicle.setMileage(b.getVehicle().getMileage());
            vehicle.setYearsOfUse(b.getVehicle().getYearsOfUse());
            dto.setVehicle(vehicle);
        }
        return dto;
    }

    private Branch resolveBranch(AutoService service, Long localId) {
        if (localId != null) {
            return branchRepository.findById(localId)
                    .filter(branch -> branch.getBusiness().getId().equals(service.getBusiness().getId()))
                    .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        }
        return service.getBranch();
    }

    private boolean isSlotAvailable(Branch branch, Business business, LocalDate date, LocalTime time) {
        if (!isFutureSlot(date, time)) {
            return false;
        }
        List<BranchAvailability> availabilities = branchAvailabilityRepository.findByBranchAndDayOfWeek(branch, date.getDayOfWeek().name());
        if (availabilities.isEmpty()) {
            return false;
        }

        List<BookingStatus> occupiedStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
        for (BranchAvailability availability : availabilities) {
            if (availability.getStartTime() == null || availability.getEndTime() == null) {
                continue;
            }

            boolean insideWindow = !time.isBefore(availability.getStartTime())
                    && time.plusMinutes(30).compareTo(availability.getEndTime()) <= 0;

            if (!insideWindow) {
                continue;
            }

            Long occupied = bookingRepository.countByBusinessAndBranchIdAndDateAndTimeAndStatusIn(
                    business,
                    branch.getId(),
                    date,
                    time,
                    occupiedStatuses
            );
            int capacity = availability.getCapacity() == null || availability.getCapacity() < 1 ? 1 : availability.getCapacity();
            return occupied < capacity;
        }

        return false;
    }

    private boolean isBusinessSlotAvailable(Business business, LocalDate date, LocalTime time) {
        if (!isFutureSlot(date, time)) {
            return false;
        }
        List<BranchAvailability> availabilities = branchAvailabilityRepository.findByBusinessAndDayOfWeek(business, date.getDayOfWeek().name());
        if (availabilities.isEmpty()) {
            return false;
        }

        List<BookingStatus> occupiedStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
        for (BranchAvailability availability : availabilities) {
            if (availability.getStartTime() == null || availability.getEndTime() == null) {
                continue;
            }

            boolean insideWindow = !time.isBefore(availability.getStartTime())
                    && time.plusMinutes(30).compareTo(availability.getEndTime()) <= 0;

            if (!insideWindow) {
                continue;
            }

            Long occupied = bookingRepository.countByBusinessAndDateAndTimeAndStatusInAndBranchIsNull(
                    business,
                    date,
                    time,
                    occupiedStatuses
            );
            int capacity = availability.getCapacity() == null || availability.getCapacity() < 1 ? 1 : availability.getCapacity();
            return occupied < capacity;
        }

        return false;
    }

    private boolean isFutureSlot(LocalDate date, LocalTime time) {
        LocalDate today = LocalDate.now(PERU_ZONE);
        if (date.isAfter(today)) return true;
        if (date.isBefore(today)) return false;
        return time.isAfter(LocalTime.now(PERU_ZONE));
    }
}
