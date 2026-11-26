import { FormControl } from '@angular/forms';
import {
  emailValido,
  emailValidator,
  normalizarEmail,
  normalizarTelefone,
  telefoneValido,
  telefoneValidator,
} from './contato';

describe('contato', () => {
  it.each(['recepcao@lumen.com.br', ' Recepcao.BVG@Lumen.Local ', 'ana+coleta@gmail.com'])(
    'deve aceitar o e-mail %s',
    (email) => expect(emailValido(email)).toBe(true),
  );

  it.each(['', 'recepcao@lumen', 'recepcao.lumen.local', 'recep cao@lumen.com', '@lumen.com.br'])(
    'deve recusar o e-mail "%s"',
    (email) => expect(emailValido(email)).toBe(false),
  );

  it('deve deixar o e-mail vazio para o Validators.required', () => {
    expect(emailValidator(new FormControl(''))).toBeNull();
    expect(emailValidator(new FormControl('recepcao@lumen'))).toEqual({ email: true });
  });

  it('deve aceitar telefone vazio e recusar telefone sem DDD', () => {
    expect(telefoneValidator(new FormControl(''))).toBeNull();
    expect(telefoneValidator(new FormControl('9887-1234'))).toEqual({ telefone: true });
    expect(telefoneValido('(81) 98877-1234')).toBe(true);
  });

  it('deve normalizar e-mail em minúsculas e telefone em E.164', () => {
    expect(normalizarEmail('  Recepcao.BVG@Lumen.Local ')).toBe('recepcao.bvg@lumen.local');
    expect(normalizarTelefone('(81) 98877-1234')).toBe('+5581988771234');
    expect(normalizarTelefone('+55 81 98877-1234')).toBe('+5581988771234');
  });
});
