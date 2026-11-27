import { ChamadoResumo, ResponsavelChamado } from './chamado.model';

export type TipoEventoTempoReal =
  | 'CHAMADO_CRIADO'
  | 'CHAMADO_ATRIBUIDO'
  | 'STATUS_ALTERADO'
  | 'CHAMADO_ATUALIZADO';

export interface EventoTempoReal {
  tipo: TipoEventoTempoReal;
  chamado: ChamadoResumo;
  autor: ResponsavelChamado | null;
  ocorrido_em: string;
}

export type EstadoTempoReal = 'desconectado' | 'conectando' | 'conectado';

export interface TicketTempoReal {
  ticket: string;
  expira_em: string;
}
