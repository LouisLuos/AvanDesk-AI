import { PrioridadeChamado, RespostasPrioridade } from '../models/chamado.model';

export type EixoMatriz = keyof RespostasPrioridade;

export interface PerguntaMatriz {
  eixo: EixoMatriz;
  titulo: string;
  pergunta: string;
  opcoes: ReadonlyArray<{ nota: number; texto: string }>;
}

export const PERGUNTAS_MATRIZ_3D: ReadonlyArray<PerguntaMatriz> = [
  {
    eixo: 'severidade',
    titulo: 'Severidade',
    pergunta: 'Quão quebrado está?',
    opcoes: [
      { nota: 1, texto: 'Tenho uma dúvida ou lentidão, mas consigo trabalhar com alternativa.' },
      { nota: 2, texto: 'Uma função específica parou sem alternativa, mas o restante do sistema funciona.' },
      { nota: 3, texto: 'O sistema ou equipamento está totalmente fora do ar / inacessível.' },
    ],
  },
  {
    eixo: 'urgencia',
    titulo: 'Urgência',
    pergunta: 'Quão rápido precisa?',
    opcoes: [
      { nota: 1, texto: 'Pode aguardar; nenhum atendimento ou rotina imediata foi interrompido.' },
      { nota: 2, texto: 'Afeta atividades administrativas/operacionais do dia, mas não há paciente esperando agora.' },
      { nota: 3, texto: 'Há paciente aguardando presencialmente na recepção/sala de exame neste momento.' },
    ],
  },
  {
    eixo: 'impacto',
    titulo: 'Impacto',
    pergunta: 'Quantos são afetados?',
    opcoes: [
      { nota: 1, texto: 'Apenas o meu computador / meu usuário.' },
      { nota: 2, texto: 'Várias pessoas do meu setor ou recepção.' },
      { nota: 3, texto: 'A unidade inteira está parada (ou mais de uma unidade).' },
    ],
  },
];

/** Mesma conta do backend (CalculadoraPrioridadeMatriz3D): soma 3–5 Baixa, 6–7 Média, 8–9 Alta. */
export function calcularPrioridadeMatriz3D(
  severidade: number,
  urgencia: number,
  impacto: number,
): PrioridadeChamado {
  const soma = severidade + urgencia + impacto;
  if (soma >= 8) return 'ALTA';
  if (soma >= 6) return 'MEDIA';
  return 'BAIXA';
}

export function textoDaResposta(eixo: EixoMatriz, nota: number | null | undefined): string {
  const pergunta = PERGUNTAS_MATRIZ_3D.find((p) => p.eixo === eixo);
  return pergunta?.opcoes.find((o) => o.nota === nota)?.texto ?? '';
}

export const ROTULOS_PRIORIDADE: Record<PrioridadeChamado, string> = {
  ALTA: 'Alta (P1)',
  MEDIA: 'Média (P2)',
  BAIXA: 'Baixa (P3)',
};
