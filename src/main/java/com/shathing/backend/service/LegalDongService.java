package com.shathing.backend.service;

import com.shathing.backend.dto.response.LegalDongResponse;
import com.shathing.backend.repository.LegalDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalDongService {

    private final LegalDongRepository legalDongRepository;

    @Transactional(readOnly = true)
    public LegalDongResponse getLegalDongs(String code) {
        if (code == null || code.isBlank()) {
            return new LegalDongResponse(mapItems(legalDongRepository.findSidoItems()));
        }
        if (code.length() == 2) {
            return new LegalDongResponse(mapItems(legalDongRepository.findSigunguItemsBySidoCode(code)));
        }
        if (code.length() == 5) {
            return new LegalDongResponse(mapItems(legalDongRepository.findEupMyeonDongItemsBySigunguCode(code)));
        }
        throw new IllegalArgumentException("code must be empty, 2 digits, or 5 digits.");
    }

    private List<LegalDongResponse.LegalDongItem> mapItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new LegalDongResponse.LegalDongItem((String) row[0], (String) row[1]))
                .toList();
    }
}
