import { calcularPrioridadeMatriz3D, textoDaResposta } from './matriz-prioridade';

describe('Matriz 3D no front (HU22)', () => {
  const tabela: ReadonlyArray<[number, number, number, string]> = [
    [1, 1, 1, 'BAIXA'], [1, 1, 2, 'BAIXA'], [1, 1, 3, 'BAIXA'],
    [1, 2, 1, 'BAIXA'], [1, 2, 2, 'BAIXA'], [1, 2, 3, 'MEDIA'],
    [1, 3, 1, 'BAIXA'], [1, 3, 2, 'MEDIA'], [1, 3, 3, 'MEDIA'],
    [2, 1, 1, 'BAIXA'], [2, 1, 2, 'BAIXA'], [2, 1, 3, 'MEDIA'],
    [2, 2, 1, 'BAIXA'], [2, 2, 2, 'MEDIA'], [2, 2, 3, 'MEDIA'],
    [2, 3, 1, 'MEDIA'], [2, 3, 2, 'MEDIA'], [2, 3, 3, 'ALTA'],
    [3, 1, 1, 'BAIXA'], [3, 1, 2, 'MEDIA'], [3, 1, 3, 'MEDIA'],
    [3, 2, 1, 'MEDIA'], [3, 2, 2, 'MEDIA'], [3, 2, 3, 'ALTA'],
    [3, 3, 1, 'MEDIA'], [3, 3, 2, 'ALTA'], [3, 3, 3, 'ALTA'],
  ];

  it.each(tabela)('S%i U%i I%i = %s, igual ao backend', (s, u, i, esperada) => {
    expect(calcularPrioridadeMatriz3D(s, u, i)).toBe(esperada);
  });

  it('deve devolver o texto de negócio da resposta escolhida', () => {
    expect(textoDaResposta('impacto', 3)).toBe('A unidade inteira está parada (ou mais de uma unidade).');
    expect(textoDaResposta('severidade', null)).toBe('');
  });
});
