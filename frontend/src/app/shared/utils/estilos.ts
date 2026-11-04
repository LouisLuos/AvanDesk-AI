/** Classes Tailwind compartilhadas pelos formulários e tabelas hi-fi da administração. */

const CLASSE_CAMPO =
  'h-9 w-full rounded-lg border bg-white px-3 text-sm text-[#14161b] shadow-2xs transition placeholder:text-[#9aa2b1] focus:outline-none focus:ring-2';
const CAMPO_VALIDO = 'border-[#e2e5ec] focus:border-[#2d5bff] focus:ring-[#e8edff]';
const CAMPO_INVALIDO = 'border-[#e5484d] focus:border-[#e5484d] focus:ring-[#e5484d]/15';

/** Campo de texto do formulário, com a borda vermelha quando inválido. */
export function classeCampo(invalido: boolean, extra = ''): string {
  return `${CLASSE_CAMPO} ${invalido ? CAMPO_INVALIDO : CAMPO_VALIDO} ${extra}`;
}

/** Selos de situação nas tabelas (Ativa/Sim e Inativa/Não). */
export const ESTILO_SELO_POSITIVO = 'bg-[#22c55e]/10 text-[#16a34a]';
export const ESTILO_SELO_NEUTRO = 'bg-[#f1f3f8] text-[#6b7280]';
