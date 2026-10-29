import {
  CanalOrigem,
  ChamadoDetalhe,
  ChamadoDetalheApiResponse,
  EventoHistoricoApiResponse,
  EventoTimeline,
  ItemSlaDetalhe,
  MotivoEspera,
  PrioridadeChamado,
  RegistroContatoApiResponse,
  RegistroContatoDetalhe,
  StatusChamado,
  TipoEventoHistorico,
} from '../models/chamado.model';

const ROTULO_CANAL: Record<CanalOrigem, string> = {
  LIGACAO: 'Ligação',
  WHATSAPP: 'WhatsApp',
  EMAIL: 'E-mail',
  PRESENCIAL: 'Presencial',
};

const ROTULO_STATUS: Record<StatusChamado, string> = {
  ABERTO: 'Aberto',
  EM_TRIAGEM: 'Em Triagem',
  EM_ATENDIMENTO: 'Em Atendimento',
  AGUARDANDO: 'Aguardando',
  RESOLVIDO: 'Resolvido',
  REABERTO: 'Reaberto',
  FECHADO: 'Fechado',
  CANCELADO: 'Cancelado',
  MESCLADO: 'Mesclado',
};

const ROTULO_PRIORIDADE: Record<PrioridadeChamado, string> = {
  ALTA: 'Alta (P1)',
  MEDIA: 'Média (P2)',
  BAIXA: 'Baixa (P3)',
};

const ROTULO_MOTIVO_ESPERA: Record<MotivoEspera, string> = {
  FORNECEDOR: 'Fornecedor externo',
  SOLICITANTE: 'Retorno do solicitante',
  PECA_EQUIPAMENTO: 'Peça ou equipamento',
};

const ESTILO_DOT_EVENTO: Record<TipoEventoHistorico | string, string> = {
  CHAMADO_ABERTO: 'bg-[#94a3b8]',
  REGISTRO_DE_CONTATO_RECEBIDO: 'bg-[#2d5bff]',
  TRIAGEM_INICIADA: 'bg-[#14b8a6]',
  PRIORIDADE_DEFINIDA: 'bg-[#dc2626]',
  CHAMADO_TRIADO: 'bg-[#14b8a6]',
  CHAMADO_ATRIBUIDO: 'bg-[#2d5bff]',
  STATUS_ALTERADO: 'bg-[#0ea5e9]',
  CHAMADO_AGUARDANDO: 'bg-[#f0a202]',
  RETORNO_RECEBIDO: 'bg-[#0ea5e9]',
  CHAMADO_ESCALADO: 'bg-[#f59e0b]',
  CHAMADO_RESOLVIDO: 'bg-[#22c55e]',
  CHAMADO_REABERTO: 'bg-[#e5484d]',
  CHAMADO_FECHADO: 'bg-[#64748b]',
  CHAMADO_CANCELADO: 'bg-[#94a3b8]',
  RELATO_VINCULADO: 'bg-[#8b5cf6]',
  RELATO_DESVINCULADO: 'bg-[#94a3b8]',
  CHAMADO_MESCLADO: 'bg-[#8b5cf6]',
  NOTIFICACAO_ENVIADA: 'bg-[#64748b]',
  ANEXO_VISUALIZADO: 'bg-[#4b5563]',
  ANOTACAO_ADICIONADA: 'bg-[#0ea5e9]',
};

const FORMATADOR_DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const FORMATADOR_HORA = new Intl.DateTimeFormat('pt-BR', {
  hour: '2-digit',
  minute: '2-digit',
});

/**
 * Converte um timestamp ISO-8601 UTC vindo do backend para o fuso horário local do navegador.
 * Se a string já vier formatada para exibição (ex.: fixtures de teste), preserva o valor original.
 */
export function formatarDataHoraLocal(valor?: string | null): string {
  if (!valor) return '';
  if (!/^\d{4}-\d{2}-\d{2}T/.test(valor)) {
    return valor;
  }
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) {
    return valor;
  }
  return FORMATADOR_DATA_HORA.format(data).replace(',', '');
}

export function formatarHoraLocal(valor?: string | null): string {
  if (!valor) return '';
  if (!/^\d{4}-\d{2}-\d{2}T/.test(valor)) {
    return valor;
  }
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) {
    return valor;
  }
  return FORMATADOR_HORA.format(data);
}

/**
 * Mascara números de telefone em formato E.164 (+5581988771234 -> +55 81 9****-1234)
 * para exibição segura na tela (LGPD). E-mails são mantidos sem máscara de telefone.
 */
export function mascararTelefoneSolicitante(contato?: string | null): string | undefined {
  if (!contato || contato.includes('@')) {
    return undefined;
  }
  const celularBr = contato.match(/^\+55(\d{2})(\d)(\d{4})(\d{4})$/);
  if (celularBr) {
    return `+55 ${celularBr[1]} ${celularBr[2]}****-${celularBr[4]}`;
  }
  const fixoBr = contato.match(/^\+55(\d{2})(\d{4})(\d{4})$/);
  if (fixoBr) {
    return `+55 ${fixoBr[1]} ****-${fixoBr[3]}`;
  }
  return undefined;
}

/**
 * Adapta a resposta bruta do endpoint GET /api/v1/chamados/{id} (ChamadoDetalheApiResponse)
 * para o ViewModel ChamadoDetalhe consumido pela tela DetalheChamado (HU02 e HU05).
 */
export function mapearChamadoDetalhe(
  entrada: ChamadoDetalheApiResponse | ChamadoDetalhe,
): ChamadoDetalhe {
  if (ehChamadoDetalheViewModel(entrada)) {
    return {
      ...entrada,
      aberto_em: formatarDataHoraLocal(entrada.aberto_em),
      primeira_resposta_em: entrada.primeira_resposta_em
        ? formatarDataHoraLocal(entrada.primeira_resposta_em)
        : null,
      solicitante: {
        ...entrada.solicitante,
        telefoneMascarado:
          entrada.solicitante.telefoneMascarado ??
          mascararTelefoneSolicitante(entrada.solicitante.contato),
      },
      registrosContato: entrada.registrosContato ?? [],
      evidencias: entrada.evidencias ?? [],
      historico: entrada.historico ?? [],
      slaItens: entrada.slaItens ?? construirSlaPadrao(entrada.aberto_em, entrada.primeira_resposta_em),
      relatosVinculados: entrada.relatosVinculados ?? [],
    };
  }

  const contatosOrdenados = [...(entrada.registros_contato ?? [])].sort(
    (a, b) => new Date(a.ocorrido_em).getTime() - new Date(b.ocorrido_em).getTime(),
  );

  const contatosPorId = new Map<string, RegistroContatoApiResponse>(
    contatosOrdenados.map((c) => [c.id, c]),
  );

  const eventosOrdenados = [...(entrada.historico ?? [])].sort(
    (a, b) => new Date(a.data).getTime() - new Date(b.data).getTime(),
  );

  const historicoMapeado: EventoTimeline[] = eventosOrdenados.map((evento) =>
    mapearEventoHistorico(evento, entrada.canal_origem, contatosPorId),
  );

  const registrosContato: RegistroContatoDetalhe[] = contatosOrdenados.map((c) => ({
    id: c.id,
    canal: c.canal,
    direcao: c.direcao,
    conteudo: c.conteudo,
    dataHoraLocal: formatarDataHoraLocal(c.ocorrido_em),
    solicitanteNome: c.solicitante?.nome?.trim() || 'Solicitante não informado',
    solicitanteContato:
      mascararTelefoneSolicitante(c.solicitante?.contato) ?? c.solicitante?.contato ?? '',
    registradoPor: c.registrado_por ?? null,
  }));

  const primeiraLinhaDescricao =
    entrada.descricao?.trim().split('\n')[0]?.slice(0, 80) || entrada.codigo;

  return {
    id: entrada.id,
    codigo: entrada.codigo,
    titulo: primeiraLinhaDescricao,
    descricao: entrada.descricao,
    unidade: entrada.unidade,
    solicitante: {
      nome: entrada.solicitante?.nome ?? undefined,
      contato: entrada.solicitante?.contato ?? '',
      telefoneMascarado: mascararTelefoneSolicitante(entrada.solicitante?.contato),
    },
    canal_origem: entrada.canal_origem,
    status: entrada.status,
    motivo_espera: entrada.motivo_espera ?? null,
    prioridade: entrada.prioridade ?? null,
    categoria: entrada.categoria?.nome ?? '',
    sistema: entrada.sistema_equipamento?.nome ?? '',
    responsavel: entrada.responsavel ?? null,
    aberto_em: formatarDataHoraLocal(entrada.aberto_em),
    primeira_resposta_em: entrada.primeira_resposta_em
      ? formatarDataHoraLocal(entrada.primeira_resposta_em)
      : null,
    registrosContato,
    evidencias: [],
    historico: historicoMapeado,
    slaItens: construirSlaPadrao(entrada.aberto_em, entrada.primeira_resposta_em),
    pausasMinutos: 0,
    tempoTerceirosHoras: 0,
    relatosVinculados: [],
    origem_dado: entrada.origem_dado,
  };
}

function ehChamadoDetalheViewModel(
  entrada: ChamadoDetalheApiResponse | ChamadoDetalhe,
): entrada is ChamadoDetalhe {
  return typeof (entrada as ChamadoDetalhe).categoria === 'string' && 'slaItens' in entrada;
}

function mapearEventoHistorico(
  evento: EventoHistoricoApiResponse,
  canalPadrao: CanalOrigem,
  contatosPorId: Map<string, RegistroContatoApiResponse>,
): EventoTimeline {
  const autor = evento.autor ?? undefined;
  const detalhes = evento.detalhes ?? {};
  const dataCompleta = formatarDataHoraLocal(evento.data);
  const hora = formatarHoraLocal(evento.data);
  const estiloDot = ESTILO_DOT_EVENTO[evento.tipo] ?? 'bg-[#94a3b8]';

  const { titulo, descricao } = construirTextoEvento(
    evento.tipo,
    autor,
    detalhes,
    canalPadrao,
    contatosPorId,
  );

  return {
    id: evento.id_evento,
    tipo: evento.tipo,
    titulo,
    descricao,
    autor,
    hora,
    dataCompleta,
    ocorridoEmIso: evento.data,
    estiloDot,
  };
}

function formatarNomeEvento(tipo: string): string {
  if (!tipo) return 'Evento do chamado';
  const palavras = tipo.toLowerCase().split('_');
  const capitalizadas = palavras.map(
    (p, i) => (i === 0 || p.length > 2 ? p.charAt(0).toUpperCase() + p.slice(1) : p)
  );
  return capitalizadas.join(' ');
}

function construirTextoEvento(
  tipo: string,
  autor: string | undefined,
  detalhes: Record<string, unknown>,
  canalPadrao: CanalOrigem,
  contatosPorId: Map<string, RegistroContatoApiResponse>,
): { titulo: string; descricao?: string } {
  const sufixoAutor = autor ? `Registrado por ${autor}` : undefined;

  switch (tipo) {
    case 'CHAMADO_ABERTO': {
      const canalBruto = (detalhes['canal'] as CanalOrigem | undefined) ?? canalPadrao;
      const rotuloCanal = ROTULO_CANAL[canalBruto] ?? canalBruto;
      return {
        titulo: `Chamado aberto via ${rotuloCanal}`,
        descricao: sufixoAutor,
      };
    }

    case 'REGISTRO_DE_CONTATO_RECEBIDO': {
      const contatoId = detalhes['registro_contato_id'] as string | undefined;
      const contato = contatoId ? contatosPorId.get(contatoId) : undefined;
      if (contato) {
        const rotuloCanal = ROTULO_CANAL[contato.canal] ?? contato.canal;
        const quemRelatou = contato.solicitante?.nome?.trim() || contato.solicitante?.contato || 'Solicitante';
        const resumoConteudo =
          contato.conteudo.length > 140 ? `${contato.conteudo.slice(0, 140)}...` : contato.conteudo;
        return {
          titulo: `Contato registrado via ${rotuloCanal} (${quemRelatou})`,
          descricao: autor ? `"${resumoConteudo}" · Registrado por ${autor}` : `"${resumoConteudo}"`,
        };
      }
      return {
        titulo: 'Contato registrado no chamado',
        descricao: sufixoAutor,
      };
    }

    case 'TRIAGEM_INICIADA':
      return {
        titulo: 'Triagem iniciada',
        descricao: sufixoAutor,
      };

    case 'PRIORIDADE_DEFINIDA':
    case 'CHAMADO_TRIADO': {
      const prio = detalhes['prioridade_final'] as PrioridadeChamado | undefined;
      const rotuloPrio = prio ? (ROTULO_PRIORIDADE[prio] ?? prio) : '';
      return {
        titulo: rotuloPrio ? `Prioridade definida: ${rotuloPrio}` : 'Chamado triado',
        descricao: sufixoAutor,
      };
    }

    case 'CHAMADO_ATRIBUIDO': {
      const responsavelNome = (detalhes['responsavel_nome'] as string | undefined) ?? autor;
      return {
        titulo: responsavelNome ? `Atribuído a ${responsavelNome}` : 'Responsável atribuído',
        descricao: sufixoAutor,
      };
    }

    case 'CHAMADO_AGUARDANDO': {
      const motivo = detalhes['motivo_espera'] as MotivoEspera | undefined;
      const rotuloMotivo = motivo ? (ROTULO_MOTIVO_ESPERA[motivo] ?? motivo) : '';
      return {
        titulo: rotuloMotivo ? `Aguardando: ${rotuloMotivo}` : 'Status alterado para Aguardando',
        descricao: sufixoAutor,
      };
    }

    case 'STATUS_ALTERADO': {
      const novoStatus = detalhes['status'] as StatusChamado | undefined;
      const rotuloNovoStatus = novoStatus ? (ROTULO_STATUS[novoStatus] ?? novoStatus) : '';
      return {
        titulo: rotuloNovoStatus
          ? `Status alterado para ${rotuloNovoStatus}`
          : 'Status do chamado atualizado',
        descricao: sufixoAutor,
      };
    }

    case 'RETORNO_RECEBIDO':
      return {
        titulo: 'Retorno recebido · Atendimento retomado',
        descricao: sufixoAutor,
      };

    case 'CHAMADO_ESCALADO': {
      const fornecedor = detalhes['fornecedor'] as string | undefined;
      return {
        titulo: fornecedor
          ? `Escalado para fornecedor: ${fornecedor}`
          : 'Chamado escalado para fornecedor externo',
        descricao: sufixoAutor,
      };
    }

    case 'CHAMADO_RESOLVIDO':
      return {
        titulo: 'Chamado resolvido',
        descricao: sufixoAutor,
      };

    case 'CHAMADO_REABERTO':
      return {
        titulo: 'Chamado reaberto',
        descricao: sufixoAutor,
      };

    case 'CHAMADO_FECHADO':
      return {
        titulo: 'Chamado encerrado com rastreabilidade verificada',
        descricao: sufixoAutor,
      };

    case 'CHAMADO_CANCELADO':
      return {
        titulo: 'Chamado cancelado',
        descricao: sufixoAutor,
      };

    case 'RELATO_VINCULADO':
      return {
        titulo: 'Relato duplicado vinculado ao chamado',
        descricao: sufixoAutor,
      };

    case 'RELATO_DESVINCULADO':
      return {
        titulo: 'Relato desvinculado do chamado',
        descricao: sufixoAutor,
      };

    case 'CHAMADO_MESCLADO':
      return {
        titulo: 'Chamado mesclado ao atendimento principal',
        descricao: sufixoAutor,
      };

    case 'NOTIFICACAO_ENVIADA':
      return {
        titulo: 'Notificação enviada',
        descricao: sufixoAutor,
      };

    case 'ANEXO_VISUALIZADO':
      return {
        titulo: 'Evidência visualizada (auditoria LGPD)',
        descricao: sufixoAutor,
      };

    case 'ANOTACAO_ADICIONADA':
      return {
        titulo: 'Anotação técnica adicionada',
        descricao: sufixoAutor,
      };

    default:
      return {
        titulo: formatarNomeEvento(tipo),
        descricao: sufixoAutor,
      };
  }
}

function construirSlaPadrao(
  abertoEmIso?: string | null,
  primeiraRespostaEmIso?: string | null,
): ItemSlaDetalhe[] {
  const temPrimeiraResposta = Boolean(primeiraRespostaEmIso);
  return [
    {
      etapa: 'Triagem',
      meta: '15 min',
      situacaoTexto: temPrimeiraResposta ? 'Cumprida' : 'Em andamento na fila',
      progresso: temPrimeiraResposta ? 100 : 25,
      estiloBarra: temPrimeiraResposta ? 'bg-[#22c55e]' : 'bg-[#14b8a6]',
    },
    {
      etapa: 'Primeira resposta',
      meta: '15 min',
      situacaoTexto: temPrimeiraResposta ? 'Respondido pelo suporte' : 'Aguardando 1ª ação',
      progresso: temPrimeiraResposta ? 100 : 10,
      estiloBarra: temPrimeiraResposta ? 'bg-[#22c55e]' : 'bg-[#94a3b8]',
    },
    {
      etapa: 'Resolução',
      meta: '2 h',
      situacaoTexto: abertoEmIso ? 'Contabilizando desde a abertura' : 'Dentro da meta',
      progresso: 15,
      estiloBarra: 'bg-[#22c55e]',
    },
  ];
}
