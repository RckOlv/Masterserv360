import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Permiso } from './permiso';

describe('Permiso', () => {
  let component: Permiso;
  let fixture: ComponentFixture<Permiso>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Permiso]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Permiso);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
