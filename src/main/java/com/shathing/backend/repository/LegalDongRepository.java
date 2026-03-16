package com.shathing.backend.repository;

import com.shathing.backend.entity.LegalDong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LegalDongRepository extends JpaRepository<LegalDong, String> {

    @Query("""
            select distinct substring(l.code, 1, 2), l.sidoName
            from LegalDong l
            order by l.sidoName
            """)
    List<Object[]> findSidoItems();

    @Query("""
            select distinct substring(l.code, 1, 5), l.sigunguName
            from LegalDong l
            where substring(l.code, 1, 2) = :sidoCode
              and l.sigunguName is not null
            order by l.sigunguName
            """)
    List<Object[]> findSigunguItemsBySidoCode(String sidoCode);

    @Query("""
            select distinct l.code, l.eupMyeonDongName
            from LegalDong l
            where substring(l.code, 1, 5) = :sigunguCode
            order by l.eupMyeonDongName
            """)
    List<Object[]> findEupMyeonDongItemsBySigunguCode(String sigunguCode);
}
