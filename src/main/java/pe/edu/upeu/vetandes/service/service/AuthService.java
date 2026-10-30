package pe.edu.upeu.vetandes.service.service;

import pe.edu.upeu.vetandes.dto.auth.CambioPasswordRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.LoginRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.LoginResponseDTO;
import pe.edu.upeu.vetandes.dto.auth.UsuarioResponseDTO;

public interface AuthService {

    void cambiarPassword(String username, CambioPasswordRequestDTO request);

    LoginResponseDTO login(LoginRequestDTO request);

    UsuarioResponseDTO usuarioActual(String username);
}