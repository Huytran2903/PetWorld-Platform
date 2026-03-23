package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/vaccines")
@RequiredArgsConstructor
public class VaccineEligibilityController {

    private final PetVaccinationRepository petVaccinationRepository;

    /**
     * Return vaccine eligibility locks for a pet.
     *
     * For each VaccineName, we take the latest record (by AdministeredDate/CreatedAt) and
     * if NextDueDate is in the future (or NULL), we consider it "locked" until NextDueDate.
     *
     * Query param:
     * - petId (required)
     * - onDate (optional, yyyy-MM-dd). If provided, lock check uses this date; otherwise uses today.
     */
    @GetMapping("/eligibility")
    public ResponseEntity<?> getEligibility(
            @RequestParam Integer petId,
            @RequestParam(required = false) String onDate
    ) {
        LocalDate effectiveDate = parseDateOrToday(onDate);

        List<PetVaccinations> history = petVaccinationRepository
                .findByPet_PetIDOrderByAdministeredDateDescCreatedAtDesc(petId);

        // pick latest per vaccineName (case-insensitive)
        Map<String, PetVaccinations> latestByName = new LinkedHashMap<>();
        for (PetVaccinations v : history) {
            if (v == null || v.getVaccineName() == null) continue;
            String key = v.getVaccineName().trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) continue;
            latestByName.putIfAbsent(key, v);
        }

        List<Map<String, Object>> locks = new ArrayList<>();
        for (PetVaccinations latest : latestByName.values()) {
            LocalDate nextDue = latest.getNextDueDate();
            boolean locked;
            if (nextDue == null) {
                locked = true; // unknown next due => treat as locked
            } else {
                locked = effectiveDate.isBefore(nextDue);
            }

            if (locked) {
                // Map.of disallows null values; nextDueDate may be null (missing_next_due).
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("vaccineName", latest.getVaccineName());
                row.put("nextDueDate", nextDue);
                row.put("reason", nextDue == null ? "missing_next_due" : "not_due_yet");
                locks.add(row);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("petId", petId);
        body.put("onDate", effectiveDate);
        body.put("locks", locks);
        return ResponseEntity.ok(body);
    }

    private LocalDate parseDateOrToday(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception ignored) {
            return LocalDate.now();
        }
    }
}

