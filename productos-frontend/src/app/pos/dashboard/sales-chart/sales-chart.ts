import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts'; 
import { Chart, ChartConfiguration, ChartData, ChartOptions, ChartType, LineController, CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler } from 'chart.js';
import { VentasPorDiaDTO } from '../../../models/venta.model';

Chart.register(
  LineController, 
  CategoryScale, 
  LinearScale, 
  PointElement, 
  LineElement,
  Tooltip,
  Legend,
  Filler
);

@Component({
  selector: 'app-sales-chart',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './sales-chart.html',
})
export default class SalesChartComponent implements OnChanges {
  
  @Input() chartData: VentasPorDiaDTO[] = [];
  public chart?: Chart;

  public lineChartData: ChartData<'line'> = {
    datasets: [{ 
      data: [], 
      label: 'Ventas (ARS)', 
      tension: 0.3,
      backgroundColor: 'rgba(228, 30, 38, 0.2)', 
      borderColor: '#E41E26',
      fill: 'origin',
    }],
    labels: []
  };
  
  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: { 
        grid: { color: 'rgba(255,255,255,0.1)' },
        title: { 
            display: true, 
            text: 'Fecha', 
            color: '#E0E0E0' 
        },
        ticks: { color: '#E0E0E0' }
      },
      y: { 
        grid: { color: 'rgba(255,255,255,0.1)' },
        title: { 
            display: true, 
            text: 'Total (ARS)', 
            color: '#E0E0E0' 
        },
        ticks: { color: '#E0E0E0' }
      }
    },
    plugins: { 
      legend: { display: false }
    },
    elements: {
      point: {
        radius: 4,
        backgroundColor: '#E41E26',
        borderColor: '#fff',
        hoverRadius: 6
      }
    },
    animation: {
        onComplete: (animation) => {
            this.chart = animation.chart;
        }
    }
  };
  
  public lineChartType: 'line' = 'line'; 

  constructor() {}

  ngOnChanges() {
    if (this.chartData && this.chartData.length > 0) {
      this.lineChartData.labels = this.chartData.map(d => 
        new Date(d.fecha + 'T00:00:00-03:00')
          .toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit' })
      );
      this.lineChartData.datasets[0].data = this.chartData.map(d => d.total);
      
      if (this.chart) {
          this.chart.update();
      }
    }
  }

  public getChartImageForPdf(): string | null {
      if (!this.chart) return null;

      const xScale = this.chart.options.scales!['x'] as any;
      const yScale = this.chart.options.scales!['y'] as any;

      // 1. MODO IMPRESIÓN (NEGRO)
      // Cambiamos colores para que se vea bien en papel blanco
      xScale.ticks.color = '#000000';
      xScale.title.color = '#000000';
      xScale.grid.color = 'rgba(0,0,0,0.2)';
      
      yScale.ticks.color = '#000000';
      yScale.title.color = '#000000';
      yScale.grid.color = 'rgba(0,0,0,0.2)';
      
      this.chart.update('none'); // Aplicar cambio visualmente (instantáneo)

      // 2. FOTO
      const image = this.chart.toBase64Image();

      // 3. RESTAURAR MODO OSCURO (BLANCO)
      // Forzamos los valores originales hardcodeados para asegurar que vuelva
      xScale.ticks.color = '#E0E0E0';
      xScale.title.color = '#E0E0E0';
      xScale.grid.color = 'rgba(255,255,255,0.1)';
      
      yScale.ticks.color = '#E0E0E0';
      yScale.title.color = '#E0E0E0';
      yScale.grid.color = 'rgba(255,255,255,0.1)';
      
      this.chart.update('none'); // Restaurar vista inmediatamente

      return image;
  }
}