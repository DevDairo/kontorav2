package com.kontora.pos.caja.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.AdicionDiaria;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.domain.CierreCaja;
import com.kontora.pos.caja.domain.PagoTrabajadoresDiario;
import com.kontora.pos.caja.dto.CerrarCajaRequest;
import com.kontora.pos.caja.dto.CierreCajaResponse;
import com.kontora.pos.caja.dto.ResumenCajaDiariaResponse;
import com.kontora.pos.caja.repository.AdicionDiariaRepository;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.caja.repository.CierreCajaRepository;
import com.kontora.pos.caja.repository.GastoCajaRepository;
import com.kontora.pos.caja.repository.PagoTrabajadoresDiarioRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.deposito.domain.MovimientoDeposito;
import com.kontora.pos.deposito.dto.MovimientoDepositoResponse;
import com.kontora.pos.deposito.repository.MovimientoDepositoRepository;
import com.kontora.pos.deposito.service.DepositoSaldoService;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.repository.PagoVentaRepository;
import com.kontora.pos.ventas.repository.VentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class CierreCajaService {

    private static final String ESTADO_CAJA_ABIERTA = "abierta";
    private static final String METODO_EFECTIVO = "efectivo";
    private static final String METODO_TRANSFERENCIA = "transferencia";
    private static final String TRANSFERENCIA_PENDIENTE = "pendiente";
    private static final String TRANSFERENCIA_VALIDADA = "validada";
    private static final String TRANSFERENCIA_RECHAZADA = "rechazada";
    private static final String TIPO_MOVIMIENTO_ENTRADA_CIERRE = "entrada_cierre";

    private final CajaDiariaRepository cajaDiariaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final AdicionDiariaRepository adicionDiariaRepository;
    private final PagoTrabajadoresDiarioRepository pagoTrabajadoresDiarioRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final VentaRepository ventaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final MovimientoDepositoRepository movimientoDepositoRepository;
    private final DepositoSaldoService depositoSaldoService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public CierreCajaService(
            CajaDiariaRepository cajaDiariaRepository,
            CierreCajaRepository cierreCajaRepository,
            AdicionDiariaRepository adicionDiariaRepository,
            PagoTrabajadoresDiarioRepository pagoTrabajadoresDiarioRepository,
            GastoCajaRepository gastoCajaRepository,
            VentaRepository ventaRepository,
            PagoVentaRepository pagoVentaRepository,
            MovimientoDepositoRepository movimientoDepositoRepository,
            DepositoSaldoService depositoSaldoService,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService) {
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.cierreCajaRepository = cierreCajaRepository;
        this.adicionDiariaRepository = adicionDiariaRepository;
        this.pagoTrabajadoresDiarioRepository = pagoTrabajadoresDiarioRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.ventaRepository = ventaRepository;
        this.pagoVentaRepository = pagoVentaRepository;
        this.movimientoDepositoRepository = movimientoDepositoRepository;
        this.depositoSaldoService = depositoSaldoService;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public CierreCajaResponse cerrarCaja(
            UUID idCajaDiaria,
            CerrarCajaRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolCierre(principalUsuario);
        CajaDiaria cajaDiaria = cajaDiariaRepository.findById(idCajaDiaria)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caja diaria no encontrada"));
        validarCajaAbierta(cajaDiaria);
        if (cierreCajaRepository.existsByCajaDiaria_IdCajaDiaria(idCajaDiaria)) {
            throw new ApiException(HttpStatus.CONFLICT, "La caja diaria ya tiene cierre registrado");
        }

        AdicionDiaria adicionDiaria = adicionDiariaRepository.findByCajaDiaria_IdCajaDiaria(idCajaDiaria)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Debe existir registro de adiciones diarias antes del cierre"));
        PagoTrabajadoresDiario pagoTrabajadores = pagoTrabajadoresDiarioRepository.findByCajaDiaria_IdCajaDiaria(idCajaDiaria)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No se puede cerrar caja sin registrar y confirmar el pago diario de trabajadores"));
        if (!pagoTrabajadores.isConfirmadoParaCierre()) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede cerrar caja sin registrar y confirmar el pago diario de trabajadores");
        }

        Usuario usuarioCierre = usuarioRepository.findById(principalUsuario.idUsuario())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));

        TotalesCierre totales = calcularTotales(idCajaDiaria, adicionDiaria, pagoTrabajadores, request);
        OffsetDateTime fechaCierre = OffsetDateTime.now();

        CierreCaja cierreCaja = new CierreCaja();
        cierreCaja.setCajaDiaria(cajaDiaria);
        cierreCaja.setTotalVentas(totales.totalVentas());
        cierreCaja.setTotalVentasEfectivo(totales.totalVentasEfectivo());
        cierreCaja.setTotalVentasTransferencia(totales.totalVentasTransferencia());
        cierreCaja.setTotalTransferenciasPendientes(totales.totalTransferenciasPendientes());
        cierreCaja.setTotalTransferenciasValidadas(totales.totalTransferenciasValidadas());
        cierreCaja.setTotalTransferenciasRechazadas(totales.totalTransferenciasRechazadas());
        cierreCaja.setTotalGastos(totales.totalGastos());
        cierreCaja.setTotalAdiciones(totales.totalAdiciones());
        cierreCaja.setTotalPagoTrabajadores(totales.totalPagoTrabajadores());
        cierreCaja.setEfectivoEsperadoSinBase(totales.efectivoEsperadoSinBase());
        cierreCaja.setEfectivoContadoSinBase(totales.efectivoContadoSinBase());
        cierreCaja.setDiferenciaCaja(totales.diferenciaCaja());
        cierreCaja.setValorADeposito(totales.valorADeposito());
        cierreCaja.setFechaCierre(fechaCierre);
        cierreCaja.setUsuarioCierre(usuarioCierre);
        cierreCaja.setObservaciones(normalizarTextoOpcional(request.observaciones()));

        CierreCaja cierreGuardado = cierreCajaRepository.saveAndFlush(cierreCaja);
        MovimientoDeposito movimientoDeposito = crearMovimientoDeposito(cierreGuardado, usuarioCierre, fechaCierre);
        auditoriaService.registrar(
                usuarioCierre,
                "cierres_caja",
                cierreGuardado.getIdCierreCaja(),
                "cerrar",
                null,
                snapshotCierre(cierreGuardado),
                "Cierre de caja diaria");
        if (movimientoDeposito != null) {
            auditoriaService.registrar(
                    usuarioCierre,
                    "movimientos_deposito",
                    movimientoDeposito.getIdMovimientoDeposito(),
                    "crear",
                    null,
                    snapshotMovimientoDeposito(movimientoDeposito),
                    "Movimiento de deposito por cierre de caja");
        }

        return toResponse(cierreGuardado, movimientoDeposito);
    }

    @Transactional(readOnly = true)
    public CierreCajaResponse obtenerCierrePorCaja(UUID idCajaDiaria) {
        CierreCaja cierreCaja = cierreCajaRepository.findByCajaDiaria_IdCajaDiaria(idCajaDiaria)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La caja diaria no tiene cierre registrado"));
        MovimientoDeposito movimientoDeposito = movimientoDepositoRepository
                .findByCierreCaja_IdCierreCaja(cierreCaja.getIdCierreCaja())
                .orElse(null);
        return toResponse(cierreCaja, movimientoDeposito);
    }

    @Transactional(readOnly = true)
    public ResumenCajaDiariaResponse obtenerResumenCajaAbierta(PrincipalUsuario principalUsuario) {
        validarRolResumen(principalUsuario);
        CajaDiaria cajaDiaria = cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_CAJA_ABIERTA)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe una caja diaria abierta"));
        AdicionDiaria adicionDiaria = adicionDiariaRepository.findByCajaDiaria_IdCajaDiaria(cajaDiaria.getIdCajaDiaria())
                .orElse(null);
        PagoTrabajadoresDiario pagoTrabajadores = pagoTrabajadoresDiarioRepository
                .findByCajaDiaria_IdCajaDiaria(cajaDiaria.getIdCajaDiaria())
                .orElse(null);
        TotalesOperativos totales = calcularTotalesOperativos(
                cajaDiaria.getIdCajaDiaria(),
                adicionDiaria == null ? BigDecimal.ZERO : adicionDiaria.getValorTotal(),
                pagoTrabajadores == null ? BigDecimal.ZERO : pagoTrabajadores.getValorTotalPagado());
        boolean pagoTrabajadoresConfirmado = pagoTrabajadores != null && pagoTrabajadores.isConfirmadoParaCierre();
        boolean listoParaCierre = adicionDiaria != null
                && pagoTrabajadoresConfirmado
                && totales.efectivoEsperadoSinBase().compareTo(BigDecimal.ZERO) >= 0;

        return new ResumenCajaDiariaResponse(
                cajaDiaria.getIdCajaDiaria(),
                cajaDiaria.getFechaOperacion(),
                normalizarMoneda(cajaDiaria.getValorBase()),
                totales.totalVentas(),
                totales.totalVentasEfectivo(),
                totales.totalVentasTransferencia(),
                totales.totalTransferenciasPendientes(),
                totales.totalTransferenciasValidadas(),
                totales.totalTransferenciasRechazadas(),
                totales.totalGastos(),
                totales.totalAdiciones(),
                adicionDiaria != null,
                totales.totalPagoTrabajadores(),
                pagoTrabajadores != null,
                pagoTrabajadoresConfirmado,
                totales.efectivoEsperadoSinBase(),
                listoParaCierre);
    }

    private TotalesCierre calcularTotales(
            UUID idCajaDiaria,
            AdicionDiaria adicionDiaria,
            PagoTrabajadoresDiario pagoTrabajadores,
            CerrarCajaRequest request) {
        TotalesOperativos totalesOperativos = calcularTotalesOperativos(
                idCajaDiaria,
                adicionDiaria.getValorTotal(),
                pagoTrabajadores.getValorTotalPagado());
        if (totalesOperativos.efectivoEsperadoSinBase().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El efectivo esperado sin base no puede ser negativo");
        }
        BigDecimal efectivoContadoSinBase = normalizarMoneda(request.efectivoContadoSinBase());
        BigDecimal diferenciaCaja = efectivoContadoSinBase
                .subtract(totalesOperativos.efectivoEsperadoSinBase())
                .setScale(2, RoundingMode.HALF_UP);

        return new TotalesCierre(
                totalesOperativos.totalVentas(),
                totalesOperativos.totalVentasEfectivo(),
                totalesOperativos.totalVentasTransferencia(),
                totalesOperativos.totalTransferenciasPendientes(),
                totalesOperativos.totalTransferenciasValidadas(),
                totalesOperativos.totalTransferenciasRechazadas(),
                totalesOperativos.totalGastos(),
                totalesOperativos.totalAdiciones(),
                totalesOperativos.totalPagoTrabajadores(),
                totalesOperativos.efectivoEsperadoSinBase(),
                efectivoContadoSinBase,
                diferenciaCaja,
                efectivoContadoSinBase);
    }

    private TotalesOperativos calcularTotalesOperativos(
            UUID idCajaDiaria,
            BigDecimal valorAdiciones,
            BigDecimal valorPagoTrabajadores) {
        BigDecimal totalVentas = normalizarMoneda(ventaRepository.sumarVentasRegistradasPorCaja(idCajaDiaria));
        BigDecimal totalVentasEfectivo = normalizarMoneda(
                pagoVentaRepository.sumarPagosRegistradosPorCajaYMetodo(idCajaDiaria, METODO_EFECTIVO));
        BigDecimal totalVentasTransferencia = normalizarMoneda(
                pagoVentaRepository.sumarPagosRegistradosPorCajaYMetodo(idCajaDiaria, METODO_TRANSFERENCIA));
        BigDecimal totalTransferenciasPendientes = normalizarMoneda(
                pagoVentaRepository.sumarTransferenciasRegistradasPorCajaYEstado(idCajaDiaria, TRANSFERENCIA_PENDIENTE));
        BigDecimal totalTransferenciasValidadas = normalizarMoneda(
                pagoVentaRepository.sumarTransferenciasRegistradasPorCajaYEstado(idCajaDiaria, TRANSFERENCIA_VALIDADA));
        BigDecimal totalTransferenciasRechazadas = normalizarMoneda(
                pagoVentaRepository.sumarTransferenciasRegistradasPorCajaYEstado(idCajaDiaria, TRANSFERENCIA_RECHAZADA));
        BigDecimal totalGastos = normalizarMoneda(gastoCajaRepository.sumarGastosVigentesPorCaja(idCajaDiaria));
        BigDecimal totalAdiciones = normalizarMoneda(valorAdiciones);
        BigDecimal totalPagoTrabajadores = normalizarMoneda(valorPagoTrabajadores);
        BigDecimal efectivoEsperadoSinBase = totalVentasEfectivo
                .add(totalAdiciones)
                .subtract(totalGastos)
                .subtract(totalPagoTrabajadores)
                .setScale(2, RoundingMode.HALF_UP);

        return new TotalesOperativos(
                totalVentas,
                totalVentasEfectivo,
                totalVentasTransferencia,
                totalTransferenciasPendientes,
                totalTransferenciasValidadas,
                totalTransferenciasRechazadas,
                totalGastos,
                totalAdiciones,
                totalPagoTrabajadores,
                efectivoEsperadoSinBase);
    }

    private MovimientoDeposito crearMovimientoDeposito(
            CierreCaja cierreCaja,
            Usuario usuarioCierre,
            OffsetDateTime fechaMovimiento) {
        if (cierreCaja.getValorADeposito().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal saldoAnterior = depositoSaldoService.bloquearYObtenerSaldoActual();
        BigDecimal saldoPosterior = saldoAnterior
                .add(cierreCaja.getValorADeposito())
                .setScale(2, RoundingMode.HALF_UP);

        MovimientoDeposito movimientoDeposito = new MovimientoDeposito();
        movimientoDeposito.setTipoMovimientoDeposito(TIPO_MOVIMIENTO_ENTRADA_CIERRE);
        movimientoDeposito.setValorMovimiento(cierreCaja.getValorADeposito());
        movimientoDeposito.setSaldoAnterior(saldoAnterior);
        movimientoDeposito.setSaldoPosterior(saldoPosterior);
        movimientoDeposito.setCierreCaja(cierreCaja);
        movimientoDeposito.setUsuarioRegistro(usuarioCierre);
        movimientoDeposito.setFechaMovimiento(fechaMovimiento);
        movimientoDeposito.setObservacion("Entrada automatica por cierre de caja");
        return movimientoDepositoRepository.saveAndFlush(movimientoDeposito);
    }

    private void validarRolCierre(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede cerrar caja diaria");
        }
    }

    private void validarRolResumen(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede consultar el resumen de caja");
        }
    }

    private void validarCajaAbierta(CajaDiaria cajaDiaria) {
        if (!ESTADO_CAJA_ABIERTA.equals(cajaDiaria.getEstadoCaja())) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede cerrar una caja que no este abierta");
        }
    }

    private BigDecimal normalizarMoneda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private Map<String, Object> snapshotCierre(CierreCaja cierreCaja) {
        return valores(
                "id_cierre_caja", cierreCaja.getIdCierreCaja(),
                "id_caja_diaria", cierreCaja.getCajaDiaria().getIdCajaDiaria(),
                "total_ventas", cierreCaja.getTotalVentas(),
                "total_ventas_efectivo", cierreCaja.getTotalVentasEfectivo(),
                "total_ventas_transferencia", cierreCaja.getTotalVentasTransferencia(),
                "total_transferencias_pendientes", cierreCaja.getTotalTransferenciasPendientes(),
                "total_transferencias_validadas", cierreCaja.getTotalTransferenciasValidadas(),
                "total_transferencias_rechazadas", cierreCaja.getTotalTransferenciasRechazadas(),
                "total_gastos", cierreCaja.getTotalGastos(),
                "total_adiciones", cierreCaja.getTotalAdiciones(),
                "total_pago_trabajadores", cierreCaja.getTotalPagoTrabajadores(),
                "efectivo_esperado_sin_base", cierreCaja.getEfectivoEsperadoSinBase(),
                "efectivo_contado_sin_base", cierreCaja.getEfectivoContadoSinBase(),
                "diferencia_caja", cierreCaja.getDiferenciaCaja(),
                "valor_a_deposito", cierreCaja.getValorADeposito(),
                "fecha_cierre", cierreCaja.getFechaCierre(),
                "id_usuario_cierre", cierreCaja.getUsuarioCierre().getIdUsuario(),
                "observaciones", cierreCaja.getObservaciones());
    }

    private Map<String, Object> snapshotMovimientoDeposito(MovimientoDeposito movimientoDeposito) {
        CierreCaja cierreCaja = movimientoDeposito.getCierreCaja();
        return valores(
                "id_movimiento_deposito", movimientoDeposito.getIdMovimientoDeposito(),
                "tipo_movimiento_deposito", movimientoDeposito.getTipoMovimientoDeposito(),
                "valor_movimiento", movimientoDeposito.getValorMovimiento(),
                "saldo_anterior", movimientoDeposito.getSaldoAnterior(),
                "saldo_posterior", movimientoDeposito.getSaldoPosterior(),
                "id_cierre_caja", cierreCaja == null ? null : cierreCaja.getIdCierreCaja(),
                "id_usuario_registro", movimientoDeposito.getUsuarioRegistro().getIdUsuario(),
                "fecha_movimiento", movimientoDeposito.getFechaMovimiento(),
                "observacion", movimientoDeposito.getObservacion());
    }

    private CierreCajaResponse toResponse(CierreCaja cierreCaja, MovimientoDeposito movimientoDeposito) {
        Usuario usuarioCierre = cierreCaja.getUsuarioCierre();
        return new CierreCajaResponse(
                cierreCaja.getIdCierreCaja(),
                cierreCaja.getCajaDiaria().getIdCajaDiaria(),
                cierreCaja.getTotalVentas(),
                cierreCaja.getTotalVentasEfectivo(),
                cierreCaja.getTotalVentasTransferencia(),
                cierreCaja.getTotalTransferenciasPendientes(),
                cierreCaja.getTotalTransferenciasValidadas(),
                cierreCaja.getTotalTransferenciasRechazadas(),
                cierreCaja.getTotalGastos(),
                cierreCaja.getTotalAdiciones(),
                cierreCaja.getTotalPagoTrabajadores(),
                cierreCaja.getEfectivoEsperadoSinBase(),
                cierreCaja.getEfectivoContadoSinBase(),
                cierreCaja.getDiferenciaCaja(),
                cierreCaja.getValorADeposito(),
                cierreCaja.getFechaCierre(),
                usuarioCierre.getIdUsuario(),
                usuarioCierre.getNombreUsuario(),
                cierreCaja.getObservaciones(),
                movimientoDeposito == null ? null : toResponse(movimientoDeposito));
    }

    private MovimientoDepositoResponse toResponse(MovimientoDeposito movimientoDeposito) {
        Usuario usuarioRegistro = movimientoDeposito.getUsuarioRegistro();
        CierreCaja cierreCaja = movimientoDeposito.getCierreCaja();
        return new MovimientoDepositoResponse(
                movimientoDeposito.getIdMovimientoDeposito(),
                movimientoDeposito.getTipoMovimientoDeposito(),
                movimientoDeposito.getValorMovimiento(),
                movimientoDeposito.getSaldoAnterior(),
                movimientoDeposito.getSaldoPosterior(),
                cierreCaja == null ? null : cierreCaja.getIdCierreCaja(),
                usuarioRegistro.getIdUsuario(),
                usuarioRegistro.getNombreUsuario(),
                movimientoDeposito.getFechaMovimiento(),
                movimientoDeposito.getObservacion());
    }

    private record TotalesCierre(
            BigDecimal totalVentas,
            BigDecimal totalVentasEfectivo,
            BigDecimal totalVentasTransferencia,
            BigDecimal totalTransferenciasPendientes,
            BigDecimal totalTransferenciasValidadas,
            BigDecimal totalTransferenciasRechazadas,
            BigDecimal totalGastos,
            BigDecimal totalAdiciones,
            BigDecimal totalPagoTrabajadores,
            BigDecimal efectivoEsperadoSinBase,
            BigDecimal efectivoContadoSinBase,
            BigDecimal diferenciaCaja,
            BigDecimal valorADeposito
    ) {
    }

    private record TotalesOperativos(
            BigDecimal totalVentas,
            BigDecimal totalVentasEfectivo,
            BigDecimal totalVentasTransferencia,
            BigDecimal totalTransferenciasPendientes,
            BigDecimal totalTransferenciasValidadas,
            BigDecimal totalTransferenciasRechazadas,
            BigDecimal totalGastos,
            BigDecimal totalAdiciones,
            BigDecimal totalPagoTrabajadores,
            BigDecimal efectivoEsperadoSinBase
    ) {
    }
}
