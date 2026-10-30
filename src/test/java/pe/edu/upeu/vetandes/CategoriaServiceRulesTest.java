package pe.edu.upeu.vetandes;

import pe.edu.upeu.vetandes.dto.CategoriaRequestDTO;
import pe.edu.upeu.vetandes.entity.Categoria;
import pe.edu.upeu.vetandes.exception.ReglaNegocioException;
import pe.edu.upeu.vetandes.repository.CategoriaRepository;
import pe.edu.upeu.vetandes.repository.ProductoRepository;
import pe.edu.upeu.vetandes.service.impl.CategoriaServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CategoriaServiceRulesTest {
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final CategoriaServiceImpl service = new CategoriaServiceImpl(categorias, productos);

    @Test void impideEliminarCategoriaConProductos() {
        when(categorias.findById(1L)).thenReturn(Optional.of(new Categoria()));
        when(productos.existsByCategoriaId(1L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.delete(1L));
        verify(categorias, never()).delete(any());
    }
    @Test void eliminaCategoriaIndependiente() {
        Categoria categoria = new Categoria();
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        service.delete(1L);
        verify(categorias).delete(categoria);
    }
    @Test void impideRenombrarConNombreDuplicado() {
        when(categorias.findById(1L)).thenReturn(Optional.of(new Categoria()));
        when(categorias.existsByNombreIgnoreCaseAndIdNot("Analgesicos", 1L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.update(1L,
                new CategoriaRequestDTO(" Analgesicos ", null, true)));
        verify(categorias, never()).save(any());
    }
}
