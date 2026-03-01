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
      
      const container = toast.parentElement;
      if (container) {
        container.style.setProperty('z-index', '2147483647', 'important');
        container.style.setProperty('pointer-events', 'auto', 'important');
      }
    }
  });
}

// 2. FUNCIÓN PARA REEMPLAZAR LOS confirm() FEOS (EN EL CENTRO DE LA PANTALLA)
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
      const container = document.querySelector('.swal2-container') as HTMLElement;
      if (container) {
        container.style.setProperty('z-index', '2147483647', 'important');
      }
    }
  }).then((result) => {
    return result.isConfirmed; 
  });
}

// ✅ 3. NUEVA FUNCIÓN: PEDIR UN TEXTO OBLIGATORIO CON ESTILO
export function pedirMotivoAccion(titulo: string, mensajeHtml: string, placeholder: string = 'Escribe aquí...'): Promise<string | null> {
  return Swal.fire({
    title: titulo,
    html: `<div class="text-start text-white-50">${mensajeHtml}</div>`,
    input: 'textarea',
    inputPlaceholder: placeholder,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#E41E26', // Rojo Masterserv
    cancelButtonColor: '#9E9E9E',
    confirmButtonText: '<i class="bi bi-trash"></i> Confirmar Anulación',
    cancelButtonText: 'Cancelar',
    background: '#1e1e1e',
    color: '#ffffff',
    customClass: {
      popup: 'border border-secondary rounded-3 shadow-lg',
      input: 'bg-dark text-white border-secondary' // Estilo oscuro para el textarea
    },
    didOpen: () => {
      const container = document.querySelector('.swal2-container') as HTMLElement;
      if (container) {
        container.style.setProperty('z-index', '2147483647', 'important');
      }
    },
    inputValidator: (value) => {
      if (!value || value.trim() === '') {
        return '¡Este campo es obligatorio!';
      }
      return null;
    }
  }).then((result) => {
    if (result.isConfirmed) {
      return result.value as string; // Devuelve el texto escrito
    }
    return null; // Devuelve null si canceló
  });
}