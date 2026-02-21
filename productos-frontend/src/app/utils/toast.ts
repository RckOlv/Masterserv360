import Swal from 'sweetalert2';

export function mostrarToast(mensaje: string, tipo: 'success' | 'danger' | 'warning' | 'info' = 'info') {
  
  const iconType = tipo === 'danger' ? 'error' : tipo;

  Swal.fire({
    toast: true,
    position: 'top-end',
    icon: iconType,
    title: mensaje,
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    background: '#1e1e1e',
    color: '#ffffff',
    showCloseButton: true,
    didOpen: (toast) => {
      toast.addEventListener('mouseenter', Swal.stopTimer);
      toast.addEventListener('mouseleave', Swal.resumeTimer);
      
      // 🔥 EL GOLPE DE GRACIA 🔥
      // Agarramos el contenedor de la alerta y le inyectamos el z-index máximo directo en el HTML
      const container = toast.parentElement;
      if (container) {
        container.style.setProperty('z-index', '2147483647', 'important');
        container.style.setProperty('pointer-events', 'auto', 'important');
      }
    }
  });
}