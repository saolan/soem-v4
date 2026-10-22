package com.saolan.soem.seguridad.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saolan.soem.seguridad.dto.MenuNodoDTO;
import com.saolan.soem.seguridad.dto.MenuPlanoDTO;

/**
 * Arma el arbol de menu a partir de filas planas en O(n), sin acceso a datos
 * (funcion pura, probada sin base de datos).
 * <ul>
 * <li>Raices: filas sin {@code menuSupeId}. Una fila cuyo menu superior no
 * esta en la lista queda fuera junto con su subarbol: en el menu de la sesion
 * eso significa que el usuario no tiene acceso a algun ancestro, o que este
 * esta inactivo.</li>
 * <li>Las filas que forman un ciclo nunca son alcanzables desde una raiz, por
 * lo que tambien quedan fuera.</li>
 * <li>Cada nivel se ordena por {@code menuId} (convencion de la semilla: los
 * ids espaciados definen el orden de presentacion).</li>
 * </ul>
 */
public final class ConstructorArbolMenu {

    private ConstructorArbolMenu() {
    }

    /**
     * @param filas              menus a incluir, en cualquier orden
     * @param podarGruposVacios  si es {@code true}, se descartan los grupos
     *                           ({@code detall = false}) que quedan sin hijos
     */
    public static List<MenuNodoDTO> construir(List<MenuPlanoDTO> filas, boolean podarGruposVacios) {
        Map<Integer, List<MenuPlanoDTO>> hijosPorSuperior = new HashMap<>();
        List<MenuPlanoDTO> raices = new ArrayList<>();

        List<MenuPlanoDTO> ordenadas = filas.stream()
                .sorted(Comparator.comparing(MenuPlanoDTO::menuId))
                .toList();
        for (MenuPlanoDTO fila : ordenadas) {
            if (fila.menuSupeId() == null) {
                raices.add(fila);
            } else {
                hijosPorSuperior.computeIfAbsent(fila.menuSupeId(), clave -> new ArrayList<>()).add(fila);
            }
        }
        return construirNivel(raices, hijosPorSuperior, podarGruposVacios);
    }

    /**
     * Indexa las filas por {@code menuId} conservando el orden, para
     * recorrer la cadena de ancestros al validar la jerarquia.
     */
    public static Map<Integer, MenuPlanoDTO> indexarPorId(List<MenuPlanoDTO> filas) {
        Map<Integer, MenuPlanoDTO> indice = new LinkedHashMap<>();
        for (MenuPlanoDTO fila : filas) {
            indice.put(fila.menuId(), fila);
        }
        return indice;
    }

    private static List<MenuNodoDTO> construirNivel(List<MenuPlanoDTO> filas,
            Map<Integer, List<MenuPlanoDTO>> hijosPorSuperior, boolean podarGruposVacios) {
        List<MenuNodoDTO> nodos = new ArrayList<>();
        for (MenuPlanoDTO fila : filas) {
            List<MenuNodoDTO> hijos = construirNivel(
                    hijosPorSuperior.getOrDefault(fila.menuId(), List.of()), hijosPorSuperior, podarGruposVacios);
            boolean esGrupo = !Boolean.TRUE.equals(fila.detall());
            if (podarGruposVacios && esGrupo && hijos.isEmpty()) {
                continue;
            }
            nodos.add(new MenuNodoDTO(fila.menuId(), fila.descri(), fila.paginaRuta(), fila.detall(), fila.estado(),
                    hijos));
        }
        return nodos;
    }
}
