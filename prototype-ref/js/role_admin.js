// Administrator module logic for En su punto SAC

class RoleAdmin {
  static activeTab = 'reportes'; // 'reportes', 'platos', 'usuarios'

  static render(container, tab = 'reportes') {
    this.activeTab = tab;
    container.innerHTML = '';
    
    // Draw layout structure
    container.innerHTML = `
      <div class="fade-in">
        <div style="margin-bottom: 20px;">
          <h1 style="font-size: 1.8rem; color: white;">Panel de Control Administrativo</h1>
          <p style="color: var(--text-secondary); font-size: 0.9rem;">Gestión de menú de platos, cuentas de personal y analítica de ventas.</p>
        </div>

        <!-- Subtabs -->
        <div class="admin-tabs">
          <button class="admin-tab-btn ${this.activeTab === 'reportes' ? 'active' : ''}" data-tab="reportes">📊 Reportes y Ventas</button>
          <button class="admin-tab-btn ${this.activeTab === 'platos' ? 'active' : ''}" data-tab="platos">🍽️ Carta de Platos</button>
          <button class="admin-tab-btn ${this.activeTab === 'usuarios' ? 'active' : ''}" data-tab="usuarios">🧑‍🤝‍🧑 Personal y Acceso</button>
        </div>

        <div id="admin-tab-content">
          <!-- Loaded dynamically -->
        </div>
      </div>
    `;

    // Hook tab clicks
    container.querySelectorAll('.admin-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const selectedTab = btn.getAttribute('data-tab');
        this.render(container, selectedTab);
      });
    });

    const contentViewport = document.getElementById('admin-tab-content');
    
    if (this.activeTab === 'reportes') {
      this.renderReports(contentViewport);
    } else if (this.activeTab === 'platos') {
      this.renderPlatesCRUD(contentViewport);
    } else if (this.activeTab === 'usuarios') {
      this.renderUsersCRUD(contentViewport);
    }
  }

  // ----------------------------------------------------
  // 1. REPORTS SECTION
  // ----------------------------------------------------
  static renderReports(container) {
    UIRenderer.setActiveUseCase('CU-07', 1);

    const orders = AppState.getOrders();
    const paidOrders = orders.filter(o => o.status === 'pagado');

    // Calculations
    const totalSales = paidOrders.reduce((sum, o) => sum + o.total, 0);
    const orderCount = paidOrders.length;
    const avgTicket = orderCount > 0 ? (totalSales / orderCount) : 0;

    // Métodos de pago
    const paymentCounts = { efectivo: 0, tarjeta: 0, yape: 0 };
    paidOrders.forEach(o => {
      if (o.paymentMethod && paymentCounts[o.paymentMethod] !== undefined) {
        paymentCounts[o.paymentMethod] += o.total;
      }
    });

    // Platos más vendidos
    const dishSales = {};
    paidOrders.forEach(o => {
      o.items.forEach(it => {
        dishSales[it.name] = (dishSales[it.name] || 0) + it.quantity;
      });
    });

    // Sort popular dishes
    const topDishes = Object.keys(dishSales)
      .map(name => ({ name, qty: dishSales[name] }))
      .sort((a, b) => b.qty - a.qty)
      .slice(0, 5);

    // Render HTML structure
    container.innerHTML = `
      <div class="fade-in">
        <!-- KPIs Row -->
        <div class="admin-kpi-grid">
          <div class="glass-card kpi-card">
            <div class="kpi-icon">💰</div>
            <div class="kpi-data">
              <span class="kpi-label">Ingresos Totales</span>
              <span class="kpi-value">S/. ${totalSales.toFixed(2)}</span>
            </div>
          </div>
          <div class="glass-card kpi-card">
            <div class="kpi-icon">🍽️</div>
            <div class="kpi-data">
              <span class="kpi-label">Pedidos Cobrados</span>
              <span class="kpi-value">${orderCount} pedidos</span>
            </div>
          </div>
          <div class="glass-card kpi-card">
            <div class="kpi-icon">🎟️</div>
            <div class="kpi-data">
              <span class="kpi-label">Ticket Promedio</span>
              <span class="kpi-value">S/. ${avgTicket.toFixed(2)}</span>
            </div>
          </div>
        </div>

        <!-- Charts Row -->
        <div class="reports-layout">
          <div class="glass-card chart-card">
            <div class="chart-title">Distribución por Métodos de Pago (S/.)</div>
            <div class="chart-canvas-container">
              <canvas id="paymentChart" width="300" height="220"></canvas>
            </div>
          </div>
          <div class="glass-card chart-card">
            <div class="chart-title">Top 5 Platos Más Vendidos (Unidades)</div>
            <div class="chart-canvas-container">
              <canvas id="dishChart" width="300" height="220"></canvas>
            </div>
          </div>
        </div>

        <!-- Recent Transactions Table -->
        <div class="glass-card" style="margin-top: 24px;">
          <h3 style="font-size: 1.1rem; color: white; margin-bottom: 15px;">Historial de Pedidos Cobrados (Hoy)</h3>
          
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Pedido ID</th>
                  <th>Mesa</th>
                  <th>Ítems</th>
                  <th>Método Pago</th>
                  <th>Hora Pago</th>
                  <th style="text-align: right;">Total</th>
                </tr>
              </thead>
              <tbody>
                ${paidOrders.map(o => {
                  const itemsStr = o.items.map(it => `${it.quantity}x ${it.name}`).join(', ');
                  const paymentBadge = o.paymentMethod === 'yape' 
                    ? '<span class="badge badge-eating" style="font-size: 0.65rem;">Yape/Plin</span>'
                    : o.paymentMethod === 'tarjeta'
                    ? '<span class="badge badge-ready" style="font-size: 0.65rem;">Tarjeta</span>'
                    : '<span class="badge badge-free" style="font-size: 0.65rem;">Efectivo</span>';

                  const formatTime = o.paymentTimestamp 
                    ? new Date(o.paymentTimestamp).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
                    : '-';

                  return `
                    <tr>
                      <td style="font-family: monospace; font-size: 0.8rem; font-weight: bold; color: var(--text-primary);">${o.id.split('-')[1] || o.id}</td>
                      <td><strong>${o.tableName}</strong></td>
                      <td style="max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${itemsStr}">${itemsStr}</td>
                      <td>${paymentBadge}</td>
                      <td>${formatTime}</td>
                      <td style="text-align: right; font-weight: bold; color: var(--accent);">S/. ${o.total.toFixed(2)}</td>
                    </tr>
                  `;
                }).reverse().join('')}
                ${paidOrders.length === 0 ? '<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">Aún no se registran cobros en esta sesión.</td></tr>' : ''}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `;

    // Trigger step 2 for visualization
    UIRenderer.setActiveUseCase('CU-07', 2);

    // Draw Custom Charts
    setTimeout(() => {
      this.drawPaymentChart(paymentCounts);
      this.drawDishChart(topDishes);
    }, 50);
  }

  // Draw Donut/Pie Chart on HTML5 Canvas
  static drawPaymentChart(paymentCounts) {
    const canvas = document.getElementById('paymentChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    
    const data = [
      { label: 'Efectivo', val: paymentCounts.efectivo, color: '#10b981' }, // Green
      { label: 'Tarjeta', val: paymentCounts.tarjeta, color: '#8b5cf6' }, // Purple
      { label: 'Yape/Plin', val: paymentCounts.yape, color: '#3b82f6' }  // Blue
    ];

    const total = data.reduce((sum, item) => sum + item.val, 0);

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (total === 0) {
      // Empty state
      ctx.fillStyle = '#64748b';
      ctx.font = '13px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('Sin datos de ventas registradas hoy.', canvas.width / 2, canvas.height / 2);
      return;
    }

    // Coordinates
    const centerX = 100;
    const centerY = 110;
    const radius = 70;

    let startAngle = 0;

    data.forEach(slice => {
      if (slice.val === 0) return;
      const sliceAngle = (slice.val / total) * 2 * Math.PI;

      // Draw Arc
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle);
      ctx.lineTo(centerX, centerY);
      ctx.fillStyle = slice.color;
      ctx.fill();

      startAngle += sliceAngle;
    });

    // Draw middle hole for Donut effect
    ctx.beginPath();
    ctx.arc(centerX, centerY, radius * 0.5, 0, 2 * Math.PI);
    ctx.fillStyle = '#131b2e'; // matches --bg-secondary
    ctx.fill();

    // Draw Legends
    ctx.textAlign = 'left';
    ctx.font = '12px sans-serif';
    data.forEach((slice, idx) => {
      const percentage = total > 0 ? ((slice.val / total) * 100).toFixed(0) : 0;
      
      // Color Indicator box
      ctx.fillStyle = slice.color;
      ctx.fillRect(190, 50 + (idx * 35), 12, 12);

      // Label Text
      ctx.fillStyle = '#f8fafc';
      ctx.fillText(slice.label, 210, 60 + (idx * 35));

      // Value text
      ctx.fillStyle = '#94a3b8';
      ctx.font = '10px sans-serif';
      ctx.fillText(`S/. ${slice.val.toFixed(2)} (${percentage}%)`, 210, 72 + (idx * 35));
      ctx.font = '12px sans-serif';
    });
  }

  // Draw Horizontal Bar Chart on HTML5 Canvas
  static drawDishChart(topDishes) {
    const canvas = document.getElementById('dishChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (topDishes.length === 0) {
      ctx.fillStyle = '#64748b';
      ctx.font = '13px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('Esperando ventas de platos.', canvas.width / 2, canvas.height / 2);
      return;
    }

    const maxQty = Math.max(...topDishes.map(d => d.qty), 1);
    const barHeight = 22;
    const gap = 16;
    const chartOffsetLeft = 110;
    const chartWidth = 140;

    ctx.textAlign = 'left';
    ctx.font = '11px sans-serif';

    topDishes.forEach((dish, idx) => {
      const y = 30 + (idx * (barHeight + gap));
      const width = (dish.qty / maxQty) * chartWidth;

      // Label text (Truncate if needed)
      ctx.fillStyle = '#f8fafc';
      const shortName = dish.name.length > 15 ? dish.name.substring(0, 14) + '..' : dish.name;
      ctx.fillText(shortName, 10, y + 15);

      // Draw Bar background track
      ctx.fillStyle = 'rgba(255, 255, 255, 0.03)';
      ctx.fillRect(chartOffsetLeft, y, chartWidth, barHeight);

      // Draw Bar filling
      ctx.fillStyle = '#ff6600'; // --accent
      ctx.fillRect(chartOffsetLeft, y, width, barHeight);

      // Draw Value text
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 10px sans-serif';
      ctx.fillText(`${dish.qty} un.`, chartOffsetLeft + width + 8, y + 15);
      ctx.font = '11px sans-serif';
    });
  }

  // ----------------------------------------------------
  // 2. PLATES CRUD SECTION
  // ----------------------------------------------------
  static renderPlatesCRUD(container) {
    // Set UC Track
    UIRenderer.setActiveUseCase('CU-05', 1);

    const dishes = AppState.getAllDishesIncludingInactive().filter(d => d.active);

    let rowsHTML = '';
    dishes.forEach(dish => {
      let categoryLabel = 'Entrada';
      if (dish.category === 'segundos') categoryLabel = 'Segundo';
      if (dish.category === 'bebidas') categoryLabel = 'Bebida';
      if (dish.category === 'postres') categoryLabel = 'Postre';

      rowsHTML += `
        <tr>
          <td><strong>${dish.name}</strong></td>
          <td><span class="badge badge-eating" style="font-size:0.7rem;">${categoryLabel}</span></td>
          <td style="max-width:300px; text-overflow:ellipsis; overflow:hidden; white-space:nowrap;" title="${dish.desc}">${dish.desc}</td>
          <td style="font-weight:bold; color:var(--accent);">S/. ${dish.price.toFixed(2)}</td>
          <td>
            <div style="display:flex; gap:8px;">
              <button class="btn btn-secondary btn-sm btn-edit-dish" data-dish-id="${dish.id}">Editar</button>
              <button class="btn btn-danger btn-sm btn-delete-dish" data-dish-id="${dish.id}">Eliminar</button>
            </div>
          </td>
        </tr>
      `;
    });

    container.innerHTML = `
      <div class="glass-card fade-in">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
          <div>
            <h3 style="font-size: 1.15rem; color: white;">Carta del Menú</h3>
            <p style="font-size:0.8rem; color:var(--text-secondary);">Agrega, modifica o elimina platos de la oferta del restaurante.</p>
          </div>
          <button class="btn btn-primary btn-sm" id="btn-create-dish">+ Nuevo Plato</button>
        </div>

        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Nombre del Plato</th>
                <th>Categoría</th>
                <th>Descripción</th>
                <th>Precio</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              ${rowsHTML}
              ${dishes.length === 0 ? '<tr><td colspan="5" style="text-align:center;">No hay platos registrados.</td></tr>' : ''}
            </tbody>
          </table>
        </div>
      </div>
    `;

    // Hook Create Dish
    document.getElementById('btn-create-dish').addEventListener('click', () => {
      UIRenderer.setActiveUseCase('CU-05', 2);
      this.openDishFormModal(null, container);
    });

    // Hook Edits
    container.querySelectorAll('.btn-edit-dish').forEach(btn => {
      btn.addEventListener('click', () => {
        const dishId = btn.getAttribute('data-dish-id');
        
        UIRenderer.setActiveUseCase('CU-05', 2);
        
        this.openDishFormModal(dishId, container);
      });
    });

    // Hook Deletes
    container.querySelectorAll('.btn-delete-dish').forEach(btn => {
      btn.addEventListener('click', () => {
        const dishId = btn.getAttribute('data-dish-id');
        const dish = dishes.find(d => d.id === dishId);
        
        UIRenderer.setActiveUseCase('CU-05', 5);

        UIRenderer.showModal(
          'Eliminar Plato',
          `<p style="color:var(--text-secondary);">¿Estás seguro de eliminar el plato <strong>${dish.name}</strong> de la carta?<br>Ya no aparecerá disponible para los pedidos de los meseros.</p>`,
          [
            {
              text: 'Cancelar',
              class: 'btn-secondary',
              onClick: (e, modalRef) => modalRef.closeModal()
            },
            {
              text: 'Sí, Eliminar',
              class: 'btn-danger',
              onClick: (e, modalRef) => {
                const allDishes = AppState.getAllDishesIncludingInactive();
                const dishIndex = allDishes.findIndex(d => d.id === dishId);
                if (dishIndex !== -1) {
                  // Simply deactivate to keep historic consistency
                  allDishes[dishIndex].active = false;
                  AppState.saveDishes(allDishes);
                  
                  UIRenderer.logAction(`Plato "${dish.name}" eliminado de la carta.`, 'warn');
                  modalRef.closeModal();
                  
                  // Reload view
                  this.renderPlatesCRUD(container);
                }
              }
            }
          ]
        );
      });
    });
  }

  // Opens Dialog form to Create/Edit Dish
  static openDishFormModal(dishId = null, tabContainer) {
    const dishes = AppState.getAllDishesIncludingInactive();
    const dish = dishId ? dishes.find(d => d.id === dishId) : null;
    const isEdit = !!dish;

    const modalTitle = isEdit ? 'Modificar Plato' : 'Registrar Nuevo Plato';

    const modalBody = `
      <form id="dish-form" style="display:flex; flex-direction:column; gap:15px;">
        <div class="form-group" style="margin-bottom:0;">
          <label for="form-dish-name">Nombre del Plato</label>
          <input type="text" id="form-dish-name" class="form-control" value="${isEdit ? dish.name : ''}" required>
        </div>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:15px;">
          <div class="form-group" style="margin-bottom:0;">
            <label for="form-dish-category">Categoría</label>
            <select id="form-dish-category" class="form-control">
              <option value="entradas" ${isEdit && dish.category === 'entradas' ? 'selected' : ''}>Entrada</option>
              <option value="segundos" ${isEdit && dish.category === 'segundos' ? 'selected' : ''}>Segundo</option>
              <option value="bebidas" ${isEdit && dish.category === 'bebidas' ? 'selected' : ''}>Bebida</option>
              <option value="postres" ${isEdit && dish.category === 'postres' ? 'selected' : ''}>Postre</option>
            </select>
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label for="form-dish-price">Precio (S/.)</label>
            <input type="number" id="form-dish-price" class="form-control" value="${isEdit ? dish.price.toFixed(2) : '10.00'}" step="0.50" min="0.50" required>
          </div>
        </div>
        <div class="form-group" style="margin-bottom:0;">
          <label for="form-dish-desc">Descripción del Plato</label>
          <textarea id="form-dish-desc" class="form-control" style="height:80px; resize:none;" required>${isEdit ? dish.desc : ''}</textarea>
        </div>
      </form>
    `;

    const footerButtons = [
      {
        text: 'Cancelar',
        class: 'btn-secondary',
        onClick: (e, modalRef) => modalRef.closeModal()
      },
      {
        text: isEdit ? 'Guardar Cambios' : 'Registrar Plato',
        class: 'btn-primary',
        onClick: (e, modalRef) => {
          const form = document.getElementById('dish-form');
          if (!form.reportValidity()) return;

          const name = document.getElementById('form-dish-name').value.trim();
          const category = document.getElementById('form-dish-category').value;
          const price = parseFloat(document.getElementById('form-dish-price').value);
          const desc = document.getElementById('form-dish-desc').value.trim();

          UIRenderer.setActiveUseCase('CU-05', 3);

          if (isEdit) {
            // Edit
            const index = dishes.findIndex(d => d.id === dishId);
            if (index !== -1) {
              dishes[index].name = name;
              dishes[index].category = category;
              dishes[index].price = price;
              dishes[index].desc = desc;
              
              AppState.saveDishes(dishes);
              UIRenderer.logAction(`Plato "${name}" modificado en la base de datos.`, 'success');
            }
          } else {
            // New
            const newDish = {
              id: `pl-${Date.now()}`,
              name,
              category,
              price,
              desc,
              active: true
            };
            dishes.push(newDish);
            AppState.saveDishes(dishes);
            UIRenderer.logAction(`Nuevo plato "${name}" añadido a la carta.`, 'success');
          }

          UIRenderer.setActiveUseCase('CU-05', 4);
          modalRef.closeModal();

          // Refresh viewport
          this.renderPlatesCRUD(tabContainer);
        }
      }
    ];

    UIRenderer.showModal(modalTitle, modalBody, footerButtons);
  }

  // ----------------------------------------------------
  // 3. USERS CRUD SECTION
  // ----------------------------------------------------
  static renderUsersCRUD(container) {
    UIRenderer.setActiveUseCase('CU-06', 1);

    const users = AppState.getUsers();

    let rowsHTML = '';
    users.forEach(user => {
      rowsHTML += `
        <tr>
          <td><strong>${user.name}</strong></td>
          <td><span class="badge badge-waiting" style="font-size:0.65rem; background:rgba(255,255,255,0.05); color:white; border-color:var(--border-color);">${user.role.toUpperCase()}</span></td>
          <td><code style="color:var(--text-secondary); background:rgba(0,0,0,0.2); padding:2px 6px; border-radius:4px;">${user.username}</code></td>
          <td><code style="color:var(--text-muted); font-size:0.75rem;">${user.password}</code></td>
          <td>
            <div style="display:flex; gap:8px;">
              <button class="btn btn-secondary btn-sm btn-edit-user" data-user-id="${user.id}">Editar</button>
              <button class="btn btn-danger btn-sm btn-delete-user" data-user-id="${user.id}" ${user.username === 'admin' ? 'disabled' : ''}>Quitar</button>
            </div>
          </td>
        </tr>
      `;
    });

    container.innerHTML = `
      <div class="glass-card fade-in">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
          <div>
            <h3 style="font-size: 1.15rem; color: white;">Personal Registrado</h3>
            <p style="font-size:0.8rem; color:var(--text-secondary);">Gestiona los accesos y roles del personal del restaurante.</p>
          </div>
          <button class="btn btn-primary btn-sm" id="btn-create-user">+ Nuevo Colaborador</button>
        </div>

        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Nombre del Empleado</th>
                <th>Rol de Sistema</th>
                <th>Nombre Usuario</th>
                <th>Contraseña</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              ${rowsHTML}
            </tbody>
          </table>
        </div>
      </div>
    `;

    // Hook Create User
    document.getElementById('btn-create-user').addEventListener('click', () => {
      UIRenderer.setActiveUseCase('CU-06', 2); // Click to open form
      this.openUserFormModal(null, container);
    });

    // Hook Edit User
    container.querySelectorAll('.btn-edit-user').forEach(btn => {
      btn.addEventListener('click', () => {
        const userId = btn.getAttribute('data-user-id');
        UIRenderer.setActiveUseCase('CU-06', 2);
        this.openUserFormModal(userId, container);
      });
    });

    // Hook Remove User
    container.querySelectorAll('.btn-delete-user').forEach(btn => {
      btn.addEventListener('click', () => {
        const userId = btn.getAttribute('data-user-id');
        const user = users.find(u => u.id === userId);

        UIRenderer.showModal(
          'Dar de Baja Colaborador',
          `<p style="color:var(--text-secondary);">¿Estás seguro de inhabilitar a <strong>${user.name}</strong> del sistema?</p>`,
          [
            {
              text: 'Cancelar',
              class: 'btn-secondary',
              onClick: (e, modalRef) => modalRef.closeModal()
            },
            {
              text: 'Confirmar Baja',
              class: 'btn-danger',
              onClick: (e, modalRef) => {
                const allUsers = AppState.getUsers();
                const uIndex = allUsers.findIndex(u => u.id === userId);
                if (uIndex !== -1) {
                  allUsers.splice(uIndex, 1);
                  AppState.saveUsers(allUsers);
                  
                  UIRenderer.logAction(`Colaborador "${user.name}" dado de baja.`, 'warn');
                  modalRef.closeModal();
                  
                  // Reload
                  this.renderUsersCRUD(container);
                }
              }
            }
          ]
        );
      });
    });
  }

  // Opens User creation form
  static openUserFormModal(userId = null, tabContainer) {
    const users = AppState.getUsers();
    const user = userId ? users.find(u => u.id === userId) : null;
    const isEdit = !!user;

    const modalTitle = isEdit ? 'Editar Acceso Colaborador' : 'Registrar Colaborador';

    const modalBody = `
      <form id="user-form" style="display:flex; flex-direction:column; gap:15px;">
        <div class="form-group" style="margin-bottom:0;">
          <label for="form-user-name">Nombre Completo</label>
          <input type="text" id="form-user-name" class="form-control" value="${isEdit ? user.name : ''}" required placeholder="Ej: Luis Valdivia">
        </div>
        <div class="form-group" style="margin-bottom:0;">
          <label for="form-user-role">Rol Asignado</label>
          <select id="form-user-role" class="form-control" ${isEdit && user.username === 'admin' ? 'disabled' : ''}>
            <option value="mesero" ${isEdit && user.role === 'mesero' ? 'selected' : ''}>Mesero</option>
            <option value="chef" ${isEdit && user.role === 'chef' ? 'selected' : ''}>Chef (Cocina)</option>
            <option value="cajero" ${isEdit && user.role === 'cajero' ? 'selected' : ''}>Cajero</option>
            <option value="admin" ${isEdit && user.role === 'admin' ? 'selected' : ''}>Administrador</option>
          </select>
        </div>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:15px;">
          <div class="form-group" style="margin-bottom:0;">
            <label for="form-user-username">Nombre de Usuario</label>
            <input type="text" id="form-user-username" class="form-control" value="${isEdit ? user.username : ''}" required placeholder="Ej: luis.v" ${isEdit ? 'disabled' : ''}>
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label for="form-user-pwd">Contraseña</label>
            <input type="text" id="form-user-pwd" class="form-control" value="${isEdit ? user.password : '123'}" required>
          </div>
        </div>
      </form>
    `;

    const footerButtons = [
      {
        text: 'Cancelar',
        class: 'btn-secondary',
        onClick: (e, modalRef) => modalRef.closeModal()
      },
      {
        text: isEdit ? 'Guardar Cambios' : 'Registrar Colaborador',
        class: 'btn-primary',
        onClick: (e, modalRef) => {
          const form = document.getElementById('user-form');
          if (!form.reportValidity()) return;

          const name = document.getElementById('form-user-name').value.trim();
          const role = document.getElementById('form-user-role').value;
          const pwd = document.getElementById('form-user-pwd').value.trim();

          UIRenderer.setActiveUseCase('CU-06', 3);

          if (isEdit) {
            const index = users.findIndex(u => u.id === userId);
            if (index !== -1) {
              users[index].name = name;
              if (users[index].username !== 'admin') {
                users[index].role = role;
              }
              users[index].password = pwd;
              AppState.saveUsers(users);
              
              UIRenderer.logAction(`Usuario "${users[index].username}" actualizado.`, 'success');
            }
          } else {
            const username = document.getElementById('form-user-username').value.trim().toLowerCase();
            
            // Check username conflicts
            if (users.some(u => u.username === username)) {
              alert('Este nombre de usuario ya está registrado.');
              return;
            }

            const newUser = {
              id: `usr-${Date.now()}`,
              name,
              role,
              username,
              password: pwd
            };
            users.push(newUser);
            AppState.saveUsers(users);
            
            UIRenderer.logAction(`Nuevo usuario "${username}" registrado como ${role.toUpperCase()}.`, 'success');
          }

          UIRenderer.setActiveUseCase('CU-06', 4);
          modalRef.closeModal();

          // Refresh viewport
          this.renderUsersCRUD(tabContainer);
        }
      }
    ];

    UIRenderer.showModal(modalTitle, modalBody, footerButtons);
  }
}
