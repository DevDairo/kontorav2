package com.kontora.pos.deposito.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.catalogos.domain.TipoServicio;
import com.kontora.pos.catalogos.repository.TipoServicioRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.deposito.domain.ConsignacionBancaria;
import com.kontora.pos.deposito.domain.MovimientoDeposito;
import com.kontora.pos.deposito.domain.PagoServicio;
import com.kontora.pos.deposito.dto.ConsignacionBancariaResponse;
import com.kontora.pos.deposito.dto.MovimientoDepositoResponse;
import com.kontora.pos.deposito.dto.PagoServicioResponse;
import com.kontora.pos.deposito.dto.RegistrarConsignacionBancariaRequest;
import com.kontora.pos.deposito.dto.RegistrarPagoServicioRequest;
import com.kontora.pos.deposito.dto.SaldoDepositoResponse;
import com.kontora.pos.deposito.repository.ConsignacionBancariaRepository;
import com.kontora.pos.deposito.repository.MovimientoDepositoRepository;
import com.kontora.pos.deposito.repository.PagoServicioRepository;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class DepositoService {

    private static final String ROL_ADMINISTRADOR = "administrador";
    private static final String ROL_GERENTE = "gerente";
    private static final String ESTADO_REGISTRADO = "registrado";
    private static final String ESTADO_ACTIVO = "activo";
    private static final String TIPO_SALIDA_CONSIGNACION = "salida_consignacion";
    private static final String TIPO_SALIDA_PAGO_SERVICIO = "salida_pago_servicio";

    private final MovimientoDepositoRepository movimientoDepositoRepository;
    private final ConsignacionBancariaRepository consignacionBancariaRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final DepositoSaldoService depositoSaldoService;
    private final AuditoriaService auditoriaService;

    public DepositoService(
            MovimientoDepositoRepository movimientoDepositoRepository,
            ConsignacionBancariaRepository consignacionBancariaRepository,
            PagoServicioRepository pagoServicioRepository,
            TipoServicioRepository tipoServicioRepository,
            UsuarioRepository usuarioRepository,
            DepositoSaldoService depositoSaldoService,
            AuditoriaService auditoriaService) {
        this.movimientoDepositoRepository = movimientoDepositoRepository;
        this.consignacionBancariaRepository = consignacionBancariaRepository;
        this.pagoServicioRepository = pagoServicioRepository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.depositoSaldoService = depositoSaldoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public SaldoDepositoResponse obtenerSaldoActual(PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        return new SaldoDepositoResponse(depositoSaldoService.obtenerSaldoActual());
    }

    @Transactional
    public ConsignacionBancariaResponse registrarConsignacion(
            RegistrarConsignacionBancariaRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        Usuario usuarioRegistro = usuarioAutenticado(principalUsuario);
        BigDecimal valorConsignado = normalizarMoneda(request.valorConsignado());
        BigDecimal saldoAnterior = depositoSaldoService.bloquearYObtenerSaldoActual();
        validarSaldoDisponible(saldoAnterior, valorConsignado);
        OffsetDateTime fechaRegistro = OffsetDateTime.now();
        String observacion = normalizarTexto(request.observacion());

        MovimientoDeposito movimiento = crearMovimientoSalida(
                TIPO_SALIDA_CONSIGNACION,
                valorConsignado,
                saldoAnterior,
                usuarioRegistro,
                fechaRegistro,
                observacion == null ? "Salida por consignacion bancaria" : observacion);

        ConsignacionBancaria consignacion = new ConsignacionBancaria();
        consignacion.setMovimientoDeposito(movimiento);
        consignacion.setValorConsignado(valorConsignado);
        consignacion.setFechaConsignacion(fechaRegistro);
        consignacion.setUsuarioRegistro(usuarioRegistro);
        consignacion.setObservacion(observacion);
        consignacion.setEstado(ESTADO_REGISTRADO);
        ConsignacionBancaria consignacionGuardada = consignacionBancariaRepository.saveAndFlush(consignacion);

        auditoriaService.registrar(
                usuarioRegistro,
                "movimientos_deposito",
                movimiento.getIdMovimientoDeposito(),
                "crear",
                null,
                snapshotMovimiento(movimiento),
                "Salida de deposito por consignacion bancaria");
        auditoriaService.registrar(
                usuarioRegistro,
                "consignaciones_bancarias",
                consignacionGuardada.getIdConsignacionBancaria(),
                "crear",
                null,
                snapshotConsignacion(consignacionGuardada),
                "Consignacion bancaria registrada desde deposito");

        return toResponse(consignacionGuardada);
    }

    @Transactional
    public PagoServicioResponse registrarPagoServicio(
            RegistrarPagoServicioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        Usuario usuarioRegistro = usuarioAutenticado(principalUsuario);
        TipoServicio tipoServicio = tipoServicioRepository.findById(request.idTipoServicio())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tipo de servicio no encontrado"));
        if (!ESTADO_ACTIVO.equals(tipoServicio.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "El tipo de servicio no esta activo");
        }

        BigDecimal valorPagado = normalizarMoneda(request.valorPagado());
        BigDecimal saldoAnterior = depositoSaldoService.bloquearYObtenerSaldoActual();
        validarSaldoDisponible(saldoAnterior, valorPagado);
        OffsetDateTime fechaRegistro = OffsetDateTime.now();
        String descripcion = normalizarTexto(request.descripcion());

        MovimientoDeposito movimiento = crearMovimientoSalida(
                TIPO_SALIDA_PAGO_SERVICIO,
                valorPagado,
                saldoAnterior,
                usuarioRegistro,
                fechaRegistro,
                descripcion == null ? "Pago de servicio: " + tipoServicio.getNombreServicio() : descripcion);

        PagoServicio pagoServicio = new PagoServicio();
        pagoServicio.setMovimientoDeposito(movimiento);
        pagoServicio.setTipoServicio(tipoServicio);
        pagoServicio.setValorPagado(valorPagado);
        pagoServicio.setDescripcion(descripcion);
        pagoServicio.setFechaPago(fechaRegistro);
        pagoServicio.setUsuarioRegistro(usuarioRegistro);
        pagoServicio.setEstado(ESTADO_REGISTRADO);
        PagoServicio pagoServicioGuardado = pagoServicioRepository.saveAndFlush(pagoServicio);

        auditoriaService.registrar(
                usuarioRegistro,
                "movimientos_deposito",
                movimiento.getIdMovimientoDeposito(),
                "crear",
                null,
                snapshotMovimiento(movimiento),
                "Salida de deposito por pago de servicio");
        auditoriaService.registrar(
                usuarioRegistro,
                "pagos_servicios",
                pagoServicioGuardado.getIdPagoServicio(),
                "crear",
                null,
                snapshotPagoServicio(pagoServicioGuardado),
                "Pago de servicio registrado desde deposito");

        return toResponse(pagoServicioGuardado);
    }

    private MovimientoDeposito crearMovimientoSalida(
            String tipoMovimiento,
            BigDecimal valor,
            BigDecimal saldoAnterior,
            Usuario usuarioRegistro,
            OffsetDateTime fechaRegistro,
            String observacion) {
        MovimientoDeposito movimiento = new MovimientoDeposito();
        movimiento.setTipoMovimientoDeposito(tipoMovimiento);
        movimiento.setValorMovimiento(valor);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoPosterior(saldoAnterior.subtract(valor).setScale(2, RoundingMode.HALF_UP));
        movimiento.setUsuarioRegistro(usuarioRegistro);
        movimiento.setFechaMovimiento(fechaRegistro);
        movimiento.setObservacion(observacion);
        return movimientoDepositoRepository.saveAndFlush(movimiento);
    }

    private void validarSaldoDisponible(BigDecimal saldoAnterior, BigDecimal valorSalida) {
        if (saldoAnterior.compareTo(valorSalida) < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Saldo insuficiente en deposito para registrar la salida");
        }
    }

    private Usuario usuarioAutenticado(PrincipalUsuario principalUsuario) {
        return usuarioRepository.findById(principalUsuario.idUsuario())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private void validarRolAdministrativo(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!ROL_ADMINISTRADOR.equals(rol) && !ROL_GERENTE.equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede operar deposito");
        }
    }

    private BigDecimal normalizarMoneda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private MovimientoDepositoResponse toMovimientoResponse(MovimientoDeposito movimiento) {
        return new MovimientoDepositoResponse(
                movimiento.getIdMovimientoDeposito(),
                movimiento.getTipoMovimientoDeposito(),
                movimiento.getValorMovimiento(),
                movimiento.getSaldoAnterior(),
                movimiento.getSaldoPosterior(),
                movimiento.getCierreCaja() == null ? null : movimiento.getCierreCaja().getIdCierreCaja(),
                movimiento.getUsuarioRegistro().getIdUsuario(),
                movimiento.getUsuarioRegistro().getNombreUsuario(),
                movimiento.getFechaMovimiento(),
                movimiento.getObservacion());
    }

    private ConsignacionBancariaResponse toResponse(ConsignacionBancaria consignacion) {
        return new ConsignacionBancariaResponse(
                consignacion.getIdConsignacionBancaria(),
                consignacion.getValorConsignado(),
                consignacion.getFechaConsignacion(),
                consignacion.getUsuarioRegistro().getIdUsuario(),
                consignacion.getUsuarioRegistro().getNombreUsuario(),
                consignacion.getObservacion(),
                consignacion.getEstado(),
                toMovimientoResponse(consignacion.getMovimientoDeposito()));
    }

    private PagoServicioResponse toResponse(PagoServicio pagoServicio) {
        return new PagoServicioResponse(
                pagoServicio.getIdPagoServicio(),
                pagoServicio.getTipoServicio().getIdTipoServicio(),
                pagoServicio.getTipoServicio().getNombreServicio(),
                pagoServicio.getValorPagado(),
                pagoServicio.getDescripcion(),
                pagoServicio.getFechaPago(),
                pagoServicio.getUsuarioRegistro().getIdUsuario(),
                pagoServicio.getUsuarioRegistro().getNombreUsuario(),
                pagoServicio.getEstado(),
                toMovimientoResponse(pagoServicio.getMovimientoDeposito()));
    }

    private Map<String, Object> snapshotMovimiento(MovimientoDeposito movimiento) {
        return valores(
                "id_movimiento_deposito", movimiento.getIdMovimientoDeposito(),
                "tipo_movimiento_deposito", movimiento.getTipoMovimientoDeposito(),
                "valor_movimiento", movimiento.getValorMovimiento(),
                "saldo_anterior", movimiento.getSaldoAnterior(),
                "saldo_posterior", movimiento.getSaldoPosterior(),
                "fecha_movimiento", movimiento.getFechaMovimiento(),
                "observacion", movimiento.getObservacion());
    }

    private Map<String, Object> snapshotConsignacion(ConsignacionBancaria consignacion) {
        return valores(
                "id_consignacion_bancaria", consignacion.getIdConsignacionBancaria(),
                "id_movimiento_deposito", consignacion.getMovimientoDeposito().getIdMovimientoDeposito(),
                "valor_consignado", consignacion.getValorConsignado(),
                "fecha_consignacion", consignacion.getFechaConsignacion(),
                "estado", consignacion.getEstado(),
                "observacion", consignacion.getObservacion());
    }

    private Map<String, Object> snapshotPagoServicio(PagoServicio pagoServicio) {
        return valores(
                "id_pago_servicio", pagoServicio.getIdPagoServicio(),
                "id_movimiento_deposito", pagoServicio.getMovimientoDeposito().getIdMovimientoDeposito(),
                "id_tipo_servicio", pagoServicio.getTipoServicio().getIdTipoServicio(),
                "nombre_servicio", pagoServicio.getTipoServicio().getNombreServicio(),
                "valor_pagado", pagoServicio.getValorPagado(),
                "fecha_pago", pagoServicio.getFechaPago(),
                "estado", pagoServicio.getEstado(),
                "descripcion", pagoServicio.getDescripcion());
    }
}
