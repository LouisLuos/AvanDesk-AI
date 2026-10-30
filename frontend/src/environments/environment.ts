export const environment = {
  production: true,
  /**
   * Em producao na Vercel, as chamadas para '/api/v1' sao automaticamente
   * redirecionadas pelo vercel.json para o backend no Render, eliminando problemas de CORS.
   */
  apiUrl: '/api/v1',
  useMocks: false,
};
