package br.com.bilca.event.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminAuth {

    private final String adminApiToken;

    public AdminAuth(@Value("${bilca.admin-api-token:}") String adminApiToken) {
        this.adminApiToken = adminApiToken;
    }

    public void require(String token) {
        if (adminApiToken.isBlank() || !adminApiToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token administrativo inválido");
        }
    }
}
