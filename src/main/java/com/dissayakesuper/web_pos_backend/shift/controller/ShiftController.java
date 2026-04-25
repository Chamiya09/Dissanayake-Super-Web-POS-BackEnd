package com.dissayakesuper.web_pos_backend.shift.controller;

import com.dissayakesuper.web_pos_backend.shift.dto.EndShiftRequest;
import com.dissayakesuper.web_pos_backend.shift.dto.ShiftResponse;
import com.dissayakesuper.web_pos_backend.shift.dto.StartShiftRequest;
import com.dissayakesuper.web_pos_backend.shift.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@PreAuthorize("hasAnyRole('STAFF','MANAGER')")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping("/start")
    public ResponseEntity<ShiftResponse> startShift(@Valid @RequestBody StartShiftRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(shiftService.startShift(authentication.getName(), request.initialCash()));
    }

    @PostMapping("/end")
    public ResponseEntity<ShiftResponse> endShift(@Valid @RequestBody EndShiftRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(shiftService.endShift(authentication.getName(), request.finalCash()));
    }

    @GetMapping("/current")
    public ResponseEntity<ShiftResponse> currentShift(Authentication authentication) {
        return ResponseEntity.ok(shiftService.getCurrentShift(authentication.getName()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ShiftResponse>> shiftHistory(Authentication authentication) {
        return ResponseEntity.ok(shiftService.getVisibleShiftHistory(authentication.getName()));
    }
}
