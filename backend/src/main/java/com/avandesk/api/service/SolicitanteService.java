package com.avandesk.api.service;

import com.avandesk.api.entity.Solicitante;
import com.avandesk.api.exception.RegraNegocioException;
import com.avandesk.api.repository.SolicitanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SolicitanteService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern TELEFONE_E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final String CONTATO_INVALIDO =
        "Contato deve ser um telefone no formato E.164 (ex.: +5581999999999) ou um e-mail.";

    private final SolicitanteRepository solicitanteRepository;

    public Solicitante identificarOuCriar(String nome, String contato) {
        String nomeInformado = nome == null || nome.isBlank() ? null : nome.trim();
        String texto = contato.trim();

        if (texto.contains("@")) {
            String email = texto.toLowerCase(Locale.ROOT);
            if (!EMAIL.matcher(email).matches()) {
                throw new RegraNegocioException(CONTATO_INVALIDO);
            }
            return solicitanteRepository.findByEmail(email)
                .orElseGet(() -> criar(nomeInformado, null, email));
        }

        String telefone = normalizarTelefone(texto);
        Optional<Solicitante> existente = nomeInformado == null
            ? solicitanteRepository.findFirstByTelefoneAndNomeIsNull(telefone)
            : solicitanteRepository.findFirstByTelefoneAndNomeIgnoreCase(telefone, nomeInformado);
        return existente.orElseGet(() -> criar(nomeInformado, telefone, null));
    }

    private static String normalizarTelefone(String texto) {
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 13) {
            throw new RegraNegocioException(CONTATO_INVALIDO);
        }
        String telefone = digitos.length() <= 11 ? "+55" + digitos : "+" + digitos;
        if (!TELEFONE_E164.matcher(telefone).matches()) {
            throw new RegraNegocioException(CONTATO_INVALIDO);
        }
        return telefone;
    }

    private Solicitante criar(String nome, String telefone, String email) {
        Solicitante solicitante = new Solicitante();
        solicitante.setNome(nome);
        solicitante.setTelefone(telefone);
        solicitante.setEmail(email);
        return solicitanteRepository.save(solicitante);
    }
}
