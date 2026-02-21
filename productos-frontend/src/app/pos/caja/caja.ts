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
  styleUrls: ['./caja.css']
})
export default class CajaComponent implements OnInit {
  private cajaService = inject(CajaService);
  private authService = inject(AuthService);

  cajaActual: Caja | null = null;
  isLoading = true;
  usuarioId: number = 0;

  // Formularios
  montoInicialInput: number = 0;
  montoDeclaradoInput: number = 0;
  
  // Formulario de Retiro (NUEVO)
  montoRetiroInput: number = 0;
  motivoRetiroInput: string = '';
  
  isProcessing = false;
  isRetiring = false;

  ngOnInit() {
    this.obtenerUsuarioYCaja();
  }

  obtenerUsuarioYCaja() {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      try {
        const payload = token.split('.')[1];
        const datosUsuario = JSON.parse(atob(payload));
        this.usuarioId = datosUsuario.id;

        if (this.usuarioId) {
          this.cargarCaja();
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
        mostrarToast('¡Caja abierta con éxito!', 'success');
      },
      error: (err) => {
        this.isProcessing = false;
        mostrarToast(err.error?.message || 'Error al abrir la caja.', 'danger');
      }
    });
  }

  // Lógica de Retiro/Extracción (NUEVO)
  abrirModalRetiro() {
    this.montoRetiroInput = 0;
    this.motivoRetiroInput = '';
    const modalEl = document.getElementById('modalRetiroCaja');
    if (modalEl) {
      new bootstrap.Modal(modalEl).show();
    }
  }

  registrarRetiro() {
    if (this.montoRetiroInput <= 0) {
      mostrarToast('Ingresa un monto válido.', 'warning');
      return;
    }
    if (!this.motivoRetiroInput.trim()) {
      mostrarToast('Debes indicar el motivo del gasto.', 'warning');
      return;
    }
    if (!this.cajaActual) return;

    this.isRetiring = true;
    this.cajaService.registrarRetiro(this.cajaActual.id, this.montoRetiroInput, this.motivoRetiroInput).subscribe({
      next: (cajaActualizada) => {
        this.cajaActual = cajaActualizada;
        this.isRetiring = false;
        const modalEl = document.getElementById('modalRetiroCaja');
        if (modalEl) { bootstrap.Modal.getInstance(modalEl)?.hide(); }
        mostrarToast('Retiro registrado correctamente.', 'success');
      },
      error: (err) => {
        this.isRetiring = false;
        mostrarToast('Error al registrar el retiro.', 'danger');
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
    this.isProcessing = true;
    this.cajaService.cerrarCaja(this.cajaActual.id, this.montoDeclaradoInput).subscribe({
      next: (cajaCerrada) => {
        this.cajaActual = null;
        this.isProcessing = false;
        const modalEl = document.getElementById('modalCierreCaja');
        if (modalEl) { bootstrap.Modal.getInstance(modalEl)?.hide(); }

        const dif = cajaCerrada.diferencia || 0;
        if (dif === 0) mostrarToast('Caja cerrada correctamente.', 'success');
        else if (dif > 0) mostrarToast(`Cierre con SOBRANTE de $${dif}`, 'warning');
        else mostrarToast(`Cierre con FALTANTE de $${Math.abs(dif)}`, 'danger');
      },
      error: (err) => {
        this.isProcessing = false;
        mostrarToast('Error al cerrar la caja.', 'danger');
      }
    });
  }

  // Cálculos dinámicos (MODIFICADOS PARA RESTAR EXTRACCIONES)
  get totalEsperadoEfectivo(): number {
    if (!this.cajaActual) return 0;
    const extracciones = this.cajaActual.extracciones || 0;
    return this.cajaActual.montoInicial + this.cajaActual.ventasEfectivo - extracciones;
  }

  get totalVentasDelDia(): number {
    if (!this.cajaActual) return 0;
    return this.cajaActual.ventasEfectivo + this.cajaActual.ventasTarjeta + this.cajaActual.ventasTransferencia;
  }
}