export const PERFIS_ADMINISTRACAO: readonly string[] = ['LIDERANCA_TI'];

export const PERFIL_SOLICITANTE = 'SOLICITANTE';

export const PERFIS_EQUIPE: readonly string[] = ['ANALISTA_SUPORTE', 'LIDERANCA_TI', 'ADMIN'];

export function paginaInicialDoPerfil(perfil: string | null | undefined): string {
  return perfil === PERFIL_SOLICITANTE ? '/solicitante' : '/chamados';
}
