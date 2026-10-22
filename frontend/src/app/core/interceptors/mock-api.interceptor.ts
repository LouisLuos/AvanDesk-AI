import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
  HttpStatusCode,
} from '@angular/common/http';
import { Observable, delay, mergeMap, of, throwError, timer } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  CanalOrigem,
  ChamadoRequest,
  ChamadoResponse,
  ChamadoResumo,
  PrioridadeChamado,
  ResponsavelChamado,
  SlaChamado,
  StatusChamado,
} from '../../shared/models/chamado.model';
import { Unidade, UnidadeRequest } from '../../shared/models/unidade.model';
import { USUARIO_ATUAL } from '../../shared/utils/usuario-atual';

/**
 * API simulada em memória para desenvolver o frontend antes do backend ficar pronto.
 * Ativada por `environment.useMocks`. Responde no mesmo formato ApiResponse do contrato.
 * Dados fictícios — nenhum dado real de paciente ou colaborador.
 */

const LATENCIA_MS = 400;

const unidades: Unidade[] = [
  { id: 'a1f0c3e2-0001-4000-8000-000000000001', nome: 'U.03 Boa Viagem', identificacao: 'LUM-BVG', localizacao: 'Rua Boa Viagem, 123', atendimento_24h: false, ativo: true },
  { id: 'a1f0c3e2-0002-4000-8000-000000000002', nome: 'U.07 Recife Centro', identificacao: 'LUM-RCT', localizacao: 'Av Agamenon Magalhaes, 400', atendimento_24h: true, ativo: true },
  { id: 'a1f0c3e2-0003-4000-8000-000000000003', nome: 'U.11 Boa Vista', identificacao: 'LUM-BVT', localizacao: 'Rua da Aurora, 50', atendimento_24h: false, ativo: true },
  { id: 'a1f0c3e2-0004-4000-8000-000000000004', nome: 'U.14 Jaboatão', identificacao: 'LUM-JBT', localizacao: 'Av Barreto de Menezes, 200', atendimento_24h: false, ativo: true },
  { id: 'a1f0c3e2-0006-4000-8000-000000000006', nome: 'Sede Administrativa', identificacao: 'LUM-SED', localizacao: 'Rua do Bom Jesus, 10', atendimento_24h: false, ativo: true },
  { id: 'a1f0c3e2-0005-4000-8000-000000000005', nome: 'U.20 Caruaru', identificacao: 'LUM-CRU', localizacao: '', atendimento_24h: false, ativo: false },
];

const RESPONSAVEIS: Record<string, ResponsavelChamado> = {
  ana: { id: USUARIO_ATUAL.id, nome: USUARIO_ATUAL.nome },
  carlos: { id: 'usr-carlos-lima', nome: 'Carlos Lima' },
  rafael: { id: 'usr-rafael-dias', nome: 'Rafael Dias' },
  juliana: { id: 'usr-juliana-melo', nome: 'Juliana Melo' },
};

const minutosAtras = (minutos: number) => new Date(Date.now() - minutos * 60_000).toISOString();

const unidadeRef = (indice: number) => ({ id: unidades[indice].id, nome: unidades[indice].nome });

type SementeChamado = [
  numero: number,
  titulo: string,
  unidade: number,
  canal: CanalOrigem,
  status: StatusChamado,
  prioridade: PrioridadeChamado | null,
  sla: SlaChamado,
  responsavel: keyof typeof RESPONSAVEIS | null,
  minutosDesdeAtualizacao: number,
];

const SEMENTES: SementeChamado[] = [
  [127, 'Impressora de etiquetas parada na coleta', 0, 'LIGACAO', 'EM_ATENDIMENTO', 'ALTA', { situacao: 'EM_RISCO', rotulo: 'Em risco · 12 min', progresso: 86 }, 'ana', 6],
  [126, 'Lentidão no sistema de agendamento', 1, 'WHATSAPP', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 9 min', progresso: 40 }, null, 6],
  [125, 'Acesso bloqueado após troca de senha', 2, 'EMAIL', 'AGUARDANDO', 'BAIXA', { situacao: 'PAUSADO', rotulo: 'Pausado · solicitante', progresso: 30 }, 'carlos', 60],
  [124, 'Computador da recepção não liga', 3, 'PRESENCIAL', 'EM_ATENDIMENTO', 'MEDIA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 5 h', progresso: 35 }, 'rafael', 60],
  [123, 'Rede instável na recepção', 0, 'WHATSAPP', 'EM_TRIAGEM', 'ALTA', { situacao: 'ESTOURADO', rotulo: 'Estourado · +18 min', progresso: 100 }, null, 120],
  [122, 'Médico sem acesso aos resultados', 2, 'LIGACAO', 'REABERTO', 'MEDIA', { situacao: 'EM_RISCO', rotulo: 'Em risco · 1 h', progresso: 90 }, 'ana', 180],
  [121, 'Falha de impressão no RH', 4, 'LIGACAO', 'RESOLVIDO', 'BAIXA', { situacao: 'CUMPRIDO', rotulo: 'Cumprido · 2 h 10 min', progresso: 50 }, 'carlos', 1500],
  [120, 'Sistema de agendamento muito lento', 1, 'WHATSAPP', 'MESCLADO', 'MEDIA', { situacao: 'INCORPORADO', rotulo: 'Incorporado ao 000112', progresso: 0 }, 'ana', 1560],
  [119, 'Leitor de código de barras sem resposta', 3, 'PRESENCIAL', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 22 min', progresso: 70 }, null, 1620],
  [118, 'PACS não carrega imagens na estação 2', 1, 'LIGACAO', 'EM_ATENDIMENTO', 'ALTA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 1 h 40 min', progresso: 55 }, 'rafael', 1700],
  [117, 'Ramal da triagem sem linha', 0, 'EMAIL', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 4 min', progresso: 20 }, null, 1800],
  [116, 'Monitor piscando no guichê 3', 2, 'PRESENCIAL', 'EM_TRIAGEM', 'BAIXA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 7 h', progresso: 15 }, 'juliana', 2000],
  [115, 'LIS fora do ar na coleta', 3, 'LIGACAO', 'EM_ATENDIMENTO', 'ALTA', { situacao: 'ESTOURADO', rotulo: 'Estourado · +42 min', progresso: 100 }, 'ana', 2100],
  [114, 'Solicitação de novo e-mail corporativo', 4, 'EMAIL', 'AGUARDANDO', 'BAIXA', { situacao: 'PAUSADO', rotulo: 'Pausado · aprovação', progresso: 25 }, 'carlos', 2300],
  [113, 'Teclado com teclas travando', 0, 'WHATSAPP', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 14 min', progresso: 55 }, null, 2400],
  [112, 'Agendamento online com erro ao confirmar', 1, 'WHATSAPP', 'EM_ATENDIMENTO', 'MEDIA', { situacao: 'EM_RISCO', rotulo: 'Em risco · 25 min', progresso: 82 }, 'ana', 2500],
  [111, 'Impressora do laboratório com atolamento', 2, 'LIGACAO', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 18 min', progresso: 60 }, null, 2700],
  [110, 'VPN não conecta no notebook da diretoria', 4, 'EMAIL', 'EM_ATENDIMENTO', 'MEDIA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 3 h', progresso: 45 }, 'juliana', 2900],
  [109, 'Wi-Fi de visitantes indisponível', 3, 'PRESENCIAL', 'EM_TRIAGEM', 'BAIXA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 6 h', progresso: 20 }, null, 3000],
  [108, 'Certificado digital expirado na estação fiscal', 4, 'EMAIL', 'AGUARDANDO', 'MEDIA', { situacao: 'PAUSADO', rotulo: 'Pausado · fornecedor', progresso: 50 }, 'carlos', 3200],
  [107, 'Nobreak apitando na sala de servidores', 1, 'LIGACAO', 'EM_ATENDIMENTO', 'ALTA', { situacao: 'EM_RISCO', rotulo: 'Em risco · 30 min', progresso: 80 }, 'rafael', 3400],
  [106, 'Senha do sistema de ponto bloqueada', 0, 'WHATSAPP', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 7 min', progresso: 30 }, null, 3600],
  [105, 'Scanner de documentos não digitaliza', 2, 'PRESENCIAL', 'EM_TRIAGEM', 'BAIXA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 8 h', progresso: 10 }, 'juliana', 3800],
  [104, 'Etiquetadora imprimindo em branco', 3, 'LIGACAO', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 11 min', progresso: 45 }, null, 4000],
  [103, 'Perfil de acesso incorreto no LIS', 1, 'EMAIL', 'EM_ATENDIMENTO', 'MEDIA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 2 h', progresso: 60 }, 'ana', 4300],
  [102, 'Telefone IP sem registro', 0, 'PRESENCIAL', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 20 min', progresso: 65 }, null, 4500],
  [101, 'Lentidão na rede da recepção', 2, 'WHATSAPP', 'EM_ATENDIMENTO', 'ALTA', { situacao: 'ESTOURADO', rotulo: 'Estourado · +1 h', progresso: 100 }, 'carlos', 4700],
  [100, 'Instalação de software de planilhas', 4, 'EMAIL', 'AGUARDANDO', 'BAIXA', { situacao: 'PAUSADO', rotulo: 'Pausado · licença', progresso: 35 }, 'juliana', 5000],
  [99, 'Mouse sem fio sem resposta', 3, 'PRESENCIAL', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 3 min', progresso: 15 }, null, 5200],
  [98, 'Painel de senhas travado', 0, 'LIGACAO', 'EM_TRIAGEM', 'MEDIA', { situacao: 'DENTRO_DA_META', rotulo: 'Dentro da meta · 4 h', progresso: 30 }, null, 5500],
  [97, 'Atualização do navegador nas estações', 4, 'EMAIL', 'ABERTO', null, { situacao: 'TRIAGEM', rotulo: 'Triagem · 25 min', progresso: 75 }, null, 5800],
];

const ANO_ATUAL = new Date().getFullYear();

const chamados: ChamadoResumo[] = SEMENTES.map(
  ([numero, titulo, unidade, canal, status, prioridade, sla, responsavel, minutos]) => ({
    id: `chm-${numero}`,
    codigo: `LUM-${ANO_ATUAL}-${String(numero).padStart(6, '0')}`,
    titulo,
    unidade: unidadeRef(unidade),
    canal_origem: canal,
    status,
    prioridade,
    sla,
    responsavel: responsavel ? RESPONSAVEIS[responsavel] : null,
    atualizado_em: minutosAtras(minutos),
  }),
);

let sequencialChamado = 127;

export const mockApiInterceptor: HttpInterceptorFn = (req, next) => {
  const apiIndex = req.url.indexOf(environment.apiUrl);
  if (!environment.useMocks || apiIndex === -1) {
    return next(req);
  }

  const caminho = req.url.substring(apiIndex + environment.apiUrl.length);

  if (caminho === '/unidades') {
    if (req.method === 'GET') return listarUnidades(req);
    if (req.method === 'POST') return cadastrarUnidade(req as HttpRequest<UnidadeRequest>);
  }

  const desativar = caminho.match(/^\/unidades\/([^/]+)\/desativar$/);
  if (desativar && req.method === 'PATCH') return desativarUnidade(desativar[1]);

  const editar = caminho.match(/^\/unidades\/([^/]+)$/);
  if (editar && req.method === 'PUT') {
    return editarUnidade(editar[1], req as HttpRequest<UnidadeRequest>);
  }

  if (caminho === '/chamados' && req.method === 'GET') {
    return sucesso(
      HttpStatusCode.Ok,
      'Chamados recuperados',
      chamados.map((c) => ({ ...c })),
    ).pipe(delay(LATENCIA_MS));
  }

  if (caminho === '/chamados' && req.method === 'POST') {
    return registrarChamado(req as HttpRequest<ChamadoRequest>);
  }

  return next(req);
};

// ---------- Unidades ----------

function listarUnidades(req: HttpRequest<unknown>): Observable<HttpResponse<unknown>> {
  const filtro = req.params.get('ativo');
  const lista =
    filtro === null ? unidades : unidades.filter((u) => String(u.ativo) === filtro);
  return sucesso(HttpStatusCode.Ok, 'Unidades recuperadas', lista.map((u) => ({ ...u })));
}

function cadastrarUnidade(req: HttpRequest<UnidadeRequest>): Observable<HttpResponse<unknown>> {
  const erros = validarUnidade(req.body);
  if (erros.length) return erro(HttpStatusCode.BadRequest, 'Erro de validação', erros);

  const nova: Unidade = {
    id: crypto.randomUUID(),
    nome: req.body!.nome.trim(),
    identificacao: req.body!.identificacao.trim().toUpperCase(),
    localizacao: req.body!.localizacao?.trim() || '',
    atendimento_24h: req.body!.atendimento_24h,
    ativo: true,
  };
  unidades.push(nova);
  return sucesso(HttpStatusCode.Created, 'Unidade cadastrada com sucesso', { ...nova });
}

function editarUnidade(
  id: string,
  req: HttpRequest<UnidadeRequest>,
): Observable<HttpResponse<unknown>> {
  const unidade = unidades.find((u) => u.id === id);
  if (!unidade) return erro(HttpStatusCode.NotFound, 'Unidade não encontrada', null);

  const erros = validarUnidade(req.body, id);
  if (erros.length) return erro(HttpStatusCode.BadRequest, 'Erro de validação', erros);

  unidade.nome = req.body!.nome.trim();
  unidade.identificacao = req.body!.identificacao.trim().toUpperCase();
  unidade.localizacao = req.body!.localizacao?.trim() || '';
  unidade.atendimento_24h = req.body!.atendimento_24h;
  return sucesso(HttpStatusCode.Ok, 'Unidade atualizada com sucesso', { ...unidade });
}

function desativarUnidade(id: string): Observable<HttpResponse<unknown>> {
  const unidade = unidades.find((u) => u.id === id);
  if (!unidade) return erro(HttpStatusCode.NotFound, 'Unidade não encontrada', null);

  unidade.ativo = false;
  return sucesso(HttpStatusCode.Ok, 'Unidade desativada com sucesso', { ...unidade });
}

function validarUnidade(body: UnidadeRequest | null, idAtual?: string): string[] {
  const erros: string[] = [];
  if (!body?.nome?.trim()) erros.push("Campo 'nome' é obrigatório.");
  if (!body?.identificacao?.trim()) erros.push("Campo 'identificacao' é obrigatório.");
  const identificacao = body?.identificacao?.trim().toUpperCase();
  if (identificacao && unidades.some((u) => u.identificacao === identificacao && u.id !== idAtual)) {
    erros.push('Já existe uma unidade com a identificação informada.');
  }
  return erros;
}

// ---------- Chamados ----------

function registrarChamado(req: HttpRequest<ChamadoRequest>): Observable<HttpResponse<unknown>> {
  const body = req.body;
  const erros: string[] = [];
  if (!body?.unidade_id) erros.push("Campo 'unidade_id' é obrigatório.");
  if (!body?.canal_origem) erros.push("Campo 'canal_origem' é obrigatório.");
  if (!body?.solicitante?.contato?.trim()) erros.push("Campo 'solicitante.contato' é obrigatório.");
  if (!body?.descricao?.trim()) erros.push("Campo 'descricao' é obrigatório.");

  const unidade = unidades.find((u) => u.id === body?.unidade_id);
  if (body?.unidade_id && (!unidade || !unidade.ativo)) {
    erros.push('A unidade informada não existe ou está inativa.');
  }
  if (erros.length) return erro(HttpStatusCode.BadRequest, 'Erro de validação', erros);

  sequencialChamado++;
  const chamado: ChamadoResponse = {
    id: crypto.randomUUID(),
    codigo: `LUM-${new Date().getFullYear()}-${String(sequencialChamado).padStart(6, '0')}`,
    status: 'ABERTO',
    data_criacao: new Date().toISOString(),
    // Como no backend: o servidor marca a origem; aqui todo dado é fictício.
    origem_dado: 'SIMULADO',
  };
  chamados.unshift({
    id: chamado.id,
    codigo: chamado.codigo,
    titulo: body!.descricao.trim().split('\n')[0].slice(0, 80),
    unidade: { id: unidade!.id, nome: unidade!.nome },
    canal_origem: body!.canal_origem,
    status: 'ABERTO',
    prioridade: body!.prioridade ?? null,
    sla: { situacao: 'TRIAGEM', rotulo: 'Triagem · 0 min', progresso: 0 },
    responsavel: body!.atribuir_a_mim ? { id: USUARIO_ATUAL.id, nome: USUARIO_ATUAL.nome } : null,
    atualizado_em: chamado.data_criacao,
  });
  return sucesso(HttpStatusCode.Created, 'Chamado registrado com sucesso', chamado);
}

// ---------- Helpers ----------

function sucesso<T>(status: number, message: string, data: T): Observable<HttpResponse<unknown>> {
  const body: ApiResponse<T> = { status_code: status, message, data, errors: null };
  return of(new HttpResponse({ status, body }));
}

function erro(
  status: number,
  message: string,
  errors: string[] | null,
): Observable<never> {
  const body: ApiResponse<null> = { status_code: status, message, data: null, errors };
  return throwError(() => new HttpErrorResponse({ status, error: body }));
}
