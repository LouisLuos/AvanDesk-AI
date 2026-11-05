/** Iniciais do nome para avatares: "Ana Souza" vira "AS". */
export function iniciais(nome: string): string {
  return nome
    .split(' ')
    .filter(Boolean)
    .map((parte) => parte[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}
