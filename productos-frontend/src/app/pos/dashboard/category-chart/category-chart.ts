import { Component, Input, OnChanges, SimpleChanges, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-category-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-chart.html',
  styles: [`
    .chart-container {
      position: relative;
      height: 250px; /* Altura controlada */
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  `]
})
export class CategoryChartComponent implements AfterViewInit, OnChanges {
  
  // Recibe datos: [{ categoria: 'Frenos', total: 50000 }, ...]
  @Input() data: any[] = []; 
  @ViewChild('categoryCanvas') canvasRef!: ElementRef;
  
  chart: any;

  ngAfterViewInit() {
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges) {
    // Si cambian los datos y el gráfico ya existe, lo actualizamos
    if (changes['data'] && this.chart) {
      this.updateChart();
    }
  }

  createChart() {
    const ctx = this.canvasRef.nativeElement.getContext('2d');
    
    this.chart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: [],
        datasets: [{
          data: [],
          backgroundColor: [
            '#E41E26', // Rojo Masterserv
            '#FFC107', // Amarillo
            '#0dcaf0', // Celeste
            '#198754', // Verde
            '#6610f2', // Violeta
            '#d63384', // Rosa
            '#fd7e14', // Naranja
            '#6c757d'  // Gris
          ],
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%', // Hace el agujero del medio más grande (estilo moderno)
        plugins: {
          legend: {
            position: 'right',
            labels: {
              color: '#e0e0e0', // Texto claro para dark mode
              font: { size: 11 },
              boxWidth: 12
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                let label = context.label || '';
                if (label) {
                    label += ': ';
                }
                if (context.parsed !== null) {
                    label += new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(context.parsed);
                }
                return label;
              }
            }
          }
        }
      }
    });
    
    this.updateChart();
  }

  updateChart() {
    if (!this.data || this.data.length === 0) {
        // Si no hay datos, podríamos limpiar el gráfico
        this.chart.data.labels = [];
        this.chart.data.datasets[0].data = [];
    } else {
        this.chart.data.labels = this.data.map(d => d.categoria);
        this.chart.data.datasets[0].data = this.data.map(d => d.total);
    }
    this.chart.update();
  }
}