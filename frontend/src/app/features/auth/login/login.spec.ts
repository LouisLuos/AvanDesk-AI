import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { Login } from './login';

describe('Login Component', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authService: AuthService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar com o formulário inválido', () => {
    expect(component['form'].valid).toBe(false);
  });

  it('deve alternar a visibilidade da senha ao clicar no botão', () => {
    expect(component['mostrarSenha']()).toBe(false);
    component['alternarVisibilidadeSenha']();
    expect(component['mostrarSenha']()).toBe(true);
    component['alternarVisibilidadeSenha']();
    expect(component['mostrarSenha']()).toBe(false);
  });

  it('deve marcar todos os campos como tocados ao submeter formulário inválido', () => {
    component['entrar']();
    expect(component['form'].controls.usuario.touched).toBe(true);
    expect(component['form'].controls.senha.touched).toBe(true);
  });

  it('deve realizar login com sucesso e navegar para /chamados', () => {
    const navegarSpy = vi.spyOn(router, 'navigate');
    vi.spyOn(authService, 'login').mockReturnValue(
      of({
        access_token: 'fake-token',
        usuario: { id: 'u-1', nome: 'Ana Souza', email: 'ana@lumen.com', perfil: 'Analista' },
      }),
    );

    component['form'].setValue({
      usuario: 'ana.souza',
      senha: '123',
      manterConectado: true,
    });

    component['entrar']();

    expect(authService.login).toHaveBeenCalledWith({
      usuario: 'ana.souza',
      senha: '123',
      manterConectado: true,
    });
    expect(navegarSpy).toHaveBeenCalledWith(['/chamados']);
  });

  it('deve exibir mensagem de erro quando o login falhar', () => {
    vi.spyOn(authService, 'login').mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { errors: ['Credenciais inválidas'] },
          }),
      ),
    );

    component['form'].setValue({
      usuario: 'invalido',
      senha: 'errada',
      manterConectado: false,
    });

    component['entrar']();
    expect(component['erros']()).toContain('Credenciais inválidas');
  });
});
