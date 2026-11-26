package com.avandesk.api.notificacao.service;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.notificacao.entity.ModeloNotificacao;
import com.avandesk.api.notificacao.entity.NotificacaoEmail;
import com.avandesk.api.notificacao.port.MensagemEmail;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ModelosDeEmail {

    private static final Locale PORTUGUES = Locale.forLanguageTag("pt-BR");
    private static final ZoneId FUSO = ZoneId.of("America/Recife");
    private static final DateTimeFormatter DATA_E_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", PORTUGUES);
    private static final Map<StatusChamado, String> ROTULOS = new EnumMap<>(StatusChamado.class);

    static {
        ROTULOS.put(StatusChamado.ABERTO, "Aberto");
        ROTULOS.put(StatusChamado.EM_TRIAGEM, "Em Triagem");
        ROTULOS.put(StatusChamado.EM_ATENDIMENTO, "Em Atendimento");
        ROTULOS.put(StatusChamado.AGUARDANDO, "Aguardando");
        ROTULOS.put(StatusChamado.RESOLVIDO, "Resolvido");
        ROTULOS.put(StatusChamado.REABERTO, "Reaberto");
        ROTULOS.put(StatusChamado.FECHADO, "Fechado");
        ROTULOS.put(StatusChamado.CANCELADO, "Cancelado");
        ROTULOS.put(StatusChamado.MESCLADO, "Mesclado");
    }

    private final TemplateEngine motor = criarMotor();

    public MensagemEmail montar(NotificacaoEmail notificacao) {
        Chamado chamado = notificacao.getChamado();
        boolean abertura = notificacao.getModelo() == ModeloNotificacao.ABERTURA;
        String rotuloNovo = ROTULOS.get(notificacao.getStatusNovo());

        Context contexto = new Context(PORTUGUES);
        contexto.setVariable("nome", chamado.getSolicitante().getNome());
        contexto.setVariable("codigo", chamado.getCodigo());
        contexto.setVariable("unidade", chamado.getUnidade().getNome());
        contexto.setVariable("categoria", chamado.getCategoria() == null ? "Não informada" : chamado.getCategoria().getNome());
        contexto.setVariable("abertoEm", formatar(chamado.getAbertoEm()));
        contexto.setVariable("atualizadoEm", formatar(notificacao.getCriadaEm()));
        contexto.setVariable("statusAnterior",
            notificacao.getStatusAnterior() == null ? null : ROTULOS.get(notificacao.getStatusAnterior()));
        contexto.setVariable("statusNovo", rotuloNovo);
        contexto.setVariable("orientacao", orientacao(notificacao, chamado.getCodigo()));

        String modelo = abertura ? "abertura" : "mudanca-status";
        String assunto = abertura
            ? "[Nexo] Chamado %s registrado".formatted(chamado.getCodigo())
            : "[Nexo] Chamado %s: %s".formatted(chamado.getCodigo(), rotuloNovo);
        return new MensagemEmail(chamado.getSolicitante().getEmail(), assunto,
            motor.process("email/" + modelo + ".txt", contexto), motor.process("email/" + modelo + ".html", contexto));
    }

    static String orientacao(NotificacaoEmail notificacao, String codigo) {
        if (notificacao.getModelo() == ModeloNotificacao.ABERTURA) {
            String inicio = "Guarde o código %s e informe-o sempre que falar com o suporte sobre este problema.".formatted(codigo);
            return notificacao.getStatusNovo() == StatusChamado.EM_ATENDIMENTO
                ? inicio + " Um analista já está cuidando do seu chamado."
                : inicio + " Você receberá um e-mail a cada etapa do atendimento.";
        }
        return switch (notificacao.getStatusNovo()) {
            case EM_TRIAGEM -> "A equipe de suporte está analisando a sua solicitação para definir a prioridade e o "
                + "responsável. Não é preciso fazer nada agora.";
            case EM_ATENDIMENTO -> "Um analista está trabalhando no seu chamado. Se surgir alguma informação nova, fale "
                + "com o suporte informando o código %s.".formatted(codigo);
            case AGUARDANDO -> orientacaoDaEspera(notificacao.getMotivoEspera(), codigo);
            case RESOLVIDO -> "O suporte registrou a solução do seu problema. Se ele continuar acontecendo, fale com o "
                + "suporte em até 7 dias informando o código %s para que o chamado seja reaberto.".formatted(codigo);
            default -> "Acompanhe o seu chamado com o suporte informando o código %s.".formatted(codigo);
        };
    }

    private static String orientacaoDaEspera(MotivoEspera motivo, String codigo) {
        if (motivo == MotivoEspera.SOLICITANTE) {
            return "A equipe precisa de uma informação ou ação sua para continuar. Fale com o suporte informando o "
                + "código %s. O prazo do atendimento fica pausado até o seu retorno.".formatted(codigo);
        }
        if (motivo == MotivoEspera.FORNECEDOR) {
            return "O atendimento depende do retorno de um fornecedor externo e continua assim que ele responder. "
                + "Não é preciso fazer nada agora.";
        }
        return "O atendimento aguarda a chegada de uma peça ou equipamento e continua assim que ela chegar. "
            + "Não é preciso fazer nada agora.";
    }

    private static String formatar(Instant instante) {
        return instante == null ? null : DATA_E_HORA.format(instante.atZone(FUSO));
    }

    private static TemplateEngine criarMotor() {
        TemplateEngine motor = new TemplateEngine();
        motor.addTemplateResolver(resolvedor(TemplateMode.HTML, "*.html", 1));
        motor.addTemplateResolver(resolvedor(TemplateMode.TEXT, "*.txt", 2));
        return motor;
    }

    private static ClassLoaderTemplateResolver resolvedor(TemplateMode modo, String padrao, int ordem) {
        ClassLoaderTemplateResolver resolvedor = new ClassLoaderTemplateResolver();
        resolvedor.setPrefix("templates/");
        resolvedor.setTemplateMode(modo);
        resolvedor.setResolvablePatterns(Set.of(padrao));
        resolvedor.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolvedor.setCheckExistence(true);
        resolvedor.setCacheable(true);
        resolvedor.setOrder(ordem);
        return resolvedor;
    }
}
