import Swal from 'sweetalert2';

export function mostrarToast(mensaje: string, tipo: 'success' | 'danger' | 'warning' | 'info') {
  
  // SweetAlert usa 'error' en lugar de 'danger', así que hacemos la conversión automática
  const iconType = tipo === 'danger' ? 'error' : tipo;

  Swal.fire({
    toast: true, // Esto hace que sea una alerta chiquita y no un cartel gigante en el medio
    position: 'top-end', // Arriba a la derecha
    icon: iconType,
    title: mensaje,
    showConfirmButton: false,
    timer: 3000, // Desaparece en 3 segundos
    timerProgressBar: true, // Le pone la barrita de tiempo abajo
    background: '#1e1e1e', // Fondo oscuro modo Dark!
    color: '#ffffff', // Letra blanca
    showCloseButton: true,
    didOpen: (toast) => {
      toast.addEventListener('mouseenter', Swal.stopTimer)
      toast.addEventListener('mouseleave', Swal.resumeTimer)
    }
  });
}