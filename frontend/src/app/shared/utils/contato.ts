import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const REGEX_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export function emailValido(valor: string | null | undefined): boolean {
  const texto = valor?.trim() ?? '';
  return texto.length <= 254 && REGEX_EMAIL.test(texto);
}

export function telefoneValido(valor: string | null | undefined): boolean {
  const digitos = (valor ?? '').replace(/\D/g, '');
  return digitos.length >= 10 && digitos.length <= 13;
}

export const emailValidator: ValidatorFn = (
  control: AbstractControl<string | null>,
): ValidationErrors | null => {
  const valor = control.value?.trim();
  if (!valor) return null;
  return emailValido(valor) ? null : { email: true };
};

export const telefoneValidator: ValidatorFn = (
  control: AbstractControl<string | null>,
): ValidationErrors | null => {
  const valor = control.value?.trim();
  if (!valor) return null;
  return telefoneValido(valor) ? null : { telefone: true };
};

export function normalizarEmail(valor: string): string {
  return valor.trim().toLowerCase();
}

export function normalizarTelefone(valor: string): string {
  const digitos = valor.replace(/\D/g, '');
  return digitos.length <= 11 ? `+55${digitos}` : `+${digitos}`;
}
