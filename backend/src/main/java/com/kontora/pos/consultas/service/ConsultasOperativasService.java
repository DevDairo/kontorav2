package com.kontora.pos.consultas.service;

import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.consultas.dto.ConsultaAuditoriaResponse;
import com.kontora.pos.consultas.dto.ConsultaCierreDiarioResponse;
import com.kontora.pos.consultas.dto.ConsultaGastoCajaResponse;
import com.kontora.pos.consultas.dto.ConsultaInventarioActualResponse;
import com.kontora.pos.consultas.dto.ConsultaMovimientoDepositoResponse;
import com.kontora.pos.consultas.dto.ConsultaMovimientoInventarioResponse;
import com.kontora.pos.consultas.dto.ConsultaTransferenciaResponse;
import com.kontora.pos.consultas.dto.ConsultaVentaResponse;
import com.kontora.pos.consultas.dto.ConsultaVentasVasosResponse;
import com.kontora.pos.consultas.repository.ConsultasOperativasRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ConsultasOperativasService {

    private static final String ROL_VENDEDOR = "vendedor";
    private static final String ROL_ADMINISTRADOR = "administrador";
    private static final String ROL_GERENTE = "gerente";
    private static final String TRANSFERENCIA_PENDIENTE = "pendiente";
    private static final String TRANSFERENCIA_VALIDADA = "validada";
    private static final String TRANSFERENCIA_RECHAZADA = "rechazada";

    private final ConsultasOperativasRepository consultasRepository;

    public ConsultasOperativasService(ConsultasOperativasRepository consultasRepository) {
        this.consultasRepository = consultasRepository;
    }

    @Transactional(readOnly = true)
    public List<ConsultaVentaResponse> consultarVentas(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            PrincipalUsuario principalUsuario) {
        Periodo periodo = normalizarPeriodoRequerido(fechaInicio, fechaFin);
        UUID idUsuarioVendedor = esVendedor(principalUsuario) ? principalUsuario.idUsuario() : null;
        return consultasRepository.consultarVentas(periodo.fechaInicio(), periodo.fechaFin(), idUsuarioVendedor);
    }

    @Transactional(readOnly = true)
    public ConsultaCierreDiarioResponse consultarCierrePorFecha(
            LocalDate fechaOperacion,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede consultar cierres diarios");
        return consultasRepository.consultarCierrePorFecha(fechaOperacion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe cierre de caja para la fecha consultada"));
    }

    @Transactional(readOnly = true)
    public List<ConsultaGastoCajaResponse> consultarGastos(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            PrincipalUsuario principalUsuario) {
        Periodo periodo = normalizarPeriodoRequerido(fechaInicio, fechaFin);
        UUID idUsuarioRegistro = esVendedor(principalUsuario) ? principalUsuario.idUsuario() : null;
        return consultasRepository.consultarGastos(periodo.fechaInicio(), periodo.fechaFin(), idUsuarioRegistro);
    }

    @Transactional(readOnly = true)
    public List<ConsultaInventarioActualResponse> consultarInventarioActual() {
        return consultasRepository.consultarInventarioActual();
    }

    @Transactional(readOnly = true)
    public List<ConsultaMovimientoInventarioResponse> consultarMovimientosInventario(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            UUID idCajaDiaria,
            UUID idItemInventario) {
        Periodo periodo = normalizarPeriodoOpcional(fechaInicio, fechaFin);
        return consultasRepository.consultarMovimientosInventario(
                periodo.fechaInicio(),
                periodo.fechaFin(),
                idCajaDiaria,
                idItemInventario);
    }

    @Transactional(readOnly = true)
    public List<ConsultaVentasVasosResponse> consultarVentasVasos(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede consultar ventas de vasos");
        Periodo periodo = normalizarPeriodoRequerido(fechaInicio, fechaFin);
        return consultasRepository.consultarVentasVasos(periodo.fechaInicio(), periodo.fechaFin());
    }

    @Transactional(readOnly = true)
    public List<ConsultaMovimientoDepositoResponse> consultarMovimientosDeposito(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede consultar deposito");
        Periodo periodo = normalizarPeriodoOpcional(fechaInicio, fechaFin);
        return consultasRepository.consultarMovimientosDeposito(periodo.fechaInicio(), periodo.fechaFin());
    }

    @Transactional(readOnly = true)
    public List<ConsultaTransferenciaResponse> consultarTransferencias(
            String estadoValidacion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            PrincipalUsuario principalUsuario) {
        Periodo periodo = normalizarPeriodoOpcional(fechaInicio, fechaFin);
        UUID idUsuarioVendedor = esVendedor(principalUsuario) ? principalUsuario.idUsuario() : null;
        return consultasRepository.consultarTransferencias(
                estadosTransferencia(estadoValidacion),
                periodo.fechaInicio(),
                periodo.fechaFin(),
                idUsuarioVendedor);
    }

    @Transactional(readOnly = true)
    public List<ConsultaAuditoriaResponse> consultarAuditoria(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String tablaAfectada,
            String accion,
            PrincipalUsuario principalUsuario) {
        if (!esGerente(principalUsuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el gerente puede consultar auditoria");
        }
        Periodo periodo = normalizarPeriodoRequerido(fechaInicio, fechaFin);
        return consultasRepository.consultarAuditoria(
                periodo.fechaInicio(),
                periodo.fechaFin(),
                normalizarTexto(tablaAfectada),
                normalizarTexto(accion),
                true);
    }

    private Periodo normalizarPeriodoRequerido(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fechaInicio es obligatoria");
        }
        LocalDate fechaFinNormalizada = fechaFin == null ? fechaInicio : fechaFin;
        validarPeriodo(fechaInicio, fechaFinNormalizada);
        return new Periodo(fechaInicio, fechaFinNormalizada);
    }

    private Periodo normalizarPeriodoOpcional(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null && fechaFin == null) {
            return new Periodo(null, null);
        }
        LocalDate inicio = fechaInicio == null ? fechaFin : fechaInicio;
        LocalDate fin = fechaFin == null ? inicio : fechaFin;
        validarPeriodo(inicio, fin);
        return new Periodo(inicio, fin);
    }

    private void validarPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fechaFin no puede ser anterior a fechaInicio");
        }
    }

    private List<String> estadosTransferencia(String estadoValidacion) {
        String estado = normalizarTexto(estadoValidacion);
        if (estado == null) {
            return List.of(TRANSFERENCIA_PENDIENTE, TRANSFERENCIA_RECHAZADA);
        }
        if (!TRANSFERENCIA_PENDIENTE.equals(estado)
                && !TRANSFERENCIA_VALIDADA.equals(estado)
                && !TRANSFERENCIA_RECHAZADA.equals(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "estadoValidacion solo permite pendiente, validada o rechazada");
        }
        return List.of(estado);
    }

    private void validarRolAdministrativo(PrincipalUsuario principalUsuario, String mensaje) {
        if (!esAdministrador(principalUsuario) && !esGerente(principalUsuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, mensaje);
        }
    }

    private boolean esVendedor(PrincipalUsuario principalUsuario) {
        return ROL_VENDEDOR.equals(rol(principalUsuario));
    }

    private boolean esAdministrador(PrincipalUsuario principalUsuario) {
        return ROL_ADMINISTRADOR.equals(rol(principalUsuario));
    }

    private boolean esGerente(PrincipalUsuario principalUsuario) {
        return ROL_GERENTE.equals(rol(principalUsuario));
    }

    private String rol(PrincipalUsuario principalUsuario) {
        return principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim().toLowerCase(Locale.ROOT);
    }

    private record Periodo(LocalDate fechaInicio, LocalDate fechaFin) {
    }
}
