import Swal from 'sweetalert2';

// 1. FUNCIÓN PARA NOTIFICACIONES CHICAS (ARRIBA A LA DERECHA)
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

// 2. NUEVA FUNCIÓN PARA REEMPLAZAR LOS confirm() FEOS (EN EL CENTRO DE LA PANTALLA)
export function confirmarAccion(titulo: string, texto: string): Promise<boolean> {
  return Swal.fire({
    title: titulo,
    text: texto,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#E41E26', // Rojo Masterserv
    cancelButtonColor: '#9E9E9E',  // Gris Metal
    confirmButtonText: '<i class="bi bi-check-lg"></i> Sí, confirmar',
    cancelButtonText: 'Cancelar',
    background: '#1e1e1e', // Fondo oscuro modo Dark
    color: '#ffffff',
    customClass: {
      popup: 'border border-secondary rounded-3 shadow-lg'
    },
    didOpen: () => {
      // Le metemos el z-index brutal por si salta arriba de una tabla o modal
      const container = document.querySelector('.swal2-container') as HTMLElement;
      if (container) {
        container.style.setProperty('z-index', '2147483647', 'important');
      }
    }
  }).then((result) => {
    return result.isConfirmed; // Devuelve true si el usuario tocó "Sí, confirmar"
  });
}