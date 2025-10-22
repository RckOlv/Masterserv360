export function mostrarToast(mensaje: string, tipo: 'success' | 'danger' | 'warning' = 'success') {
  const toastEl = document.getElementById('appToast');
  const toastMsg = document.getElementById('toastMessage');
  if (!toastEl || !toastMsg) return;

  // Cambia color según tipo
  toastEl.classList.remove('bg-success', 'bg-danger', 'bg-warning');
  toastEl.classList.add(
    tipo === 'danger' ? 'bg-danger' : tipo === 'warning' ? 'bg-warning' : 'bg-success'
  );

  toastMsg.textContent = mensaje;

  const toast = new (window as any).bootstrap.Toast(toastEl);
  toast.show();
}
