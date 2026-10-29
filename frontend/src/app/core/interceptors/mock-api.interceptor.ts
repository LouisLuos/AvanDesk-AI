import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
  HttpStatusCode,
} from '@angular/common/http';
import { Observable, delay, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  CanalOrigem,
  ChamadoDetalhe,
  ChamadoRequest,
  ChamadoResponse,
  ChamadoResumo,
  ItemLista,
  PrioridadeChamado,
  ResponsavelChamado,
  SlaChamado,
  StatusChamado,
} from '../../shared/models/chamado.model';
import { Unidade, UnidadeRequest } from '../../shared/models/unidade.model';
import { USUARIO_ATUAL } from '../../shared/utils/usuario-atual';

/**
 * API simulada em memória para desenvolver o frontend antes do backend ficar pronto.
 * Ativada por `environment.useMocks`. Quando `useMocks = false` (modo integração),
 * repassa todos os endpoints já implementados no backend (/auth/*, /unidades*,
 * /categorias, /sistemas-equipamentos, POST /chamados e GET /chamados/{uuid}) para o servidor real,
 * mantendo apenas o fallback provisório de GET /chamados e sementes chm-* até a HU14.
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

/** Mesmos nomes do seed de dev do backend (db/seed/R__dados_dev.sql), já em ordem alfabética. */
const categorias: ItemLista[] = [
  { id: 'c4e0a000-0001-4000-8000-000000000001', nome: 'Acesso e senhas' },
  { id: 'c4e0a000-0002-4000-8000-000000000002', nome: 'Equipamento de apoio' },
  { id: 'c4e0a000-0003-4000-8000-000000000003', nome: 'Infraestrutura / Rede' },
  { id: 'c4e0a000-0004-4000-8000-000000000004', nome: 'Outro' },
  { id: 'c4e0a000-0005-4000-8000-000000000005', nome: 'Sistema hospitalar' },
  { id: 'c4e0a000-0006-4000-8000-000000000006', nome: 'Telefonia e comunicação' },
];

const sistemasEquipamentos: ItemLista[] = [
  { id: '5e5a0000-0001-4000-8000-000000000001', nome: 'Computador da recepção' },
  { id: '5e5a0000-0002-4000-8000-000000000002', nome: 'Impressora comum' },
  { id: '5e5a0000-0003-4000-8000-000000000003', nome: 'Impressora de etiquetas' },
  { id: '5e5a0000-0004-4000-8000-000000000004', nome: 'Outro' },
  { id: '5e5a0000-0005-4000-8000-000000000005', nome: 'Portal de resultados' },
  { id: '5e5a0000-0006-4000-8000-000000000006', nome: 'Rede / Wi-Fi' },
  { id: '5e5a0000-0007-4000-8000-000000000007', nome: 'Sistema de agendamento' },
  { id: '5e5a0000-0008-4000-8000-000000000008', nome: 'Telefone / ramal' },
];

export const mockApiInterceptor: HttpInterceptorFn = (req, next) => {
  const apiIndex = req.url.indexOf(environment.apiUrl);
  if (apiIndex === -1) {
    return next(req);
  }

  const caminho = req.url.substring(apiIndex + environment.apiUrl.length);

  // Endpoint ainda não implementado no backend (HU14: GET /chamados)
  if (caminho === '/chamados' && req.method === 'GET') {
    return sucesso(
      HttpStatusCode.Ok,
      'Chamados recuperados',
      chamados.map((c) => ({ ...c })),
    ).pipe(delay(LATENCIA_MS));
  }

  const detalheMatch = caminho.match(/^\/chamados\/([^/]+)$/);
  if (detalheMatch && req.method === 'GET') {
    const idParam = detalheMatch[1];
    const ehUuidReal = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      idParam,
    );
    // Na HU02/HU05, GET /api/v1/chamados/{id} está pronto no backend para UUIDs reais;
    // sementes locais provisórias da fila (chm-*) continuam usando o fallback em memória até a HU14.
    if (!environment.useMocks && ehUuidReal) {
      return next(req);
    }
    return obterDetalheChamado(idParam);
  }

  // Quando o modo simulado está desligado (useMocks = false), todas as rotas de backend já prontas
  // (/auth/*, /unidades*, /categorias, /sistemas-equipamentos, POST /chamados e GET /chamados/{id}) vão ao servidor real.
  if (!environment.useMocks) {
    if (caminho === '/unidades' && req.method === 'GET') {
      return next(req).pipe(
        tap((evento) => {
          if (evento instanceof HttpResponse) {
            const corpo = evento.body as ApiResponse<Unidade[]> | null;
            if (corpo?.data?.length) {
              for (const u of corpo.data) {
                const idx = unidades.findIndex((item) => item.id === u.id);
                if (idx >= 0) unidades[idx] = { ...u };
                else unidades.push({ ...u });
              }
            }
          }
        }),
      );
    }

    if (caminho === '/chamados' && req.method === 'POST') {
      const payload = req.body as ChamadoRequest | null;
      return next(req).pipe(
        tap((evento) => {
          if (evento instanceof HttpResponse && payload) {
            const corpo = evento.body as ApiResponse<ChamadoResponse> | null;
            const criado = corpo?.data;
            if (criado) {
              const unidade = unidades.find((u) => u.id === payload.unidade_id);
              chamados.unshift({
                id: criado.id,
                codigo: criado.codigo,
                titulo: payload.descricao.trim().split('\n')[0].slice(0, 80),
                unidade: { id: payload.unidade_id, nome: unidade?.nome ?? 'Unidade' },
                canal_origem: payload.canal_origem,
                status: criado.status ?? 'ABERTO',
                prioridade: payload.prioridade ?? null,
                sla: { situacao: 'TRIAGEM', rotulo: 'Triagem · 0 min', progresso: 0 },
                responsavel: payload.atribuir_a_mim
                  ? { id: USUARIO_ATUAL.id, nome: USUARIO_ATUAL.nome }
                  : null,
                atualizado_em: criado.data_criacao,
              });
            }
          }
        }),
      );
    }

    return next(req);
  }

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

  if (caminho === '/auth/login' && req.method === 'POST') {
    return loginMock(req);
  }

  if (caminho === '/auth/logout' && req.method === 'POST') {
    return sucesso(HttpStatusCode.Ok, 'Logout realizado com sucesso', null);
  }

  if (caminho === '/chamados' && req.method === 'POST') {
    return registrarChamado(req as HttpRequest<ChamadoRequest>);
  }

  if (caminho === '/categorias' && req.method === 'GET') {
    return sucesso(HttpStatusCode.Ok, 'Categorias recuperadas com sucesso', [...categorias]);
  }

  if (caminho === '/sistemas-equipamentos' && req.method === 'GET') {
    return sucesso(HttpStatusCode.Ok, 'Sistemas e equipamentos recuperados com sucesso', [
      ...sistemasEquipamentos,
    ]);
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
  if (!body?.categoria_id) erros.push("Campo 'categoria_id' é obrigatório.");

  const unidade = unidades.find((u) => u.id === body?.unidade_id);
  if (body?.unidade_id && (!unidade || !unidade.ativo)) {
    erros.push('A unidade informada não existe ou está inativa.');
  }
  if (erros.length) return erro(HttpStatusCode.BadRequest, 'Erro de validação', erros);

  // Como no backend (PR #15): id que não existe na lista volta 404.
  if (!categorias.some((c) => c.id === body!.categoria_id)) {
    return erro(HttpStatusCode.NotFound, 'Categoria não encontrada.', null);
  }
  const sistemaId = body!.sistema_equipamento_id;
  if (sistemaId && !sistemasEquipamentos.some((s) => s.id === sistemaId)) {
    return erro(HttpStatusCode.NotFound, 'Sistema/equipamento não encontrado.', null);
  }

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

function loginMock(req: HttpRequest<unknown>): Observable<HttpResponse<unknown>> {
  const body = req.body as { email?: string; senha?: string };
  if (!body?.email?.trim() || !body?.senha?.trim()) {
    return erro(HttpStatusCode.BadRequest, 'Erro de validação', ['E-mail e senha são obrigatórios.']);
  }
  return sucesso(HttpStatusCode.Ok, 'Autenticação realizada com sucesso', {
    access_token: 'mock-jwt-token-lumen-2026',
    usuario: {
      id: USUARIO_ATUAL.id,
      nome: USUARIO_ATUAL.nome,
      email: body.email.includes('@') ? body.email : `${body.email}@lumen.local`,
      perfil: USUARIO_ATUAL.perfil,
    },
  }).pipe(delay(LATENCIA_MS));
}

function obterDetalheChamado(idOuCodigo: string): Observable<HttpResponse<unknown>> {
  const resumo = chamados.find((c) => c.id === idOuCodigo || c.codigo === idOuCodigo) ?? chamados[0];

  const detalhe: ChamadoDetalhe = {
    id: resumo.id,
    codigo: resumo.codigo,
    titulo: resumo.titulo,
    descricao:
      'Impressora de etiquetas da coleta parou de imprimir. Há pacientes aguardando na recepção para o cadastro das amostras. Já tentamos reiniciar o equipamento, sem sucesso.',
    unidade: resumo.unidade,
    solicitante: {
      nome: 'Mariana Alves',
      contato: '+5581988771234',
      telefoneMascarado: '+55 81 9****-1234',
    },
    canal_origem: resumo.canal_origem,
    status: resumo.status,
    prioridade: resumo.prioridade,
    categoria: 'Equipamento de apoio',
    sistema: 'Impressora de etiquetas',
    responsavel: resumo.responsavel,
    aberto_em: '2026-10-07T10:02:00Z',
    primeira_resposta_em: '2026-10-07T10:04:00Z',
    evidencias: [
      { id: 'ev-1', nome: 'erro-impressora.png', enviadoAs: '10:03' },
      { id: 'ev-2', nome: 'etiquetas-travadas.jpg', enviadoAs: '10:04' },
    ],
    historico: [
      {
        id: 'h-1',
        tipo: 'CHAMADO_ABERTO',
        titulo: `Chamado aberto via ${resumo.canal_origem === 'LIGACAO' ? 'Ligação' : resumo.canal_origem}`,
        descricao: 'Registrado por Ana Souza',
        autor: 'Ana Souza',
        hora: '10:02',
        dataCompleta: '07/10/2026 10:02',
        estiloDot: 'bg-[#94a3b8]',
      },
      {
        id: 'h-2',
        tipo: 'TRIAGEM_INICIADA',
        titulo: 'Triagem iniciada',
        autor: 'Ana Souza',
        hora: '10:05',
        dataCompleta: '07/10/2026 10:05',
        estiloDot: 'bg-[#14b8a6]',
      },
      {
        id: 'h-3',
        tipo: 'PRIORIDADE_DEFINIDA',
        titulo: 'Prioridade definida: Alta (P1)',
        descricao: 'Sugestão do sistema (soma 8), confirmada por Ana Souza',
        autor: 'Ana Souza',
        hora: '10:05',
        dataCompleta: '07/10/2026 10:05',
        estiloDot: 'bg-[#dc2626]',
      },
      {
        id: 'h-4',
        tipo: 'CHAMADO_ATRIBUIDO',
        titulo: 'Atribuído a Ana Souza',
        autor: 'Ana Souza',
        hora: '10:06',
        dataCompleta: '07/10/2026 10:06',
        estiloDot: 'bg-[#2d5bff]',
      },
      {
        id: 'h-5',
        tipo: 'STATUS_ALTERADO',
        titulo: 'Status alterado para Em Atendimento',
        autor: 'Ana Souza',
        hora: '10:07',
        dataCompleta: '07/10/2026 10:07',
        estiloDot: 'bg-[#0ea5e9]',
      },
      {
        id: 'h-6',
        tipo: 'REGISTRO_DE_CONTATO_RECEBIDO',
        titulo: 'Contato registrado',
        descricao: 'Solicitante informou que há pacientes aguardando',
        autor: 'Mariana Alves',
        hora: '10:21',
        dataCompleta: '07/10/2026 10:21',
        estiloDot: 'bg-[#94a3b8]',
      },
    ],
    slaItens: [
      { etapa: 'Triagem', meta: '15 min', situacaoTexto: 'Cumprida em 3 min', progresso: 100, estiloBarra: 'bg-[#22c55e]' },
      { etapa: 'Primeira resposta', meta: '15 min', situacaoTexto: 'Cumprida em 2 min', progresso: 100, estiloBarra: 'bg-[#22c55e]' },
      { etapa: 'Resolução', meta: '2 h', situacaoTexto: 'Em risco - restam 22 min', progresso: 82, estiloBarra: 'bg-[#f59e0b]' },
    ],
    pausasMinutos: 0,
    tempoTerceirosHoras: 0,
    relatosVinculados: [
      {
        id: 'chm-118',
        codigo: 'LUM-2026-000118',
        status: 'EM_ATENDIMENTO',
        descricao: 'Impressora de etiquetas sem imprimir',
        vinculadoEm: 'às 10:21',
        vinculadoPor: 'Ana Souza',
      },
    ],
    origem_dado: 'SIMULADO',
  };

  return sucesso(HttpStatusCode.Ok, 'Detalhes do chamado recuperados', detalhe).pipe(delay(LATENCIA_MS));
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
