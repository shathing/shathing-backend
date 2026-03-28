package com.shathing.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    private static final String UNITED_STATES_COUNTRY_CODE = "US";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_kr", unique = true)
    private String nameKr;

    @Column(name = "name_us")
    private String nameUs;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public Category(String nameKr, String nameUs, int displayOrder) {
        this.nameKr = nameKr;
        this.nameUs = nameUs;
        this.displayOrder = displayOrder;
    }

    public void updateNames(String nameKr, String nameUs) {
        this.nameKr = nameKr;
        this.nameUs = nameUs;
    }

    public String getDisplayName(String countryCode) {
        if (UNITED_STATES_COUNTRY_CODE.equalsIgnoreCase(countryCode) && nameUs != null && !nameUs.isBlank()) {
            return nameUs;
        }
        return nameKr;
    }
}
