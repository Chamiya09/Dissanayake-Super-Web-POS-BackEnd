package com.dissayakesuper.web_pos_backend.shift.service;

import com.dissayakesuper.web_pos_backend.shift.dto.ShiftResponse;
import com.dissayakesuper.web_pos_backend.shift.entity.Shift;
import com.dissayakesuper.web_pos_backend.shift.entity.ShiftStatus;
import com.dissayakesuper.web_pos_backend.shift.repository.ShiftRepository;
import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import com.dissayakesuper.web_pos_backend.sale.repository.SaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;

    public ShiftService(ShiftRepository shiftRepository,
                        UserRepository userRepository,
                        SaleRepository saleRepository) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
    }

    public ShiftResponse startShift(String username, BigDecimal initialCash) {
        User actor = getUserByUsername(username);

        shiftRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(actor.getId(), ShiftStatus.OPEN)
                .ifPresent(shift -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "An open shift already exists. End the current shift before starting a new one.");
                });

        Shift newShift = Shift.builder()
                .userId(actor.getId())
                .startTime(LocalDateTime.now())
                .initialCash(initialCash)
                .status(ShiftStatus.OPEN)
                .build();

        Shift saved = shiftRepository.save(newShift);
        return ShiftResponse.fromEntity(saved, 0.0);
    }

    public ShiftResponse endShift(String username, BigDecimal finalCash) {
        User actor = getUserByUsername(username);

        Shift openShift = shiftRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(actor.getId(), ShiftStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No open shift found."));

        LocalDateTime shiftEnd = LocalDateTime.now();
        Double totalSales = saleRepository.sumCompletedSalesBetween(openShift.getStartTime(), shiftEnd);

        openShift.setEndTime(shiftEnd);
        openShift.setFinalCash(finalCash);
        openShift.setStatus(ShiftStatus.CLOSED);

        Shift saved = shiftRepository.save(openShift);
        return ShiftResponse.fromEntity(saved, totalSales == null ? 0.0 : totalSales);
    }

    @Transactional(readOnly = true)
    public ShiftResponse getCurrentShift(String username) {
        User actor = getUserByUsername(username);
        Shift openShift = shiftRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(actor.getId(), ShiftStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active shift."));

        Double totalSales = saleRepository.sumCompletedSalesBetween(openShift.getStartTime(), LocalDateTime.now());
        return ShiftResponse.fromEntity(openShift, totalSales == null ? 0.0 : totalSales);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getVisibleShiftHistory(String username) {
        User actor = getUserByUsername(username);

        List<Shift> shifts = "Manager".equalsIgnoreCase(actor.getRole())
                ? shiftRepository.findAllByOrderByStartTimeDesc()
                : shiftRepository.findByUserIdOrderByStartTimeDesc(actor.getId());

        return shifts.stream()
                .map(shift -> ShiftResponse.fromEntity(shift, 0.0))
                .toList();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found."));
    }
}
