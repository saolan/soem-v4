package com.saolan.soem.seguridad.servicio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import com.saolan.soem.compartido.dto.ErrorCampo;
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
 * Login, renovacion y cierre de sesion. Los permisos y la sucursal activa se
 * resuelven una unica vez por token (al emitirlo), nunca en cada solicitud
 * (ver comentario de diseno en {@code FiltroAutorizacion}).
 */
@ApplicationScoped
public class AutenticacionServicio {

    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Usuario o clave incorrectos";
    private static final String MENSAJE_SESION_INVALIDA = "La sesión no es válida, debe iniciar sesión nuevamente";

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

    @Transactional
    public ResultadoAutenticacion login(LoginRequestDTO request, String ipOrigen, String agenteUsuario) {
        PersUsua usuario = usuarioRepositorio.buscarPorUsuari(request.usuario())
                .filter(u -> Boolean.TRUE.equals(u.getEstado()))
                .orElseThrow(() -> new ExcepcionNoAutenticado(MENSAJE_CREDENCIALES_INVALIDAS));

        if (!claveServicio.verificar(request.clave(), usuario.getClave())) {
            throw new ExcepcionNoAutenticado(MENSAJE_CREDENCIALES_INVALIDAS);
        }

        List<Sucursal> sucursalesAccesibles = rolSucuRepositorio.resolverSucursalesAccesibles(usuario.getPersonaId());
        Sucursal sucursalActiva = resolverSucursalActiva(request.sucursalId(), sucursalesAccesibles);

        return emitirSesion(usuario, sucursalActiva, ipOrigen, agenteUsuario);
    }

    /**
     * Rota el refresh token que llega en la cookie: si el hash ya esta
     * revocado, se interpreta como reuso de un token ya rotado (posible
     * robo) y se revocan todos los tokens vigentes de esa persona.
     */
    @Transactional
    public ResultadoAutenticacion refrescar(String refreshTokenCrudo, String ipOrigen, String agenteUsuario) {
        String hash = tokenServicio.hashearRefreshToken(refreshTokenCrudo);
        RefreshToken tokenActual = refreshTokenRepositorio.buscarPorHash(hash)
                .orElseThrow(() -> new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA));

        if (Boolean.TRUE.equals(tokenActual.getRevocado())) {
            revocarTodosVigentes(tokenActual.getUsuario().getPersonaId(), "reuso_detectado");
            throw new ExcepcionNoAutenticado("Se detectó un uso inválido de la sesión, debe iniciar sesión nuevamente");
        }
        if (tokenActual.getFechaHoraExpira().isBefore(OffsetDateTime.now())) {
            throw new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA);
        }

        PersUsua usuario = tokenActual.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA);
        }

        revocar(tokenActual, "rotacion");

        List<Sucursal> sucursalesAccesibles = rolSucuRepositorio.resolverSucursalesAccesibles(usuario.getPersonaId());
        Sucursal sucursalActiva = mantenerOReasignarSucursal(tokenActual.getSucursal(), sucursalesAccesibles);

        return emitirSesion(usuario, sucursalActiva, ipOrigen, agenteUsuario, tokenActual);
    }

    @Transactional
    public void logout(String refreshTokenCrudo) {
        String hash = tokenServicio.hashearRefreshToken(refreshTokenCrudo);
        refreshTokenRepositorio.buscarPorHash(hash)
                .filter(token -> !Boolean.TRUE.equals(token.getRevocado()))
                .ifPresent(token -> revocar(token, "logout"));
    }

    @Transactional
    public void cambiarClave(Integer personaId, CambioClaveDTO cambioClaveDTO) {
        PersUsua usuario = usuarioRepositorio.buscarPorId(personaId)
                .orElseThrow(() -> new ExcepcionNoAutenticado(MENSAJE_SESION_INVALIDA));

        if (!claveServicio.verificar(cambioClaveDTO.claveActual(), usuario.getClave())) {
            throw new ExcepcionValidacion("La clave actual no es correcta",
                    List.of(new ErrorCampo("claveActual", "La clave actual no es correcta")));
        }

        usuario.setClave(claveServicio.hashear(cambioClaveDTO.claveNueva()));
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setCaducaFechHora(null);
    }

    private ResultadoAutenticacion emitirSesion(PersUsua usuario, Sucursal sucursalActiva, String ipOrigen, String agenteUsuario) {
        return emitirSesion(usuario, sucursalActiva, ipOrigen, agenteUsuario, null);
    }

    private ResultadoAutenticacion emitirSesion(PersUsua usuario, Sucursal sucursalActiva, String ipOrigen,
            String agenteUsuario, RefreshToken tokenPadre) {
        Set<String> permisos = rolPermRepositorio.resolverPermisosDePersona(usuario.getPersonaId());
        Integer sucursalId = sucursalActiva == null ? null : sucursalActiva.getSucursalId();

        String accessToken = tokenServicio.emitirAccessToken(usuario, permisos, sucursalId);
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

        LoginRespuestaDTO respuesta = new LoginRespuestaDTO(
                accessToken,
                tokenServicio.duracionAccessTokenMinutos() * 60,
                sucursalId,
                usuarioMapeador.aUsuarioDTO(usuario),
                permisos.stream().sorted().toList(),
                esClaveCaducada(usuario));

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
