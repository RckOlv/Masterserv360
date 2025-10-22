import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoriasComponent } from './categorias';

describe('CategoriasComponent', () => {
  let component: CategoriasComponent;
  let fixture: ComponentFixture<CategoriasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoriasComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CategoriasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default categories', () => {
    expect(component.categorias.length).toBe(2);
    expect(component.categorias[0].nombreCategoria).toBe('Motor');
  });

  it('should toggle modal', () => {
    expect(component.mostrarModal).toBe(false);
    component.toggleModal();
    expect(component.mostrarModal).toBe(true);
    component.toggleModal();
    expect(component.mostrarModal).toBe(false);
  });

  it('should create new category', () => {
    component.nuevaCategoria = { nombreCategoria: 'Frenos', descripcion: 'Sistema de frenado' };
    component.esEdicion = false;
    component.guardarCategoria();
    expect(component.categorias.length).toBe(3);
    expect(component.categorias[2].nombreCategoria).toBe('Frenos');
  });

  it('should edit existing category', () => {
    const categoriaAEditar = component.categorias[0];
    component.toggleModal(categoriaAEditar);
    component.nuevaCategoria.nombreCategoria = 'Motor V8';
    component.guardarCategoria();
    expect(component.categorias[0].nombreCategoria).toBe('Motor V8');
  });

  it('should delete category', () => {
    const initialLength = component.categorias.length;
    spyOn(window, 'confirm').and.returnValue(true);
    component.eliminarCategoria(1);
    expect(component.categorias.length).toBe(initialLength - 1);
  });

  it('should not delete category if user cancels', () => {
    const initialLength = component.categorias.length;
    spyOn(window, 'confirm').and.returnValue(false);
    component.eliminarCategoria(1);
    expect(component.categorias.length).toBe(initialLength);
  });
});