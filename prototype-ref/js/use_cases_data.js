// Use Cases definitions for "En su punto SAC"
// Helps map the current user UI interaction directly to UML concepts

const USE_CASES = {
  'CU-01': {
    id: 'CU-01',
    title: 'Iniciar Sesión',
    actor: 'Personal de Restaurante (Mesero, Chef, Cajero, Administrador)',
    preconditions: 'El usuario debe estar registrado en el sistema con credenciales activas.',
    postconditions: 'El sistema otorga acceso al menú y funcionalidades específicas del rol asignado.',
    steps: [
      { num: 1, desc: 'Visualizar la pantalla de Login general.' },
      { num: 2, desc: 'Ingresar nombre de usuario y contraseña (o usar los accesos directos de simulación).' },
      { num: 3, desc: 'El sistema valida las credenciales contra la base de datos local.' },
      { num: 4, desc: 'El sistema inicializa la sesión y redirige al panel según el rol.' }
    ]
  },
  'CU-02': {
    id: 'CU-02',
    title: 'Registrar Pedido',
    actor: 'Mesero',
    preconditions: 'El mesero ha iniciado sesión y la mesa del cliente está libre.',
    postconditions: 'Se registra un nuevo pedido en estado "Pendiente" y la mesa cambia a "Esperando Comida".',
    steps: [
      { num: 1, desc: 'Visualizar el plano de mesas en el panel de Mesero.' },
      { num: 2, desc: 'Seleccionar una mesa que esté en estado "Libre".' },
      { num: 3, desc: 'Ver el formulario/catálogo interactivo de platos y categorías.' },
      { num: 4, desc: 'Agregar platos al pedido usando los botones de cantidad y opcionalmente añadir notas (ej: "Sin cebolla").' },
      { num: 5, desc: 'Verificar el resumen del pedido y el monto total calculado.' },
      { num: 6, desc: 'Hacer clic en "Enviar a Cocina" para registrar el pedido y mandarlo a la cola del Chef.' }
    ]
  },
  'CU-03': {
    id: 'CU-03',
    title: 'Preparar Pedido',
    actor: 'Chef (Cocina)',
    preconditions: 'El chef ha iniciado sesión y existen pedidos en cola enviados por el mesero.',
    postconditions: 'El estado del pedido cambia a "Listo para servir", notificando a los meseros.',
    steps: [
      { num: 1, desc: 'Visualizar la cola de pedidos pendientes en el panel de Cocina.' },
      { num: 2, desc: 'Seleccionar un pedido "Pendiente" y pulsar "Preparar" para iniciar la cocción.' },
      { num: 3, desc: 'El sistema actualiza el estado a "En Preparación" para que los meseros sepan el avance.' },
      { num: 4, desc: 'Una vez finalizados los platos, pulsar "Listo" para notificar que el pedido puede ser retirado.' }
    ]
  },
  'CU-04': {
    id: 'CU-04',
    title: 'Registrar Pago de Pedido',
    actor: 'Cajero',
    preconditions: 'El cajero ha iniciado sesión y existen mesas activas con pedidos terminados o consumidos.',
    postconditions: 'El pedido se marca como "Pagado", se genera el comprobante y la mesa queda "Libre".',
    steps: [
      { num: 1, desc: 'Visualizar la lista de mesas ocupadas o listas en la caja registradora.' },
      { num: 2, desc: 'Seleccionar la mesa del cliente para cargar su consumo total.' },
      { num: 3, desc: 'Verificar los ítems detallados y el importe total a cobrar.' },
      { num: 4, desc: 'Seleccionar el método de pago (Efectivo, Tarjeta, Yape/Plin).' },
      { num: 5, desc: 'Ingresar el importe recibido (en caso de efectivo) para calcular el vuelto exacto.' },
      { num: 6, desc: 'Hacer clic en "Completar Pago" para emitir la boleta de venta ficticia y liberar la mesa.' }
    ]
  },
  'CU-05': {
    id: 'CU-05',
    title: 'Mantener Platos (CRUD)',
    actor: 'Administrador',
    preconditions: 'El administrador ha iniciado sesión en el sistema.',
    postconditions: 'Los platos y precios de la carta del restaurante quedan actualizados.',
    steps: [
      { num: 1, desc: 'Ingresar a la sección "Carta de Platos" en el panel de Administrador.' },
      { num: 2, desc: 'Hacer clic en "Nuevo Plato" para agregar un elemento o seleccionar "Editar" en un plato de la lista.' },
      { num: 3, desc: 'Rellenar o modificar los campos: nombre, descripción, precio y categoría.' },
      { num: 4, desc: 'Guardar los cambios para actualizar la carta disponible de los meseros de inmediato.' },
      { num: 5, desc: 'Opcionalmente: pulsar "Eliminar" en un plato para inhabilitarlo del menú.' }
    ]
  },
  'CU-06': {
    id: 'CU-06',
    title: 'Mantener Usuarios',
    actor: 'Administrador',
    preconditions: 'El administrador ha iniciado sesión en el sistema.',
    postconditions: 'La lista de usuarios con acceso al sistema queda actualizada.',
    steps: [
      { num: 1, desc: 'Ingresar a la sección "Gestionar Personal" en el panel de Administrador.' },
      { num: 2, desc: 'Hacer clic en "Crear Nuevo Usuario" o pulsar en "Editar" en uno existente.' },
      { num: 3, desc: 'Definir el nombre completo, usuario, contraseña y asignar un Rol (Mesero, Chef, Cajero, Admin).' },
      { num: 4, desc: 'Confirmar la acción para guardar las credenciales.' }
    ]
  },
  'CU-07': {
    id: 'CU-07',
    title: 'Visualizar Reporte de Ventas',
    actor: 'Administrador',
    preconditions: 'El administrador ha iniciado sesión y se han cobrado pedidos en el día.',
    postconditions: 'Se muestran gráficos estadísticos de ingresos y platos de alta rotación.',
    steps: [
      { num: 1, desc: 'Ingresar a la sección "Reportes y Ventas" en el panel administrativo.' },
      { num: 2, desc: 'Visualizar los indicadores de resumen (Ventas Totales, Pedidos Atendidos).' },
      { num: 3, desc: 'Analizar el gráfico circular de métodos de pago y el gráfico de barra de platos más vendidos.' },
      { num: 4, desc: 'Examinar la bitácora/historial completo de transacciones realizadas en el día.' }
    ]
  },
  'CU-08': {
    id: 'CU-08',
    title: 'Modificar Pedido',
    actor: 'Mesero',
    preconditions: 'La mesa tiene un pedido activo registrado que no ha sido cobrado.',
    postconditions: 'El pedido se actualiza en base de datos, recalculando totales y alertando a cocina si corresponde.',
    steps: [
      { num: 1, desc: 'Visualizar el plano de mesas en el panel de Mesero.' },
      { num: 2, desc: 'Seleccionar una mesa ocupada y presionar "Modificar Pedido".' },
      { num: 3, desc: 'El sistema carga los platos del pedido actual en el constructor.' },
      { num: 4, desc: 'Agregar platos adicionales o cambiar notas (Flujo Adición).' },
      { num: 5, desc: 'Si reduce platos en preparación, confirmar la solicitud de autorización del Administrador (Simulado).' },
      { num: 6, desc: 'Pulsar "Guardar Cambios" para actualizar la comanda en cocina y caja.' }
    ]
  },
  'CU-09': {
    id: 'CU-09',
    title: 'Dividir Cuenta',
    actor: 'Cajero',
    preconditions: 'El cajero ha iniciado sesión y la mesa seleccionada tiene un pedido pendiente de pago.',
    postconditions: 'Se registran múltiples transacciones de cobro parciales hasta saldar la cuenta, liberando la mesa.',
    steps: [
      { num: 1, desc: 'Visualizar la mesa del cliente y seleccionar la opción "Dividir Cuenta".' },
      { num: 2, desc: 'Definir el número de partes (personas) en las que se dividirá el pago.' },
      { num: 3, desc: 'Visualizar el importe fraccionado correspondiente a cada cliente.' },
      { num: 4, desc: 'Seleccionar una parte pendiente de cobro y elegir su método de pago.' },
      { num: 5, desc: 'Procesar el cobro parcial y emitir la boleta fraccionada correspondiente.' },
      { num: 6, desc: 'Saldar todas las partes pendientes para liberar la mesa en el salón.' }
    ]
  }
};

// Help determine the current active step based on state
function getContextUseCase(role, view, substate) {
  if (!role) {
    return { cu: 'CU-01', step: 1 };
  }

  if (role === 'mesero') {
    if (view === 'mesas') {
      return { cu: 'CU-02', step: 1 };
    }
    if (view === 'nuevo_pedido') {
      if (substate && substate.isModifying) {
        if (substate.reviewing) {
          return { cu: 'CU-08', step: 6 };
        }
        if (substate.adminApprovalRequested) {
          return { cu: 'CU-08', step: 5 };
        }
        if (substate.selectedDishCount > 0) {
          return { cu: 'CU-08', step: 4 };
        }
        return { cu: 'CU-08', step: 3 };
      }
      
      // Flow CU-02: Registrar Pedido
      if (!substate || substate.selectedDishCount === 0) {
        return { cu: 'CU-02', step: 3 };
      }
      if (substate.writingNote) {
        return { cu: 'CU-02', step: 4 };
      }
      if (substate.reviewing) {
        return { cu: 'CU-02', step: 5 };
      }
      return { cu: 'CU-02', step: 4 };
    }
  }

  if (role === 'chef') {
    if (substate && substate.preparingOrderId) {
      return { cu: 'CU-03', step: 3 };
    }
    if (substate && substate.viewingList) {
      return { cu: 'CU-03', step: 1 };
    }
    return { cu: 'CU-03', step: 2 };
  }

  if (role === 'cajero') {
    if (substate && substate.isSplitting) {
      if (substate.splitInvoiceIndex !== undefined) return { cu: 'CU-09', step: 5 };
      if (substate.paymentMethodSelected) return { cu: 'CU-09', step: 4 };
      if (substate.splitsConfigured) return { cu: 'CU-09', step: 3 };
      if (substate.splitPanelOpen) return { cu: 'CU-09', step: 2 };
      return { cu: 'CU-09', step: 1 };
    }
    if (view === 'caja_main') {
      if (substate && substate.selectedOrderId) {
        if (substate.paymentSelected) {
          return { cu: 'CU-04', step: 5 };
        }
        return { cu: 'CU-04', step: 3 };
      }
      return { cu: 'CU-04', step: 1 };
    }
    if (view === 'checkout') {
      return { cu: 'CU-04', step: 4 };
    }
    if (view === 'invoice') {
      return { cu: 'CU-04', step: 6 };
    }
  }

  if (role === 'admin') {
    if (view === 'platos') {
      if (substate && substate.editing) return { cu: 'CU-05', step: 3 };
      if (substate && substate.creating) return { cu: 'CU-05', step: 2 };
      return { cu: 'CU-05', step: 1 };
    }
    if (view === 'usuarios') {
      if (substate && (substate.editing || substate.creating)) return { cu: 'CU-06', step: 3 };
      return { cu: 'CU-06', step: 1 };
    }
    if (view === 'reportes') {
      return { cu: 'CU-07', step: 2 };
    }
  }

  return null;
}
