import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export interface RegraSenha {
  id: string;
  rotulo: string;
  atende: (senha: string) => boolean;
}

export const REGRAS_SENHA: readonly RegraSenha[] = [
  { id: 'tamanho', rotulo: 'Pelo menos 10 caracteres', atende: (s) => s.length >= 10 && s.length <= 72 },
  { id: 'maiuscula', rotulo: 'Uma letra maiúscula', atende: (s) => /\p{Lu}/u.test(s) },
  { id: 'minuscula', rotulo: 'Uma letra minúscula', atende: (s) => /\p{Ll}/u.test(s) },
  { id: 'numero', rotulo: 'Um número', atende: (s) => /\d/.test(s) },
  { id: 'simbolo', rotulo: 'Um símbolo (ex.: ! @ # $ %)', atende: (s) => /[^\p{L}\p{N}]/u.test(s) },
];

export function senhaForte(senha: string | null | undefined): boolean {
  const texto = senha ?? '';
  return REGRAS_SENHA.every((regra) => regra.atende(texto));
}

export const senhaForteValidator: ValidatorFn = (
  control: AbstractControl<string | null>,
): ValidationErrors | null => {
  const valor = control.value ?? '';
  if (!valor) return null;
  return senhaForte(valor) ? null : { senhaFraca: true };
};

export const senhasIguaisValidator: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const senha = grupo.get('senha')?.value;
  const confirmacao = grupo.get('confirmacaoSenha')?.value;
  if (!senha || !confirmacao) return null;
  return senha === confirmacao ? null : { senhasDiferentes: true };
};
