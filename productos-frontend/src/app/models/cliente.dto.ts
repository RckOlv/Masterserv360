export interface ClienteDTO {
    id?: number;
    nombre: string;
    apellido: string;
    email: string;
    tipoDocumentoBusqueda: string; // 'DNI', 'CUIT', etc.
    documento: string;
    telefono: string;
    // Opcionales que quizás no usas en el form pero el backend acepta
    tipoDocumentoId?: number; 
    codigoPais?: string;
}