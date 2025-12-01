export interface SolicitudProducto {
    id: number;
    descripcion: string;
    fechaSolicitud: string;
    procesado: boolean;
    
    // Nuevos campos planos
    clienteNombre: string;
    clienteTelefono: string;
    clienteEmail: string;
}