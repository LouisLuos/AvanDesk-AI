import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { extrairMensagensErro } from '../../../shared/utils/api-error';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly carregando = signal(false);
  protected readonly erros = signal<string[]>([]);
  protected readonly mostrarSenha = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    usuario: ['', [Validators.required]],
    senha: ['', [Validators.required]],
    manterConectado: [false],
  });

  protected alternarVisibilidadeSenha(): void {
    this.mostrarSenha.update((v) => !v);
  }

  protected entrar(): void {
    this.erros.set([]);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { usuario, senha, manterConectado } = this.form.getRawValue();
    this.carregando.set(true);

    this.authService
      .login({ usuario: usuario.trim(), senha, manterConectado })
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (resposta) => {
          this.toast.sucesso(`Bem-vindo(a), ${resposta.usuario.nome}!`);
          this.router.navigate(['/chamados']);
        },
        error: (erro) => {
          const mensagens = extrairMensagensErro(erro);
          this.erros.set(mensagens.length ? mensagens : ['Usuário ou senha incorretos.']);
        },
      });
  }

  protected esqueceuSenha(): void {
    this.toast.info('Para redefinir sua senha, entre em contato com a Liderança de TI.');
  }

  protected invalido(campo: 'usuario' | 'senha'): boolean {
    const controle = this.form.controls[campo];
    return controle.invalid && (controle.touched || controle.dirty);
  }
}
