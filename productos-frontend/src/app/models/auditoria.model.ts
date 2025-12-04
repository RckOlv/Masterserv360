export interface Auditoria {
    id: number;
    entidad: string;
    entidadId: string;
    accion: string;
    usuario: string;
    fecha: string;
    detalle: string;
    valorAnterior?: string;
    valorNuevo?: string;
}