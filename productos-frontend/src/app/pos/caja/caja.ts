import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CajaService, Caja } from '../../service/caja.service';
import { AuthService } from '../../service/auth.service';
import { mostrarToast } from '../../utils/toast';

declare var bootstrap: any;

@Component({
  selector: 'app-caja',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './caja.html',
  styleUrls: ['./caja.css'] // Si no usas CSS propio, puedes borrar esta línea
})
export default class CajaComponent implements OnInit {
  private cajaService = inject(CajaService);
  private authService = inject(AuthService); // Asumo que tienes un servicio para sacar el ID del usuario

  cajaActual: Caja | null = null;
  isLoading = true;
  usuarioId: number = 0;

  // Formularios
  montoInicialInput: number = 0;
  montoDeclaradoInput: number = 0;
  isProcessing = false;

  ngOnInit() {
    this.obtenerUsuarioYCaja();
  }

  obtenerUsuarioYCaja() {
    // 1. Buscamos el token en el LocalStorage
    const token = localStorage.getItem('jwt_token');

    if (token) {
      try {
        // 2. Un JWT tiene 3 partes separadas por puntos. Agarramos la del medio (payload)
        const payload = token.split('.')[1];
        
        // 3. Desencriptamos de Base64 a texto y lo convertimos en un Objeto JSON
        const datosUsuario = JSON.parse(atob(payload));
        
        // 4. ¡Atrapamos el ID!
        this.usuarioId = datosUsuario.id;

        if (this.usuarioId) {
          this.cargarCaja(); // Ahora que tenemos el ID, vamos a ver si tiene la caja abierta
        } else {
          mostrarToast('Error: El token no contiene un ID de usuario.', 'danger');
          this.isLoading = false;
        }
      } catch (e) {
        console.error('Error al decodificar el token:', e);
        mostrarToast('Error al leer la sesión del usuario.', 'danger');
        this.isLoading = false;
      }
    } else {
      mostrarToast('Error: No hay sesión activa.', 'danger');
      this.isLoading = false;
    }
  }

  cargarCaja() {
    this.isLoading = true;
    this.cajaService.verificarCajaAbierta(this.usuarioId).subscribe({
      next: (caja) => {
        this.cajaActual = caja;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  abrirCaja() {
    if (this.montoInicialInput < 0) {
      mostrarToast('El monto inicial no puede ser negativo.', 'warning');
      return;
    }

    this.isProcessing = true;
    this.cajaService.abrirCaja(this.usuarioId, this.montoInicialInput).subscribe({
      next: (cajaAbierta) => {
        this.cajaActual = cajaAbierta;
        this.montoInicialInput = 0;
        this.isProcessing = false;
        mostrarToast('¡Caja abierta con éxito! Ya puedes vender.', 'success');
      },
      error: (err) => {
        this.isProcessing = false;
        mostrarToast(err.error?.message || 'Error al abrir la caja.', 'danger');
      }
    });
  }

  abrirModalCierre() {
    this.montoDeclaradoInput = 0;
    const modalEl = document.getElementById('modalCierreCaja');
    if (modalEl) {
      new bootstrap.Modal(modalEl).show();
    }
  }

  cerrarCaja() {
    if (!this.cajaActual) return;
    if (this.montoDeclaradoInput < 0) {
      mostrarToast('El monto declarado no puede ser negativo.', 'warning');
      return;
    }

    this.isProcessing = true;
    this.cajaService.cerrarCaja(this.cajaActual.id, this.montoDeclaradoInput).subscribe({
      next: (cajaCerrada) => {
        this.cajaActual = null; // La caja se cerró, volvemos a la pantalla de apertura
        this.isProcessing = false;
        
        // Ocultar Modal
        const modalEl = document.getElementById('modalCierreCaja');
        if (modalEl) { bootstrap.Modal.getInstance(modalEl)?.hide(); }

        // Mensaje de diferencia
        const dif = cajaCerrada.diferencia || 0;
        if (dif === 0) {
            mostrarToast('Caja cerrada perfectamente. Arqueo cuadrado.', 'success');
        } else if (dif > 0) {
            mostrarToast(`Caja cerrada. SOBRARON $${dif}`, 'warning');
        } else {
            mostrarToast(`Caja cerrada. FALTARON $${Math.abs(dif)}`, 'danger');
        }
      },
      error: (err) => {
        this.isProcessing = false;
        mostrarToast('Error al cerrar la caja.', 'danger');
      }
    });
  }

  // Cálculos dinámicos para la vista
  get totalEsperadoEfectivo(): number {
    if (!this.cajaActual) return 0;
    return this.cajaActual.montoInicial + this.cajaActual.ventasEfectivo;
  }

  get totalVentasDelDia(): number {
    if (!this.cajaActual) return 0;
    return this.cajaActual.ventasEfectivo + this.cajaActual.ventasTarjeta + this.cajaActual.ventasTransferencia;
  }
}