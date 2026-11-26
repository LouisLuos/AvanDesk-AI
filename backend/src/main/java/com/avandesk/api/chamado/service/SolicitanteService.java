package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.dto.SolicitanteRequest;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.core.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SolicitanteService {

    private static final Pattern EMAIL = Pattern.compile(SolicitanteRequest.FORMATO_EMAIL);
    private static final Pattern TELEFONE_E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final String EMAIL_INVALIDO = "Informe um e-mail válido para o solicitante (ex.: recepcao@lumen.com.br).";
    private static final String TELEFONE_INVALIDO =
        "Telefone do solicitante deve ter DDD e número, no formato E.164 (ex.: +5581999999999).";

    private final SolicitanteRepository solicitanteRepository;

    public Solicitante identificarOuCriar(String nome, String email, String telefone) {
        String nomeInformado = nome == null || nome.isBlank() ? null : nome.trim();
        String emailNormalizado = normalizarEmail(email);
        String telefoneNormalizado = telefone == null || telefone.isBlank() ? null : normalizarTelefone(telefone);
        return solicitanteRepository.findByEmail(emailNormalizado)
            .map(existente -> completar(existente, nomeInformado, telefoneNormalizado))
            .orElseGet(() -> criar(nomeInformado, telefoneNormalizado, emailNormalizado));
    }

    private static String normalizarEmail(String email) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 254 || !EMAIL.matcher(normalizado).matches()) {
            throw new RegraNegocioException(EMAIL_INVALIDO);
        }
        return normalizado;
    }

    private static String normalizarTelefone(String texto) {
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 13) {
            throw new RegraNegocioException(TELEFONE_INVALIDO);
        }
        String telefone = digitos.length() <= 11 ? "+55" + digitos : "+" + digitos;
        if (!TELEFONE_E164.matcher(telefone).matches()) {
            throw new RegraNegocioException(TELEFONE_INVALIDO);
        }
        return telefone;
    }

    private static Solicitante completar(Solicitante existente, String nome, String telefone) {
        if (existente.getNome() == null && nome != null) {
            existente.setNome(nome);
        }
        if (existente.getTelefone() == null && telefone != null) {
            existente.setTelefone(telefone);
        }
        return existente;
    }

    private Solicitante criar(String nome, String telefone, String email) {
        Solicitante solicitante = new Solicitante();
        solicitante.setNome(nome);
        solicitante.setTelefone(telefone);
        solicitante.setEmail(email);
        return solicitanteRepository.save(solicitante);
    }
}
