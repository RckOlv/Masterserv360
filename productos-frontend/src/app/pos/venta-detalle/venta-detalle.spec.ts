import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VentaDetalle } from './venta-detalle';

describe('VentaDetalle', () => {
  let component: VentaDetalle;
  let fixture: ComponentFixture<VentaDetalle>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VentaDetalle]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VentaDetalle);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
