package pe.edu.upeu.vetandes.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PaginaResponseDTO<T>(List<T> contenido, int pagina, int tamanio,
        long totalElementos, int totalPaginas, boolean primera, boolean ultima) {
    public static <T> PaginaResponseDTO<T> from(Page<T> page) {
        return new PaginaResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
