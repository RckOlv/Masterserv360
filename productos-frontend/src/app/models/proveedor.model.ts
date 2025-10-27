export interface ProveedorDTO {
  id?: number;
  razonSocial: string;
  cuit: string;
  email?: string;
  telefono?: string;
  direccion?: string;
  estado?: 'ACTIVO' | 'INACTIVO';
  
  // Campos de auditoría (solo lectura)
  fechaCreacion?: string; 
  fechaModificacion?: string;

  // ¡El campo M:N que añadimos!
  // (En TypeScript, usamos un array de números para los IDs)
  categoriaIds?: number[]; 
}