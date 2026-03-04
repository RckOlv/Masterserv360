import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CajaService, Caja, MovimientoCajaDTO } from '../../service/caja.service';
import { AuthService } from '../../service/auth.service';
import { mostrarToast } from '../../utils/toast';
import Swal from 'sweetalert2'; // ✅ IMPORTAMOS SWEETALERT

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
  movimientos: MovimientoCajaDTO[] = [];
  isLoadingMovimientos = false;
  
  // Formularios
  montoInicialInput: number | null = null;
  montoDeclaradoInput: number | null = null;
  observacionCierreInput: string = ''; 
  
  // Formulario de Movimiento (Ingreso/Egreso)
  tipoMovimientoInput: 'INGRESO' | 'EGRESO' = 'EGRESO';
  montoMovimientoInput: number | null = null;
  motivoMovimientoInput: string = '';
  
  isProcessing = false;
  isProcessingMov = false;

  ngOnInit() {
    this.obtenerUsuarioYCaja();
  }

  // ✅ BLOQUEO DE TECLAS: No deja presionar e, E, +, ni - (ni el normal ni el del teclado numérico)
  preventInvalidChars(event: KeyboardEvent) {
    if (['e', 'E', '-', '+', 'Subtract', 'Add'].includes(event.key) || ['e', 'E', '-', '+', 'Subtract', 'Add'].includes(event.code)) {
      event.preventDefault();
    }
  }

  // ✅ DEFENSA SECUNDARIA: Si el usuario logra pegar un número negativo, lo convierte en positivo al instante
  asegurarPositivo(campo: 'montoInicialInput' | 'montoMovimientoInput' | 'montoDeclaradoInput') {
    const valor = this[campo];
    if (valor !== null && valor < 0) {
      this[campo] = Math.abs(valor);
    }
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
        if (this.cajaActual) {
            this.cargarMovimientos(); 
        }
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  cargarMovimientos() {
    if (!this.cajaActual) return;
    this.isLoadingMovimientos = true;
    this.cajaService.obtenerMovimientosCaja(this.cajaActual.id).subscribe({
        next: (movs) => {
            this.movimientos = movs;
            this.isLoadingMovimientos = false;
        },
        error: (err) => {
            console.error('Error al cargar movimientos', err);
            this.isLoadingMovimientos = false;
        }
    });
  }

  abrirCaja() {
    if (this.montoInicialInput === null || this.montoInicialInput < 0) {
      mostrarToast('Ingresa un monto inicial válido.', 'warning');
      return;
    }

    this.isProcessing = true;
    this.cajaService.abrirCaja(this.usuarioId, this.montoInicialInput).subscribe({
      next: (cajaAbierta) => {
        this.cajaActual = cajaAbierta;
        this.montoInicialInput = null;
        this.isProcessing = false;
        mostrarToast('¡Caja abierta con éxito!', 'success');
      },
      error: (err) => {
        this.isProcessing = false;
        mostrarToast(err.error?.message || 'Error al abrir la caja.', 'danger');
      }
    });
  }

  abrirModalMovimiento(tipo: 'INGRESO' | 'EGRESO') {
    this.tipoMovimientoInput = tipo;
    this.montoMovimientoInput = null;
    this.motivoMovimientoInput = '';
    const modalEl = document.getElementById('modalMovimientoCaja');
    if (modalEl) {
      new bootstrap.Modal(modalEl).show();
    }
  }

  registrarMovimiento() {
    if (!this.montoMovimientoInput || this.montoMovimientoInput <= 0) {
      mostrarToast('Ingresa un monto válido.', 'warning');
      return;
    }
    if (!this.motivoMovimientoInput.trim()) {
      mostrarToast('Debes indicar el motivo del movimiento.', 'warning');
      return;
    }
    if (!this.cajaActual) return;

    if (this.tipoMovimientoInput === 'EGRESO') {
      if (this.montoMovimientoInput > this.totalEsperadoEfectivo) {
        // ✅ SWEET ALERT EN LUGAR DE TOAST PARA SALDO INSUFICIENTE
        Swal.fire({
          icon: 'error',
          title: 'Saldo Insuficiente',
          html: `Estás intentando retirar <b>$${this.montoMovimientoInput.toLocaleString('es-AR')}</b><br>pero solo tienes <b>$${this.totalEsperadoEfectivo.toLocaleString('es-AR')}</b> físico en cajón.`,
          confirmButtonColor: '#E41E26',
          confirmButtonText: 'Entendido',
          background: '#1e1e1e',
          color: '#ffffff'
        });
        return;
      }

      this.isProcessingMov = true;
      this.cajaService.registrarRetiro(this.cajaActual.id, this.montoMovimientoInput, this.motivoMovimientoInput).subscribe({
        next: (cajaActualizada) => this.completarMovimiento(cajaActualizada, 'Retiro registrado correctamente.'),
        error: () => { this.isProcessingMov = false; mostrarToast('Error al registrar el retiro.', 'danger'); }
      });
    } else {
      this.isProcessingMov = true;
      this.cajaService.registrarIngreso(this.cajaActual.id, this.montoMovimientoInput, this.motivoMovimientoInput).subscribe({
        next: (cajaActualizada) => this.completarMovimiento(cajaActualizada, 'Ingreso registrado correctamente.'),
        error: () => { this.isProcessingMov = false; mostrarToast('Error al registrar el ingreso.', 'danger'); }
      });
    }
  }

  private completarMovimiento(cajaActualizada: Caja, mensaje: string) {
    this.cajaActual = cajaActualizada;
    this.isProcessingMov = false;
    const modalEl = document.getElementById('modalMovimientoCaja');
    if (modalEl) { bootstrap.Modal.getInstance(modalEl)?.hide(); }
    mostrarToast(mensaje, 'success');
    this.cargarMovimientos();
  }

  abrirModalCierre() {
    this.montoDeclaradoInput = null;
    this.observacionCierreInput = ''; 
    const modalEl = document.getElementById('modalCierreCaja');
    if (modalEl) {
      new bootstrap.Modal(modalEl).show();
    }
  }

  cerrarCaja() {
    if (!this.cajaActual || this.montoDeclaradoInput === null) {
        mostrarToast('Ingresa el monto físico contado en el cajón.', 'warning');
        return;
    }

    const dif = this.montoDeclaradoInput - this.totalEsperadoEfectivo;
    if (dif !== 0 && !this.observacionCierreInput.trim()) {
        mostrarToast('Hay una diferencia en caja. Debes ingresar un motivo obligatoriamente.', 'warning');
        return;
    }

    this.isProcessing = true;
    this.cajaService.cerrarCaja(this.cajaActual.id, this.usuarioId, this.montoDeclaradoInput, this.observacionCierreInput).subscribe({
      next: (cajaCerrada) => {
        this.cajaActual = null;
        this.isProcessing = false;
        const modalEl = document.getElementById('modalCierreCaja');
        if (modalEl) { bootstrap.Modal.getInstance(modalEl)?.hide(); }

        const difCierre = cajaCerrada.diferencia || 0;
        if (difCierre === 0) mostrarToast('Caja cerrada correctamente.', 'success');
        else if (difCierre > 0) mostrarToast(`Cierre con SOBRANTE de $${difCierre}`, 'warning');
        else mostrarToast(`Cierre con FALTANTE de $${Math.abs(difCierre)}`, 'danger');
      },
      error: (err) => {
        this.isProcessing = false;
        mostrarToast('Error al cerrar la caja.', 'danger');
      }
    });
  }

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