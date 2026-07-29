package com.kontora.pos.cortesias.repository;

import com.kontora.pos.cortesias.domain.DetalleCortesia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetalleCortesiaRepository extends JpaRepository<DetalleCortesia, UUID> {

    @EntityGraph(attributePaths = {"tipoGranizado", "tamanoVaso"})
    List<DetalleCortesia> findByCortesia_IdCortesiaOrderByIdDetalleCortesia(UUID idCortesia);
}
