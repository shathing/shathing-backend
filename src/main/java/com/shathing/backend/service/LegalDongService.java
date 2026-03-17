package com.shathing.backend.service;

import com.shathing.backend.dto.response.LegalDongItemResponse;
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
    public List<LegalDongItemResponse> getLegalDongs(String code) {
        if (code == null || code.isBlank()) {
            return mapItems(legalDongRepository.findSidoItems());
        }
        if (code.length() == 2) {
            return mapItems(legalDongRepository.findSigunguItemsBySidoCode(code));
        }
        if (code.length() == 5) {
            return mapItems(legalDongRepository.findEupMyeonDongItemsBySigunguCode(code));
        }
        throw new IllegalArgumentException("code must be empty, 2 digits, or 5 digits.");
    }

    private List<LegalDongItemResponse> mapItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new LegalDongItemResponse((String) row[0], (String) row[1]))
                .toList();
    }
}
