import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReglasPuntos } from './reglas-puntos';

describe('ReglasPuntos', () => {
  let component: ReglasPuntos;
  let fixture: ComponentFixture<ReglasPuntos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReglasPuntos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReglasPuntos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
