package com.shathing.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "legal_dong")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalDong {

    @Id
    @Column(name = "code", nullable = false, length = 8)
    private String code;

    @Column(name = "sido_name", nullable = false)
    private String sidoName;

    @Column(name = "sigungu_name")
    private String sigunguName;

    @Column(name = "eup_myeon_dong_name", nullable = false)
    private String eupMyeonDongName;

    public LegalDong(String code, String sidoName, String sigunguName, String eupMyeonDongName) {
        this.code = code;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.eupMyeonDongName = eupMyeonDongName;
    }
}
