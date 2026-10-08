import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const REGEX_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Aceita telefone (10 a 13 dígitos, com ou sem DDI) ou e-mail. */
export const contatoValidator: ValidatorFn = (
  control: AbstractControl<string | null>,
): ValidationErrors | null => {
  const valor = control.value?.trim();
  if (!valor) return null;
  if (valor.includes('@')) {
    return REGEX_EMAIL.test(valor) ? null : { contato: true };
  }
  const digitos = valor.replace(/\D/g, '');
  return digitos.length >= 10 && digitos.length <= 13 ? null : { contato: true };
};

/**
 * Normaliza o contato para o formato esperado pelo contrato:
 * telefone em E.164 (+55 adicionado quando ausente) ou e-mail em minúsculas.
 */
export function normalizarContato(valor: string): string {
  const texto = valor.trim();
  if (texto.includes('@')) return texto.toLowerCase();
  const digitos = texto.replace(/\D/g, '');
  return digitos.length <= 11 ? `+55${digitos}` : `+${digitos}`;
}
