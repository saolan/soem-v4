package com.saolan.soem.seguridad.servicio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.excepcion.ExcepcionNegocio;
import com.saolan.soem.compartido.excepcion.ExcepcionNoAutenticado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.seguridad.dto.CambioClaveDTO;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;
import com.saolan.soem.seguridad.mapeador.UsuarioMapeador;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.RefreshToken;
import com.saolan.soem.seguridad.repositorio.RefreshTokenRepositorio;
import com.saolan.soem.seguridad.repositorio.RolPermRepositorio;
import com.saolan.soem.seguridad.repositorio.RolSucuRepositorio;
import com.saolan.soem.seguridad.repositorio.UsuarioRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Login, renovacion, cambio de sucursal, cambio de clave y cierre de sesion.
 * Los permisos y la sucursal activa se resuelven una unica vez por token (al
 * emitirlo), nunca en cada solicitud (ver comentario de diseno en
 * {@code FiltroAutorizacion}).
 * <p>
 * Clave caducada: el access token se emite sin permisos, por lo que todo
 * endpoint protegido con {@code @RequierePermiso} responde 403 y solo quedan
 * utilizables {@code /auth/cambiar-clave}, {@code /auth/me} y
 * {@code /auth/logout}. Al cambiar la clave se emite una sesion nueva con
 * los permisos completos.
 */
@ApplicationScoped
public class AutenticacionServicio {

    private static final Logger LOGGER = Logger.getLogger(AutenticacionServicio.class.getName());

    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Usuario o clave incorrectos";
    private static final String MENSAJE_SESION_INVALIDA = "La sesión no es válida, debe iniciar sesión nuevamente";
    private static final String MENSAJE_CLAVE_CADUCADA = "Su clave está caducada, debe cambiarla antes de continuar";

    private static final String MOTIVO_ROTACION = "rotacion";
    private static final String MOTIVO_LOGOUT = "logout";
    private static final String MOTIVO_REUSO = "reuso_detectado";
    private static final String MOTIVO_CAMBIO_CLAVE = "cambio_clave";
    private static final String MOTIVO_CAMBIO_SUCURSAL = "cambio_sucursal";

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    @Inject
    private RefreshTokenRepositorio refreshTokenRepositorio;

    @Inject
    private RolPermRepositorio rolPermRepositorio;

    @Inject
    private RolSucuRepositorio rolSucuRepositorio;

    @Inject
    private ClaveServicio claveServicio;

    @Inject
    private TokenServicio tokenServicio;

    @Inject
    private UsuarioMapeador usuarioMapeador;

    @Inject
    @ConfigProperty(name = "soem.jwt.refresh-token.retencion-dias", defaultValue = "30")
    private long retencionRefreshTokenDias;

    @Transactional
    public ResultadoAutenticacion login(LoginRequestDTO request, String ipOrigen, String agenteUsuario) {
        Optional<PersUsua> usuarioActivo = usuarioRepositorio.buscarPorUsuari(request.usuario())
                .filter(u -> Boolean.TRUE.equals(u.getEstado()));

        if (usuarioActivo.isEmpty()) {
            claveServicio.simularVerificacion(request.clave());
            registrarLoginFallido(request.usuario(), ipOrigen);
            throw new ExcepcionNoAutenticado(MENSAJE_CREDENCIALES_INVALIDAS);
        }
        PersUsua usuario = usuarioActivo.get();
        if (!claveServicio.verificar(request.clave(), usuario.getClave())) {
            registrarLoginFallido(request.usuario(), ipOrigen);
            throw new ExcepcionNoAutenticado(MENSAJE_CREDENCIALES_INVALIDAS);
        }

        List<Sucursal> sucursalesAccesibles = rolSucuRepositorio.resolverSucursalesAccesibles(usuario.getPersonaId());
        Sucursal sucursalActiva = resolverSucursalActiva(request.sucursalId(), sucursalesAccesibles);

        LOGGER.log(Level.INFO, "Inicio de sesión exitoso: usuario={0}, ip={1}",
                new Object[] { usuario.getUsuari(), ipOrigen });
        return emitirSesion(usuario, sucursalActiva, sucursalesAccesibles, ipOrigen, agenteUsuario, null);
    }

    /**
     * Rota el refresh token que llega en la cookie: si el hash ya esta
     * revocado, se interpreta como reuso de un token ya rotado (posible
     * robo) y se revocan todos los tokens vigentes de esa persona.
     * <p>
     * {@code dontRollbackOn}: esa revocacion se hace justo antes de lanzar
     * {@link ExcepcionNoAutenticado}; sin esta excepcion a la regla el
     * rollback la desharia y el token robado seguiria vigente.
     */
    @Transactional(dontRollbackOn = ExcepcionNoAutenticado.class)
    public ResultadoAutenticacion refrescar(String refreshTokenCrudo, String ipOrigen, String agenteUsuario) {
        RefreshToken tokenActual = validarRefreshVigente(refreshTokenCrudo, ipOrigen);
        PersUsua usuario = tokenActual.getUsuario();

        revocar(tokenActual, MOTIVO_ROTACION);

        List<Sucursal> sucursalesAccesibles = rolSucuRepositorio.resolverSucursalesAccesibles(usuario.getPersonaId());
        Sucursal sucursalActiva = mantenerOReasignarSucursal(tokenActual.getSucursal(), sucursalesAccesibles);

        return emitirSesion(usuario, sucursalActiva, sucursalesAccesibles, ipOrigen, agenteUsuario, tokenActual);
    }

    /**
     * Cambia la sucursal activa de la sesion sin volver a pedir la clave:
     * rota el refresh token de la cookie (que debe pertenecer a la misma
     * persona del access token) y emite un access token nuevo con la
     * sucursal elegida. Mismo {@code dontRollbackOn} que {@link #refrescar}.
     */
    @Transactional(dontRollbackOn = ExcepcionNoAutenticado.class)
    public ResultadoAutenticacion cambiarSucursal(Integer personaId, String refreshTokenCrudo, Integer sucursalId,
            String ipOrigen, String agenteUsuario) {
        RefreshToken tokenActual = validarRefreshVigente(refreshTokenCrudo, ipOrigen);
        PersUsua usuario = tokenActual.getUsuario();

        if (!usuario.getPersonaId().equals(personaId)) {
            LOGGER.log(Level.WARNING, "Cambio de sucursal rechazado: la cookie no corresponde al usuario del token, ip={0}",
                    ipOrigen);
            throw new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA);
        }
        if (esClaveCaducada(usuario)) {
            throw new ExcepcionNegocio(MENSAJE_CLAVE_CADUCADA);
        }

        List<Sucursal> sucursalesAccesibles = rolSucuRepositorio.resolverSucursalesAccesibles(personaId);
        Sucursal sucursalActiva = resolverSucursalActiva(sucursalId, sucursalesAccesibles);

        revocar(tokenActual, MOTIVO_CAMBIO_SUCURSAL);
        return emitirSesion(usuario, sucursalActiva, sucursalesAccesibles, ipOrigen, agenteUsuario, tokenActual);
    }

    @Transactional
    public void logout(String refreshTokenCrudo) {
        String hash = tokenServicio.hashearRefreshToken(refreshTokenCrudo);
        refreshTokenRepositorio.buscarPorHash(hash)
                .filter(token -> !Boolean.TRUE.equals(token.getRevocado()))
                .ifPresent(token -> revocar(token, MOTIVO_LOGOUT));
    }

    /**
     * Cambia la clave y revoca todas las sesiones vigentes de la persona (el
     * resto de dispositivos queda fuera). Si la solicitud trae el refresh
     * token vigente de la sesion actual, se emite una sesion nueva con los
     * permisos completos (necesario para salir del estado de clave
     * caducada); si no, se devuelve vacio y el frontend debe iniciar sesion.
     */
    @Transactional
    public Optional<ResultadoAutenticacion> cambiarClave(Integer personaId, CambioClaveDTO cambioClaveDTO,
            String refreshTokenCrudo, String ipOrigen, String agenteUsuario) {
        PersUsua usuario = usuarioRepositorio.buscarPorId(personaId)
                .filter(u -> Boolean.TRUE.equals(u.getEstado()))
                .orElseThrow(() -> new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA));

        if (!claveServicio.verificar(cambioClaveDTO.claveActual(), usuario.getClave())) {
            throw new ExcepcionValidacion("La clave actual no es correcta",
                    List.of(new ErrorCampo("claveActual", "La clave actual no es correcta")));
        }

        Optional<RefreshToken> sesionActual = buscarRefreshVigenteDePersona(refreshTokenCrudo, personaId);

        usuario.setClave(claveServicio.hashear(cambioClaveDTO.claveNueva()));
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setCaducaFechHora(null);
        revocarTodosVigentes(personaId, MOTIVO_CAMBIO_CLAVE);
        LOGGER.log(Level.INFO, "Clave cambiada: usuario={0}, ip={1}", new Object[] { usuario.getUsuari(), ipOrigen });

        if (sesionActual.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken tokenActual = sesionActual.get();
        List<Sucursal> sucursalesAccesibles = rolSucuRepositorio.resolverSucursalesAccesibles(personaId);
        Sucursal sucursalActiva = mantenerOReasignarSucursal(tokenActual.getSucursal(), sucursalesAccesibles);
        return Optional.of(emitirSesion(usuario, sucursalActiva, sucursalesAccesibles, ipOrigen, agenteUsuario, tokenActual));
    }

    /**
     * Borra los refresh tokens expirados hace mas de
     * {@code soem.jwt.refresh-token.retencion-dias}. La retencion conserva la
     * cadena de rotaciones reciente para auditoria y deteccion de reuso.
     *
     * @return cantidad de tokens eliminados
     */
    @Transactional
    public int purgarRefreshTokensExpirados() {
        OffsetDateTime limite = OffsetDateTime.now().minusDays(retencionRefreshTokenDias);
        return refreshTokenRepositorio.purgarExpiradosAntesDe(limite);
    }

    /**
     * Valida el refresh token de la cookie: debe existir, no estar revocado
     * (si lo esta, es un reuso y se revocan todas las sesiones de la
     * persona), no estar expirado y pertenecer a un usuario activo.
     */
    private RefreshToken validarRefreshVigente(String refreshTokenCrudo, String ipOrigen) {
        String hash = tokenServicio.hashearRefreshToken(refreshTokenCrudo);
        RefreshToken tokenActual = refreshTokenRepositorio.buscarPorHash(hash)
                .orElseThrow(() -> new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA));

        if (Boolean.TRUE.equals(tokenActual.getRevocado())) {
            revocarTodosVigentes(tokenActual.getUsuario().getPersonaId(), MOTIVO_REUSO);
            LOGGER.log(Level.WARNING, "Reuso de refresh token detectado: usuario={0}, ip={1}",
                    new Object[] { tokenActual.getUsuario().getUsuari(), ipOrigen });
            throw new ExcepcionNoAutenticado("Se detectó un uso inválido de la sesión, debe iniciar sesión nuevamente");
        }
        if (tokenActual.getFechaHoraExpira().isBefore(OffsetDateTime.now())) {
            throw new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA);
        }
        if (!Boolean.TRUE.equals(tokenActual.getUsuario().getEstado())) {
            throw new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA);
        }
        return tokenActual;
    }

    /** Variante silenciosa: sin cookie o con una cookie no vigente de esa persona devuelve vacio, sin revocar nada. */
    private Optional<RefreshToken> buscarRefreshVigenteDePersona(String refreshTokenCrudo, Integer personaId) {
        if (refreshTokenCrudo == null || refreshTokenCrudo.isBlank()) {
            return Optional.empty();
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        return refreshTokenRepositorio.buscarPorHash(tokenServicio.hashearRefreshToken(refreshTokenCrudo))
                .filter(token -> !Boolean.TRUE.equals(token.getRevocado()))
                .filter(token -> token.getFechaHoraExpira().isAfter(ahora))
                .filter(token -> token.getUsuario().getPersonaId().equals(personaId));
    }

    private ResultadoAutenticacion emitirSesion(PersUsua usuario, Sucursal sucursalActiva, List<Sucursal> sucursalesAccesibles,
            String ipOrigen, String agenteUsuario, RefreshToken tokenPadre) {
        boolean claveCaducada = esClaveCaducada(usuario);
        Set<String> permisos = claveCaducada ? Set.of() : rolPermRepositorio.resolverPermisosDePersona(usuario.getPersonaId());
        Integer sucursalId = sucursalActiva == null ? null : sucursalActiva.getSucursalId();

        String accessToken = tokenServicio.emitirAccessToken(usuario, permisos, sucursalId, claveCaducada);
        String refreshTokenCrudo = tokenServicio.generarRefreshTokenCrudo();

        OffsetDateTime ahora = OffsetDateTime.now();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setSucursal(sucursalActiva);
        refreshToken.setTokenPadre(tokenPadre);
        refreshToken.setTokenHash(tokenServicio.hashearRefreshToken(refreshTokenCrudo));
        refreshToken.setFechaHoraEmision(ahora);
        refreshToken.setFechaHoraExpira(ahora.plusDays(tokenServicio.duracionRefreshTokenDias()));
        refreshToken.setRevocado(Boolean.FALSE);
        refreshToken.setIpOrigen(ipOrigen);
        refreshToken.setAgenteUsuario(agenteUsuario);
        refreshTokenRepositorio.crear(refreshToken);

        List<ComboDTO> sucursales = sucursalesAccesibles.stream()
                .map(sucursal -> new ComboDTO(sucursal.getSucursalId(), sucursal.getDescri()))
                .toList();

        LoginRespuestaDTO respuesta = new LoginRespuestaDTO(
                accessToken,
                tokenServicio.duracionAccessTokenMinutos() * 60,
                sucursalId,
                sucursales,
                usuarioMapeador.aUsuarioDTO(usuario),
                permisos.stream().sorted().toList(),
                claveCaducada);

        return new ResultadoAutenticacion(respuesta, refreshTokenCrudo);
    }

    private boolean esClaveCaducada(PersUsua usuario) {
        if (!Boolean.TRUE.equals(usuario.getCaducaClav())) {
            return false;
        }
        return usuario.getCaducaFechHora() == null || !usuario.getCaducaFechHora().isAfter(OffsetDateTime.now());
    }

    private void revocar(RefreshToken token, String motivo) {
        token.setRevocado(Boolean.TRUE);
        token.setFechaHoraRevocado(OffsetDateTime.now());
        token.setMotivoRevocacion(motivo);
    }

    private void revocarTodosVigentes(Integer personaId, String motivo) {
        refreshTokenRepositorio.buscarVigentesDePersona(personaId).forEach(token -> revocar(token, motivo));
    }

    /** Nunca registra la clave; el usuario se sanea para evitar inyeccion de lineas en el log. */
    private void registrarLoginFallido(String usuario, String ipOrigen) {
        LOGGER.log(Level.WARNING, "Inicio de sesión fallido: usuario={0}, ip={1}",
                new Object[] { sanearParaLog(usuario), ipOrigen });
    }

    private String sanearParaLog(String valor) {
        return valor == null ? null : valor.replaceAll("[\\r\\n\\t]", "_");
    }

    private Sucursal resolverSucursalActiva(Integer sucursalIdSolicitada, List<Sucursal> sucursalesAccesibles) {
        if (sucursalesAccesibles.isEmpty()) {
            throw new ExcepcionValidacion("El usuario no tiene acceso a ninguna sucursal habilitada",
                    List.of(new ErrorCampo("sucursalId", "El usuario no tiene acceso a ninguna sucursal habilitada")));
        }
        if (sucursalIdSolicitada == null) {
            return sucursalesAccesibles.get(0);
        }
        return sucursalesAccesibles.stream()
                .filter(sucursal -> sucursalIdSolicitada.equals(sucursal.getSucursalId()))
                .findFirst()
                .orElseThrow(() -> new ExcepcionValidacion("No tiene acceso a la sucursal solicitada",
                        List.of(new ErrorCampo("sucursalId", "No tiene acceso a la sucursal solicitada"))));
    }

    /** Mantiene la sucursal activa previa si sigue siendo accesible; si no, recae en la resolucion por defecto. */
    private Sucursal mantenerOReasignarSucursal(Sucursal sucursalPrevia, List<Sucursal> sucursalesAccesibles) {
        if (sucursalPrevia != null) {
            Integer idPrevio = sucursalPrevia.getSucursalId();
            var mantenida = sucursalesAccesibles.stream()
                    .filter(sucursal -> idPrevio.equals(sucursal.getSucursalId()))
                    .findFirst();
            if (mantenida.isPresent()) {
                return mantenida.get();
            }
        }
        return resolverSucursalActiva(null, sucursalesAccesibles);
    }
}
