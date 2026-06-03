// State management for "En su punto SAC"
// Simulates a database stored in localStorage

const DEFAULT_USERS = [
  { id: 'usr-1', username: 'mesero', password: '123', name: 'Juan Pérez', role: 'mesero' },
  { id: 'usr-2', username: 'chef', password: '123', name: 'Carlos Ruiz', role: 'chef' },
  { id: 'usr-3', username: 'cajero', password: '123', name: 'María Gómez', role: 'cajero' },
  { id: 'usr-4', username: 'admin', password: '123', name: 'Ana Martínez', role: 'admin' }
];

const DEFAULT_DISHES = [
  { id: 'pl-1', name: 'Ceviche Carretillero', category: 'entradas', price: 25.00, desc: 'Pescado fresco marinado en limón con cebolla, camote y choclo, coronado con chicharrón de pota.', active: true },
  { id: 'pl-2', name: 'Papa a la Huancaína', category: 'entradas', price: 15.00, desc: 'Papas sancochadas bañadas en salsa cremosa de ají amarillo, queso y leche.', active: true },
  { id: 'pl-3', name: 'Causa Rellena de Pollo', category: 'entradas', price: 18.00, desc: 'Masa de papa amarilla sazonada con ají amarillo y limón, rellena de pollo, mayonesa y palta.', active: true },
  { id: 'pl-4', name: 'Lomo Saltado', category: 'segundos', price: 38.00, desc: 'Trozos de lomo de res salteados al wok con cebolla, tomate, ají amarillo, servido con papas fritas y arroz.', active: true },
  { id: 'pl-5', name: 'Ají de Gallina', category: 'segundos', price: 28.00, desc: 'Pechuga de pollo deshilachada en crema de ají amarillo, leche y nueces, acompañado de arroz y papa.', active: true },
  { id: 'pl-6', name: 'Arroz con Mariscos', category: 'segundos', price: 35.00, desc: 'Arroz sazonado con aderezo norteño y mariscos de estación (langostinos, calamar y conchas).', active: true },
  { id: 'pl-7', name: 'Cabrito a la Norteña', category: 'segundos', price: 42.00, desc: 'Tierno cabrito macerado en chicha de jora y culantro, servido con frejoles, yuca y arroz.', active: true },
  { id: 'pl-8', name: 'Chicha Morada 1L', category: 'bebidas', price: 15.00, desc: 'Bebida tradicional de maíz morado hervido con piña, manzana, canela y clavo de olor.', active: true },
  { id: 'pl-9', name: 'Limonada Frozen', category: 'bebidas', price: 12.00, desc: 'Limonada refrescante batida con hielo picado.', active: true },
  { id: 'pl-10', name: 'Gaseosa Personal', category: 'bebidas', price: 6.00, desc: 'Coca Cola o Inka Cola personal (500ml).', active: true },
  { id: 'pl-11', name: 'Cerveza Pilsen', category: 'bebidas', price: 10.00, desc: 'Cerveza nacional en botella personal.', active: true },
  { id: 'pl-12', name: 'Suspiro a la Limeña', category: 'postres', price: 12.00, desc: 'Crema de manjarblanco y yemas, coronado con merengue al oporto.', active: true },
  { id: 'pl-13', name: 'Torta Tres Leches', category: 'postres', price: 10.00, desc: 'Bizcochuelo bañado en tres tipos de leche, decorado con chantilly y canela.', active: true },
  { id: 'pl-14', name: 'Crema Volteada', category: 'postres', price: 8.00, desc: 'Clásico flan de leche condensada con caramelo líquido.', active: true }
];

const DEFAULT_TABLES = [
  { id: 1, name: 'Mesa 1', status: 'libre', orderId: null },
  { id: 2, name: 'Mesa 2', status: 'libre', orderId: null },
  { id: 3, name: 'Mesa 3', status: 'libre', orderId: null },
  { id: 4, name: 'Mesa 4', status: 'libre', orderId: null },
  { id: 5, name: 'Mesa 5', status: 'libre', orderId: null },
  { id: 6, name: 'Mesa 6', status: 'libre', orderId: null },
  { id: 7, name: 'Mesa 7', status: 'libre', orderId: null },
  { id: 8, name: 'Mesa 8', status: 'libre', orderId: null }
];

const HISTORIC_ORDERS = [
  {
    id: 'ord-hist-1',
    tableId: 1,
    tableName: 'Mesa 1',
    items: [
      { id: 'pl-1', name: 'Ceviche Carretillero', price: 25, quantity: 1, note: '' },
      { id: 'pl-4', name: 'Lomo Saltado', price: 38, quantity: 1, note: 'Término medio' },
      { id: 'pl-8', name: 'Chicha Morada 1L', price: 15, quantity: 1, note: '' }
    ],
    status: 'pagado',
    waiterName: 'Juan Pérez',
    total: 78.00,
    timestamp: '2026-06-01T12:30:00-05:00',
    paymentMethod: 'yape',
    paymentTimestamp: '2026-06-01T13:15:00-05:00'
  },
  {
    id: 'ord-hist-2',
    tableId: 5,
    tableName: 'Mesa 5',
    items: [
      { id: 'pl-2', name: 'Papa a la Huancaína', price: 15, quantity: 2, note: '' },
      { id: 'pl-5', name: 'Ají de Gallina', price: 28, quantity: 2, note: '' },
      { id: 'pl-10', name: 'Gaseosa Personal', price: 6, quantity: 3, note: 'Heladas' },
      { id: 'pl-13', name: 'Torta Tres Leches', price: 10, quantity: 2, note: '' }
    ],
    status: 'pagado',
    waiterName: 'Juan Pérez',
    total: 124.00,
    timestamp: '2026-06-01T13:00:00-05:00',
    paymentMethod: 'tarjeta',
    paymentTimestamp: '2026-06-01T14:10:00-05:00'
  },
  {
    id: 'ord-hist-3',
    tableId: 2,
    tableName: 'Mesa 2',
    items: [
      { id: 'pl-7', name: 'Cabrito a la Norteña', price: 42, quantity: 1, note: 'Con yuca extra' },
      { id: 'pl-9', name: 'Limonada Frozen', price: 12, quantity: 1, note: '' }
    ],
    status: 'pagado',
    waiterName: 'Juan Pérez',
    total: 54.00,
    timestamp: '2026-06-01T14:15:00-05:00',
    paymentMethod: 'efectivo',
    paymentTimestamp: '2026-06-01T14:50:00-05:00'
  },
  {
    id: 'ord-hist-4',
    tableId: 4,
    tableName: 'Mesa 4',
    items: [
      { id: 'pl-3', name: 'Causa Rellena de Pollo', price: 18, quantity: 1, note: '' },
      { id: 'pl-6', name: 'Arroz con Mariscos', price: 35, quantity: 1, note: '' },
      { id: 'pl-11', name: 'Cerveza Pilsen', price: 10, quantity: 2, note: '' }
    ],
    status: 'pagado',
    waiterName: 'Juan Pérez',
    total: 73.00,
    timestamp: '2026-06-01T15:00:00-05:00',
    paymentMethod: 'yape',
    paymentTimestamp: '2026-06-01T15:45:00-05:00'
  }
];

class AppState {
  static getStorageItem(key, defaultValue) {
    const val = localStorage.getItem(`ensupunto_${key}`);
    if (val) {
      try {
        return JSON.parse(val);
      } catch (e) {
        console.error("Error parsing localStorage key " + key, e);
      }
    }
    return defaultValue;
  }

  static setStorageItem(key, value) {
    localStorage.setItem(`ensupunto_${key}`, JSON.stringify(value));
  }

  // State initialization
  static init() {
    if (!localStorage.getItem('ensupunto_initialized')) {
      this.setStorageItem('users', DEFAULT_USERS);
      this.setStorageItem('dishes', DEFAULT_DISHES);
      this.setStorageItem('tables', DEFAULT_TABLES);
      this.setStorageItem('orders', HISTORIC_ORDERS);
      this.setStorageItem('current_session', null);
      localStorage.setItem('ensupunto_initialized', 'true');
    }
  }

  // Users
  static getUsers() {
    return this.getStorageItem('users', DEFAULT_USERS);
  }

  static saveUsers(users) {
    this.setStorageItem('users', users);
  }

  // Dishes
  static getDishes() {
    return this.getStorageItem('dishes', DEFAULT_DISHES).filter(d => d.active);
  }

  static getAllDishesIncludingInactive() {
    return this.getStorageItem('dishes', DEFAULT_DISHES);
  }

  static saveDishes(dishes) {
    this.setStorageItem('dishes', dishes);
  }

  // Tables
  static getTables() {
    return this.getStorageItem('tables', DEFAULT_TABLES);
  }

  static saveTables(tables) {
    this.setStorageItem('tables', tables);
  }

  static updateTableStatus(tableId, status, orderId = null) {
    const tables = this.getTables();
    const tableIndex = tables.findIndex(t => t.id === Number(tableId));
    if (tableIndex !== -1) {
      tables[tableIndex].status = status;
      tables[tableIndex].orderId = orderId;
      this.saveTables(tables);
    }
  }

  // Orders
  static getOrders() {
    return this.getStorageItem('orders', HISTORIC_ORDERS);
  }

  static saveOrders(orders) {
    this.setStorageItem('orders', orders);
  }

  static getActiveOrders() {
    return this.getOrders().filter(o => o.status !== 'pagado');
  }

  static createOrder(tableId, items, waiterName) {
    const orders = this.getOrders();
    const tables = this.getTables();
    const table = tables.find(t => t.id === Number(tableId));

    const total = items.reduce((acc, item) => acc + (item.price * item.quantity), 0);
    const newOrder = {
      id: `ord-${Date.now()}`,
      tableId: Number(tableId),
      tableName: table ? table.name : `Mesa ${tableId}`,
      items: items.map(it => ({
        id: it.id,
        name: it.name,
        price: it.price,
        quantity: it.quantity,
        note: it.note || ''
      })),
      status: 'cocina_pendiente', // cocina_pendiente, cocina_preparacion, cocina_listo, entregado
      waiterName: waiterName,
      total: total,
      timestamp: new Date().toISOString(),
      paymentMethod: null,
      paymentTimestamp: null
    };

    orders.push(newOrder);
    this.saveOrders(orders);
    
    // Update table status
    this.updateTableStatus(tableId, 'esperando_comida', newOrder.id);
    
    return newOrder;
  }

  static modifyOrder(orderId, items) {
    const orders = this.getOrders();
    const orderIndex = orders.findIndex(o => o.id === orderId);
    if (orderIndex !== -1) {
      const order = orders[orderIndex];
      order.items = items.map(it => ({
        id: it.id,
        name: it.name,
        price: it.price,
        quantity: it.quantity,
        note: it.note || ''
      }));
      order.total = items.reduce((acc, item) => acc + (item.price * item.quantity), 0);
      
      // Enviamos el pedido de vuelta a cocina para preparar las modificaciones
      order.status = 'cocina_pendiente';
      order.timestamp = new Date().toISOString(); // resetea el timer en cocina
      
      this.saveOrders(orders);
      this.updateTableStatus(order.tableId, 'esperando_comida', order.id);
      return order;
    }
    return null;
  }

  static updateOrderStatus(orderId, status) {
    const orders = this.getOrders();
    const orderIndex = orders.findIndex(o => o.id === orderId);
    if (orderIndex !== -1) {
      orders[orderIndex].status = status;
      this.saveOrders(orders);

      // Reflect on table status
      const order = orders[orderIndex];
      let tableStatus = 'libre';
      if (status === 'cocina_pendiente' || status === 'cocina_preparacion') {
        tableStatus = 'esperando_comida';
      } else if (status === 'cocina_listo') {
        tableStatus = 'cuenta_pedida'; // Or ready to serve
      } else if (status === 'entregado') {
        tableStatus = 'comiendo';
      } else if (status === 'pagado') {
        tableStatus = 'libre';
      }

      this.updateTableStatus(order.tableId, tableStatus, status === 'pagado' ? null : orderId);
      return orders[orderIndex];
    }
    return null;
  }

  static payOrder(orderId, paymentMethod) {
    const orders = this.getOrders();
    const orderIndex = orders.findIndex(o => o.id === orderId);
    if (orderIndex !== -1) {
      orders[orderIndex].status = 'pagado';
      orders[orderIndex].paymentMethod = paymentMethod;
      orders[orderIndex].paymentTimestamp = new Date().toISOString();
      this.saveOrders(orders);

      // Free the table
      const order = orders[orderIndex];
      this.updateTableStatus(order.tableId, 'libre', null);
      return orders[orderIndex];
    }
    return null;
  }

  // Session
  static getCurrentSession() {
    return this.getStorageItem('current_session', null);
  }

  static login(username, password) {
    const users = this.getUsers();
    const user = users.find(u => u.username === username.toLowerCase() && u.password === password);
    if (user) {
      const session = {
        userId: user.id,
        name: user.name,
        role: user.role,
        username: user.username
      };
      this.setStorageItem('current_session', session);
      return session;
    }
    return null;
  }

  static logout() {
    this.setStorageItem('current_session', null);
  }

  // Reset to default data
  static resetData() {
    localStorage.removeItem('ensupunto_users');
    localStorage.removeItem('ensupunto_dishes');
    localStorage.removeItem('ensupunto_tables');
    localStorage.removeItem('ensupunto_orders');
    localStorage.removeItem('ensupunto_current_session');
    localStorage.removeItem('ensupunto_initialized');
    this.init();
  }
}
