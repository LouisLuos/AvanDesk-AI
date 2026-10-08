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
import { ChamadoRequest, ChamadoResponse } from '../../shared/models/chamado.model';
import { Unidade, UnidadeRequest } from '../../shared/models/unidade.model';

/**
 * API simulada em memória para desenvolver o frontend antes do backend ficar pronto.
 * Ativada por `environment.useMocks`. Responde no mesmo formato ApiResponse do contrato.
 * Dados fictícios — nenhum dado real de paciente ou colaborador.
 */

const LATENCIA_MS = 400;

const unidades: Unidade[] = [
  { id: 'a1f0c3e2-0001-4000-8000-000000000001', nome: 'Unidade Boa Viagem', identificacao: 'LUM-BVG', localizacao: 'Rua Boa Viagem, 123', atendimento_24h: false, ativo: true },
  { id: 'a1f0c3e2-0002-4000-8000-000000000002', nome: 'Unidade Derby (24h)', identificacao: 'LUM-DRB', localizacao: 'Av Agamenon Magalhaes, 400', atendimento_24h: true, ativo: true },
  { id: 'a1f0c3e2-0003-4000-8000-000000000003', nome: 'Unidade Casa Forte', identificacao: 'LUM-CSF', localizacao: 'Praca de Casa Forte, 50', atendimento_24h: false, ativo: true },
  { id: 'a1f0c3e2-0004-4000-8000-000000000004', nome: 'Unidade Olinda', identificacao: 'LUM-OLD', localizacao: 'Av Getulio Vargas, 200', atendimento_24h: false, ativo: true },
  { id: 'a1f0c3e2-0005-4000-8000-000000000005', nome: 'Unidade Caruaru', identificacao: 'LUM-CRU', localizacao: '', atendimento_24h: false, ativo: false },
];

let sequencialChamado = 123;

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
  };
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
