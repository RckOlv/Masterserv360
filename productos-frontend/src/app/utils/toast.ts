// Agregamos | 'info' en la lista de opciones válidas
export function mostrarToast(mensaje: string, tipo: 'success' | 'danger' | 'warning' | 'info' = 'success') {
  const toastEl = document.getElementById('appToast');
  const toastMsg = document.getElementById('toastMessage');
  if (!toastEl || !toastMsg) return;

  // Cambia color según tipo
  toastEl.classList.remove('bg-success', 'bg-danger', 'bg-warning', 'bg-info');
  
  toastEl.classList.add(
    tipo === 'success' ? 'bg-success' :
    tipo === 'danger' ? 'bg-danger' :
    tipo === 'warning' ? 'bg-warning' :
    tipo === 'info' ? 'bg-info' : 'bg-success'
  );

  toastMsg.textContent = mensaje;

  const toast = new (window as any).bootstrap.Toast(toastEl);
  toast.show();
}