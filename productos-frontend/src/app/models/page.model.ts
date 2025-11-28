export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number; // Página actual
  
  // --- AGREGAR ESTOS CAMPOS ---
  first: boolean;  // True si es la primera página
  last: boolean;   // True si es la última página
  empty: boolean;  // True si no hay resultados
}