import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ContaSolicitanteService, UnidadeCadastro } from '../../../core/services/conta-solicitante.service';
import { ContaLayout } from '../../../layouts/conta-layout/conta-layout';
import { extrairMensagensErro } from '../../../shared/utils/api-error';
import { emailValidator, normalizarEmail } from '../../../shared/utils/contato';
import { REGRAS_SENHA, senhaForteValidator, senhasIguaisValidator } from '../../../shared/utils/senha-forte';

type CampoCadastro = 'nome' | 'email' | 'unidadeId' | 'senha' | 'confirmacaoSenha';

@Component({
  selector: 'app-cadastro',
  imports: [ReactiveFormsModule, RouterLink, ContaLayout],
  templateUrl: './cadastro.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cadastro implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly contaService = inject(ContaSolicitanteService);

  protected readonly unidades = signal<UnidadeCadastro[]>([]);
  protected readonly carregandoUnidades = signal(true);
  protected readonly erroUnidades = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erros = signal<string[]>([]);
  protected readonly emailJaCadastrado = signal(false);
  protected readonly mostrarSenha = signal(false);

  protected readonly form = this.fb.nonNullable.group(
    {
      nome: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.maxLength(254), emailValidator]],
      unidadeId: ['', Validators.required],
      senha: ['', [Validators.required, senhaForteValidator]],
      confirmacaoSenha: ['', Validators.required],
    },
    { validators: senhasIguaisValidator },
  );

  private readonly senha = toSignal(this.form.controls.senha.valueChanges, { initialValue: '' });

  protected readonly regras = computed(() =>
    REGRAS_SENHA.map((regra) => ({ ...regra, ok: regra.atende(this.senha() ?? '') })),
  );

  ngOnInit(): void {
    this.carregarUnidades();
  }

  protected carregarUnidades(): void {
    this.carregandoUnidades.set(true);
    this.erroUnidades.set(false);
    this.contaService
      .unidades()
      .pipe(finalize(() => this.carregandoUnidades.set(false)))
      .subscribe({
        next: (unidades) => this.unidades.set(unidades),
        error: () => this.erroUnidades.set(true),
      });
  }

  protected alternarVisibilidadeSenha(): void {
    this.mostrarSenha.update((visivel) => !visivel);
  }

  protected invalido(campo: CampoCadastro): boolean {
    const controle = this.form.controls[campo];
    return controle.invalid && (controle.touched || controle.dirty);
  }

  protected senhasDiferentes(): boolean {
    const confirmacao = this.form.controls.confirmacaoSenha;
    return this.form.hasError('senhasDiferentes') && (confirmacao.touched || confirmacao.dirty);
  }

  protected criarConta(): void {
    this.erros.set([]);
    this.emailJaCadastrado.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const email = normalizarEmail(valor.email);
    this.enviando.set(true);
    this.contaService
      .registrar({ nome: valor.nome.trim(), email, unidade_id: valor.unidadeId, senha: valor.senha })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (conta) => {
          this.contaService.guardarEmailPendente(conta.email);
          this.router.navigate(['/verifique-seu-email']);
        },
        error: (erro) => {
          this.emailJaCadastrado.set(erro?.status === 409);
          this.erros.set(extrairMensagensErro(erro));
        },
      });
  }
}
