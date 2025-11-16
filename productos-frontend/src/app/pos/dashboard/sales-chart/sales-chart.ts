import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts'; 
import { Chart, ChartConfiguration, ChartOptions, ChartType, LineController, CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler } from 'chart.js'; // Mentor: Imports clave
import { VentasPorDiaDTO } from '../../../models/venta.model';

// --- Mentor: INICIO DE LA CORRECCIÓN ---
// (Registramos los componentes que el gráfico de línea necesita)
Chart.register(
  LineController, 
  CategoryScale, 
  LinearScale, 
  PointElement, 
  LineElement,
  Tooltip,
  Legend,
  Filler // ¡EL PLUGIN DE RELLENO!
);
// --- Mentor: FIN DE LA CORRECCIÓN ---

@Component({
  selector: 'app-sales-chart',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './sales-chart.html',
})
export default class SalesChartComponent implements OnChanges {
  
  @Input() chartData: VentasPorDiaDTO[] = [];

  public lineChartData: ChartConfiguration['data'] = {
    datasets: [{ 
      data: [], 
      label: 'Ventas (ARS)', 
      tension: 0.3,
      backgroundColor: 'rgba(228, 30, 38, 0.2)', 
      borderColor: '#E41E26',
      fill: 'origin', // Esta línea ahora funcionará
    }],
    labels: []
  };
  
  public lineChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      // --- Mentor: INICIO DE LA CORRECCIÓN (Títulos de Ejes) ---
      x: { 
        grid: { color: 'rgba(255,255,255,0.1)' },
        title: { // Título Eje X
          display: true,
          text: 'Fecha',
          color: '#E0E0E0' 
        }
      },
      y: { 
        grid: { color: 'rgba(255,255,255,0.1)' },
        title: { // Título Eje Y
          display: true,
          text: 'Total (ARS)',
          color: '#E0E0E0'
        }
      }
      // --- Mentor: FIN DE LA CORRECCIÓN ---
    },
    plugins: { 
      legend: { display: false }
    },
    elements: {
      point: {
        radius: 4,
        backgroundColor: '#E41E26',
        borderColor: '#fff',
        hoverRadius: 6,
        hoverBackgroundColor: '#fff',
        hoverBorderColor: '#E41E26'
      }
    }
  };
  
  public lineChartType: ChartType = 'line';

  // (El constructor ahora está vacío, el registro se hace arriba)
  constructor() {}

  ngOnChanges() {
    if (this.chartData && this.chartData.length > 0) {
      this.lineChartData.labels = this.chartData.map(d => 
        new Date(d.fecha + 'T00:00:00-03:00') // Aseguramos zona horaria local
          .toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit' })
      );
      this.lineChartData.datasets[0].data = this.chartData.map(d => d.total);
    }
  }
}