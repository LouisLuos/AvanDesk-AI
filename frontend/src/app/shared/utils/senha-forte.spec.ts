import { FormControl, FormGroup } from '@angular/forms';
import { REGRAS_SENHA, senhaForte, senhaForteValidator, senhasIguaisValidator } from './senha-forte';

describe('senha forte', () => {
  it('deve aceitar a senha com todos os requisitos, como no backend', () => {
    expect(senhaForte('Coleta#2026ok')).toBe(true);
  });

  it.each([
    ['Cur#2026', 'tamanho'],
    ['coleta#2026ok', 'maiuscula'],
    ['COLETA#2026OK', 'minuscula'],
    ['Coleta#Lumen!', 'numero'],
    ['Coleta2026ok', 'simbolo'],
  ])('deve apontar a regra que falta em %s', (senha, regra) => {
    const pendentes = REGRAS_SENHA.filter((r) => !r.atende(senha)).map((r) => r.id);
    expect(pendentes).toEqual([regra]);
  });

  it('deve deixar a senha vazia para o Validators.required', () => {
    expect(senhaForteValidator(new FormControl(''))).toBeNull();
    expect(senhaForteValidator(new FormControl('fraca'))).toEqual({ senhaFraca: true });
  });

  it('deve apontar senhas diferentes no grupo', () => {
    const grupo = new FormGroup(
      { senha: new FormControl('Coleta#2026ok'), confirmacaoSenha: new FormControl('Coleta#2026OK') },
      { validators: senhasIguaisValidator },
    );
    expect(grupo.hasError('senhasDiferentes')).toBe(true);
  });
});
