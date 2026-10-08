import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import { UnidadeService } from '../../../core/services/unidade.service';
import {
  CANAIS_ORIGEM,
  CanalOrigem,
  ChamadoRequest,
  ChamadoResponse,
} from '../../../shared/models/chamado.model';
import { Unidade } from '../../../shared/models/unidade.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';
import { contatoValidator, normalizarContato } from '../../../shared/utils/contato';

const LIMITE_DESCRICAO = 2000;

type CampoChamado = 'unidadeId' | 'canalOrigem' | 'contato' | 'nome' | 'descricao';

@Component({
  selector: 'app-novo-chamado',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './novo-chamado.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NovoChamado implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly unidadeService = inject(UnidadeService);
  private readonly chamadoService = inject(ChamadoService);
  private readonly toast = inject(ToastService);

  protected readonly canais = CANAIS_ORIGEM;
  protected readonly limiteDescricao = LIMITE_DESCRICAO;

  protected readonly unidades = signal<Unidade[]>([]);
  protected readonly carregandoUnidades = signal(true);
  protected readonly erroUnidades = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errosApi = signal<string[]>([]);
  protected readonly chamadoCriado = signal<ChamadoResponse | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    unidadeId: ['', Validators.required],
    canalOrigem: ['LIGACAO' as CanalOrigem, Validators.required],
    contato: ['', [Validators.required, contatoValidator]],
    nome: ['', Validators.maxLength(120)],
    descricao: [
      '',
      [Validators.required, Validators.minLength(10), Validators.maxLength(LIMITE_DESCRICAO)],
    ],
  });

  private readonly valores = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly tamanhoDescricao = computed(() => this.valores().descricao?.length ?? 0);
  protected readonly contatoEhEmail = computed(() => this.valores().canalOrigem === 'EMAIL');

  ngOnInit(): void {
    this.carregarUnidades();
  }

  protected carregarUnidades(): void {
    const controle = this.form.controls.unidadeId;
    controle.disable();
    this.carregandoUnidades.set(true);
    this.erroUnidades.set(false);
    this.unidadeService
      .listarAtivas()
      .pipe(finalize(() => this.carregandoUnidades.set(false)))
      .subscribe({
        next: (lista) => {
          this.unidades.set([...lista].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')));
          if (lista.length) controle.enable();
        },
        error: () => this.erroUnidades.set(true),
      });
  }

  protected selecionarCanal(canal: CanalOrigem): void {
    this.form.controls.canalOrigem.setValue(canal);
  }

  protected invalido(campo: CampoChamado): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  protected registrar(): void {
    this.errosApi.set([]);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const nome = valor.nome.trim();
    const payload: ChamadoRequest = {
      unidade_id: valor.unidadeId,
      canal_origem: valor.canalOrigem,
      solicitante: {
        ...(nome ? { nome } : {}),
        contato: normalizarContato(valor.contato),
      },
      descricao: valor.descricao.trim(),
    };

    this.enviando.set(true);
    this.chamadoService
      .registrar(payload)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (chamado) => {
          this.chamadoCriado.set(chamado);
          this.toast.sucesso(`Chamado ${chamado.codigo} registrado com sucesso.`);
        },
        error: (erro) => this.errosApi.set(extrairMensagensErro(erro)),
      });
  }

  protected cancelar(): void {
    this.form.reset();
    this.errosApi.set([]);
  }

  protected novoRegistro(): void {
    this.chamadoCriado.set(null);
    this.cancelar();
  }

  protected copiarCodigo(codigo: string): void {
    navigator.clipboard
      ?.writeText(codigo)
      .then(() => this.toast.info('Código copiado para a área de transferência.'))
      .catch(() => this.toast.erro('Não foi possível copiar o código.'));
  }
}
