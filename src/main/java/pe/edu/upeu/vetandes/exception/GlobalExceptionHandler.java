package pe.edu.upeu.vetandes.exception;

import pe.edu.upeu.vetandes.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(
                LocalDateTime.now(), HttpStatus.CONFLICT.value(), "Conflict",
                "No se pudo completar la operación: existen datos duplicados o registros asociados",
                request.getRequestURI(), null));
    }
    @ExceptionHandler(RecursosNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> handlNotFound(
            RecursosNoEncontradoException ex,
            HttpServletRequest request
    ){
      ErrorResponseDTO error = new ErrorResponseDTO(
              LocalDateTime.now(),
              HttpStatus.NOT_FOUND.value(),
              "Not Found",
              ex.getMessage(),
              request.getRequestURI(),
              null
      );
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinesRule(
            ReglaNegocioException ex,
            HttpServletRequest request
    ){
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> validationError = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                validationError.put(error.getField(), error.getDefaultMessage()));

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Existen errores de validacion",
                request.getRequestURI(),
                validationError
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDTO(
                LocalDateTime.now(), 401, "Unauthorized", "Usuario o contraseña incorrectos",
                request.getRequestURI(), null));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                LocalDateTime.now(), 403, "Forbidden", "No tiene permisos para realizar esta operación",
                request.getRequestURI(), null));
    }
}
