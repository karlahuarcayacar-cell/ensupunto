// Waiter (Mesero) module logic for En su punto SAC

class RoleMesero {
  static activeView = 'mesas'; // 'mesas' or 'nuevo_pedido'
  static activeTableId = null;
  static currentOrderItems = []; // { id, name, price, quantity, note }
  static isModifying = false;
  static modifyingOrderId = null;
  static initialOrderItemsBackup = [];
  static adminApprovedCancellations = {};

  static render(container, subView = 'mesas') {
    this.activeView = subView;
    container.innerHTML = '';
    
    if (this.activeView === 'mesas') {
      this.renderTablesView(container);
    } else if (this.activeView === 'nuevo_pedido') {
      this.renderOrderCreatorView(container);
    }
  }

  // Renders the Tables Layout
  static renderTablesView(container) {
    UIRenderer.setActiveUseCase('CU-02', 1);

    const tables = AppState.getTables();
    const orders = AppState.getOrders();

    let gridHTML = '<div class="tables-grid">';
    tables.forEach(table => {
      let badgeClass = 'badge-free';
      let badgeLabel = 'Libre';
      let cardStateClass = 'state-libre';
      let infoHTML = '<p style="font-size: 0.8rem; color: var(--text-muted);">Mesa disponible para nuevos clientes.</p>';
      let actionBtnHTML = `<button class="btn btn-primary btn-sm btn-action-table" data-table-id="${table.id}" data-action="new">Nueva Orden</button>`;

      if (table.status !== 'libre') {
        const order = orders.find(o => o.id === table.orderId);
        const totalAmount = order ? order.total.toFixed(2) : '0.00';
        const orderItemsCount = order ? order.items.reduce((a, b) => a + b.quantity, 0) : 0;

        if (table.status === 'esperando_comida') {
          badgeClass = 'badge-waiting';
          badgeLabel = 'En Cola';
          cardStateClass = 'state-esperando_comida';
        } else if (table.status === 'cocina_preparacion') {
          badgeClass = 'badge-cooking';
          badgeLabel = 'En Preparación';
          cardStateClass = 'state-cocina_preparacion';
        } else if (table.status === 'comiendo') {
          badgeClass = 'badge-eating';
          badgeLabel = 'Consumiendo';
          cardStateClass = 'state-comiendo';
        } else if (table.status === 'cuenta_pedida') {
          badgeClass = 'badge-ready';
          badgeLabel = 'Listo / Por Cobrar';
          cardStateClass = 'state-cuenta_pedida';
        }

        infoHTML = `
          <div class="table-order-total">S/. ${totalAmount}</div>
          <div class="table-order-time">${orderItemsCount} platos solicitados</div>
          <div style="font-size: 0.72rem; color: var(--text-muted); margin-top: 4px;">Mesero: ${order ? order.waiterName : 'Sistema'}</div>
        `;

        actionBtnHTML = `
          <button class="btn btn-secondary btn-sm btn-action-table" data-table-id="${table.id}" data-action="view">Detalles</button>
        `;
      }

      gridHTML += `
        <div class="glass-card table-card ${cardStateClass}" data-table-card-id="${table.id}">
          <div class="table-header">
            <span class="table-number">${table.name}</span>
            <span class="badge ${badgeClass}">${badgeLabel}</span>
          </div>
          <div class="table-body-info">
            ${infoHTML}
          </div>
          <div class="table-footer-actions">
            ${actionBtnHTML}
          </div>
        </div>
      `;
    });
    gridHTML += '</div>';

    container.innerHTML = `
      <div class="fade-in">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px;">
          <div>
            <h1 style="font-size: 1.8rem; color: white;">Salón de Mesas</h1>
            <p style="color: var(--text-secondary); font-size: 0.9rem;">Selecciona una mesa para registrar o actualizar pedidos.</p>
          </div>
          <button class="btn btn-secondary" id="btn-refresh-tables" style="padding: 8px 14px;">🔄 Actualizar</button>
        </div>
        ${gridHTML}
      </div>
    `;

    // Click Handlers for Table Actions
    container.querySelectorAll('.btn-action-table').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const tableId = Number(btn.getAttribute('data-table-id'));
        const action = btn.getAttribute('data-action');

        if (action === 'new') {
          UIRenderer.setActiveUseCase('CU-02', 2);
          UIRenderer.logAction(`Mesero inició toma de pedido para Mesa ${tableId}`, 'action');
          this.startNewOrder(tableId, container);
        } else {
          this.viewTableDetails(tableId);
        }
      });
    });

    container.querySelectorAll('.table-card').forEach(card => {
      card.addEventListener('click', () => {
        const tableId = Number(card.getAttribute('data-table-card-id'));
        const table = tables.find(t => t.id === tableId);
        if (table.status === 'libre') {
          UIRenderer.setActiveUseCase('CU-02', 2);
          UIRenderer.logAction(`Mesero seleccionó Mesa ${tableId} libre`, 'action');
          this.startNewOrder(tableId, container);
        } else {
          this.viewTableDetails(tableId);
        }
      });
    });

    document.getElementById('btn-refresh-tables').addEventListener('click', () => {
      this.render(container, 'mesas');
    });
  }

  // Sets state for new order and redirects to creator
  static startNewOrder(tableId, container) {
    this.activeTableId = tableId;
    this.currentOrderItems = [];
    this.isModifying = false;
    this.modifyingOrderId = null;
    this.render(container, 'nuevo_pedido');
  }

  // Sets state for modifying order and redirects to creator
  static startModifyOrder(tableId, orderId, container) {
    this.activeTableId = tableId;
    this.isModifying = true;
    this.modifyingOrderId = orderId;
    this.adminApprovedCancellations = {};
    
    const order = AppState.getOrders().find(o => o.id === orderId);
    if (order) {
      this.currentOrderItems = JSON.parse(JSON.stringify(order.items));
      this.initialOrderItemsBackup = JSON.parse(JSON.stringify(order.items));
      
      UIRenderer.setActiveUseCase('CU-08', 3);
      UIRenderer.logAction(`Mesero inició modificación de Pedido ${orderId} (Mesa ${tableId})`, 'action');
      this.render(container, 'nuevo_pedido');
    }
  }

  // Renders the Interactive Order Builder
  static renderOrderCreatorView(container) {
    const isEditing = this.isModifying;
    
    if (isEditing) {
      UIRenderer.setActiveUseCase('CU-08', 3);
    } else {
      UIRenderer.setActiveUseCase('CU-02', 3);
    }

    const dishes = AppState.getDishes();
    const tableId = this.activeTableId;
    const table = AppState.getTables().find(t => t.id === tableId);

    // Initial Category: entradas
    let activeCategory = 'entradas';

    const titleText = isEditing ? `Modificar Pedido: ${table ? table.name : `Mesa ${tableId}`}` : `Nuevo Pedido: ${table ? table.name : `Mesa ${tableId}`}`;
    const taglineText = isEditing ? `Agrega platos o solicita modificaciones al pedido del cliente.` : `Agrega platos y bebidas al pedido del cliente.`;
    const actionBtnText = isEditing ? `💾 Guardar Cambios` : `🔥 Enviar a Cocina`;
    const cancelBtnText = isEditing ? `← Cancelar Edición` : `← Volver al Salón`;

    container.innerHTML = `
      <div class="fade-in" style="height: 100%;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
          <div>
            <h1 style="font-size: 1.6rem; color: white;">${titleText}</h1>
            <p style="color: var(--text-secondary); font-size: 0.85rem;">${taglineText}</p>
          </div>
          <button class="btn btn-secondary" id="btn-cancel-order">${cancelBtnText}</button>
        </div>

        <div class="order-creator-container">
          <!-- 1. Categories Sidebar -->
          <nav class="categories-nav">
            <button class="category-tab-btn active" data-cat="entradas">🥗 Entradas</button>
            <button class="category-tab-btn" data-cat="segundos">🍲 Segundos</button>
            <button class="category-tab-btn" data-cat="bebidas">🥤 Bebidas</button>
            <button class="category-tab-btn" data-cat="postres">🍰 Postres</button>
          </nav>

          <!-- 2. Dishes Grid Center -->
          <div class="dishes-catalog-container" id="dishes-catalog">
            <!-- Loaded dynamically by active category -->
          </div>

          <!-- 3. Summary Panel Right -->
          <div class="glass-card order-summary-panel">
            <h3 style="font-size: 1.1rem; border-bottom: 1px solid var(--border-color); padding-bottom: 10px;">Detalle del Pedido</h3>
            
            <div class="summary-items-list" id="order-summary-items">
              <!-- Empty state by default -->
            </div>

            <div class="summary-totals-box">
              <div class="summary-totals-row">
                <span>Subtotal (sin IGV)</span>
                <span id="summary-subtotal">S/. 0.00</span>
              </div>
              <div class="summary-totals-row">
                <span>IGV (18%)</span>
                <span id="summary-igv">S/. 0.00</span>
              </div>
              <div class="summary-totals-row grand-total">
                <span>Total</span>
                <span id="summary-total">S/. 0.00</span>
              </div>
            </div>

            <button class="btn btn-primary" id="btn-send-to-kitchen" style="width: 100%; padding: 12px;" disabled>
              ${actionBtnText}
            </button>
          </div>
        </div>
      </div>
    `;

    // Elements
    const catalogEl = document.getElementById('dishes-catalog');
    const summaryItemsEl = document.getElementById('order-summary-items');
    const subtotalEl = document.getElementById('summary-subtotal');
    const igvEl = document.getElementById('summary-igv');
    const totalEl = document.getElementById('summary-total');
    const sendBtn = document.getElementById('btn-send-to-kitchen');

    // Render Dishes Catalog for a given category
    const renderCatalog = (category) => {
      const filteredDishes = dishes.filter(d => d.category === category);
      let catalogHTML = '<div class="dishes-grid">';
      
      filteredDishes.forEach(dish => {
        catalogHTML += `
          <div class="glass-card dish-item-card fade-in">
            <div>
              <div class="dish-item-name">${dish.name}</div>
              <p class="dish-item-desc">${dish.desc}</p>
            </div>
            <div class="dish-item-footer">
              <span class="dish-item-price">S/. ${dish.price.toFixed(2)}</span>
              <button class="btn btn-primary btn-sm btn-add-dish" data-dish-id="${dish.id}">+ Agregar</button>
            </div>
          </div>
        `;
      });

      catalogHTML += '</div>';
      catalogEl.innerHTML = catalogHTML;

      // Event listeners on add buttons
      catalogEl.querySelectorAll('.btn-add-dish').forEach(btn => {
        btn.addEventListener('click', () => {
          const dishId = btn.getAttribute('data-dish-id');
          const dish = dishes.find(d => d.id === dishId);
          this.addDishToOrder(dish);
          updateSummary();
        });
      });
    };

    // Update Summary List & Totals
    const updateSummary = () => {
      if (this.currentOrderItems.length === 0) {
        summaryItemsEl.innerHTML = `
          <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: var(--text-muted); text-align: center;">
            <span style="font-size: 2.5rem; margin-bottom: 10px;">🍽️</span>
            <p style="font-size: 0.85rem;">Pedido vacío.<br>Agrega platos del menú.</p>
          </div>
        `;
        subtotalEl.innerText = 'S/. 0.00';
        igvEl.innerText = 'S/. 0.00';
        totalEl.innerText = 'S/. 0.00';
        sendBtn.disabled = true;
        
        if (this.isModifying) {
          UIRenderer.setActiveUseCase('CU-08', 3);
        } else {
          UIRenderer.setActiveUseCase('CU-02', 3);
        }
        return;
      }

      // Populate Items
      summaryItemsEl.innerHTML = '';
      this.currentOrderItems.forEach(item => {
        const itemEl = document.createElement('div');
        itemEl.className = 'summary-item fade-in';
        itemEl.innerHTML = `
          <div class="summary-item-header">
            <span>${item.name}</span>
            <span>S/. ${(item.price * item.quantity).toFixed(2)}</span>
          </div>
          <div class="summary-item-controls">
            <div class="qty-controls">
              <button class="qty-btn btn-minus" data-dish-id="${item.id}">-</button>
              <span class="qty-val">${item.quantity}</span>
              <button class="qty-btn btn-plus" data-dish-id="${item.id}">+</button>
            </div>
            <span class="summary-item-total">S/. ${item.price.toFixed(2)} c/u</span>
          </div>
          <div>
            ${item.note ? `
              <div style="font-size: 0.72rem; color: #fbbf24; background: rgba(245,158,11,0.06); padding: 3px 6px; border-radius: 4px; display: inline-flex; align-items: center; gap: 4px;">
                <span>📝 Note:</span> "${item.note}" 
                <button style="background:none; border:none; color:var(--text-muted); cursor:pointer; font-weight:bold; margin-left:5px;" class="btn-clear-note" data-dish-id="${item.id}">&times;</button>
              </div>
            ` : `
              <button class="summary-item-note-btn btn-add-note" data-dish-id="${item.id}">
                ➕ Añadir nota de preparación...
              </button>
            `}
            <div class="note-input-container" style="display:none;" id="note-box-${item.id}">
              <input type="text" class="summary-item-note-input" placeholder="Ej: sin cebolla, bajo en sal..." value="${item.note || ''}" id="input-note-${item.id}">
              <div style="display: flex; gap: 6px; margin-top: 4px;">
                <button class="btn btn-secondary btn-sm btn-save-note" data-dish-id="${item.id}" style="padding: 2px 8px; font-size: 0.7rem;">OK</button>
                <button class="btn btn-danger btn-sm btn-cancel-note" data-dish-id="${item.id}" style="padding: 2px 8px; font-size: 0.7rem;">Cancelar</button>
              </div>
            </div>
          </div>
        `;
        summaryItemsEl.appendChild(itemEl);
      });

      // Recalculate Totals
      const totalAmount = this.currentOrderItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
      const subtotalAmount = totalAmount / 1.18;
      const igvAmount = totalAmount - subtotalAmount;

      subtotalEl.innerText = `S/. ${subtotalAmount.toFixed(2)}`;
      igvEl.innerText = `S/. ${igvAmount.toFixed(2)}`;
      totalEl.innerText = `S/. ${totalAmount.toFixed(2)}`;
      sendBtn.disabled = false;

      // Determine Step
      if (this.isModifying) {
        UIRenderer.setActiveUseCase('CU-08', 4);
      } else {
        UIRenderer.setActiveUseCase('CU-02', 4);
      }

      // Event listeners on Quantity controls
      summaryItemsEl.querySelectorAll('.btn-minus').forEach(btn => {
        btn.addEventListener('click', () => {
          this.decreaseDishQty(btn.getAttribute('data-dish-id'), container);
          updateSummary();
        });
      });

      summaryItemsEl.querySelectorAll('.btn-plus').forEach(btn => {
        btn.addEventListener('click', () => {
          const dish = dishes.find(d => d.id === btn.getAttribute('data-dish-id'));
          this.addDishToOrder(dish);
          updateSummary();
        });
      });

      // Note actions
      summaryItemsEl.querySelectorAll('.btn-add-note').forEach(btn => {
        btn.addEventListener('click', () => {
          const id = btn.getAttribute('data-dish-id');
          btn.style.display = 'none';
          const container = document.getElementById(`note-box-${id}`);
          container.style.display = 'block';
          const input = document.getElementById(`input-note-${id}`);
          input.focus();
          
          UIRenderer.setActiveUseCase('CU-02', 4);
          UIRenderer.logAction('Mesero editando notas para un plato', 'info');
        });
      });

      summaryItemsEl.querySelectorAll('.btn-save-note').forEach(btn => {
        btn.addEventListener('click', () => {
          const id = btn.getAttribute('data-dish-id');
          const input = document.getElementById(`input-note-${id}`);
          const noteText = input.value.trim();
          this.setDishNote(id, noteText);
          updateSummary();
          
          UIRenderer.logAction(`Nota guardada: "${noteText}"`, 'info');
        });
      });

      summaryItemsEl.querySelectorAll('.btn-cancel-note').forEach(btn => {
        btn.addEventListener('click', () => {
          updateSummary();
        });
      });

      summaryItemsEl.querySelectorAll('.btn-clear-note').forEach(btn => {
        btn.addEventListener('click', () => {
          const id = btn.getAttribute('data-dish-id');
          this.setDishNote(id, '');
          updateSummary();
        });
      });
    };

    // Render Initial Category
    renderCatalog(activeCategory);

    // Event listeners on Category Sidebar Tabs
    container.querySelectorAll('.category-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        container.querySelectorAll('.category-tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        activeCategory = btn.getAttribute('data-cat');
        renderCatalog(activeCategory);
      });
    });

    // Cancel order
    document.getElementById('btn-cancel-order').addEventListener('click', () => {
      if (this.isModifying) {
        UIRenderer.logAction(`Mesero canceló modificación del pedido para Mesa ${tableId}`, 'warn');
        this.isModifying = false;
        this.modifyingOrderId = null;
      } else {
        UIRenderer.logAction(`Mesero canceló toma de pedido para Mesa ${tableId}`, 'warn');
      }
      this.render(container, 'mesas');
    });

    // Confirm and Send order
    sendBtn.addEventListener('click', () => {
      const session = AppState.getCurrentSession();
      const waiterName = session ? session.name : 'Juan Pérez';
      const totalAmount = this.currentOrderItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
      const totalItems = this.currentOrderItems.reduce((a,b)=>a+b.quantity,0);

      if (isEditing) {
        UIRenderer.setActiveUseCase('CU-08', 6); // Reviewing modifications
        UIRenderer.showModal(
          'Confirmar Modificación del Pedido',
          `<p style="color:var(--text-secondary); margin-bottom:10px;">¿Estás seguro de guardar los cambios en esta orden?</p>
           <p style="color:var(--text-muted); font-size:0.75rem; margin-bottom:10px;">El pedido se re-enviará a la cocina en estado "Pendiente" con el listado de platos actualizado.</p>
           <div style="background:var(--bg-primary); padding:10px; border-radius:6px; border:1px solid var(--border-color); font-size:0.85rem;">
             <strong>Total de Platos:</strong> ${totalItems} platos.<br>
             <strong>Nuevo Total de Cuenta:</strong> S/. ${totalAmount.toFixed(2)}
           </div>`,
          [
            {
              text: 'Cancelar',
              class: 'btn-secondary',
              onClick: (e, modalRef) => modalRef.closeModal()
            },
            {
              text: '💾 Guardar Cambios',
              class: 'btn-primary',
              onClick: (e, modalRef) => {
                AppState.modifyOrder(this.modifyingOrderId, this.currentOrderItems);
                
                UIRenderer.logAction(`Pedido ${this.modifyingOrderId} (Mesa ${tableId}) ha sido actualizado y re-enviado a Cocina.`, 'success');
                
                // Clear state
                this.isModifying = false;
                this.modifyingOrderId = null;
                
                modalRef.closeModal();
                this.render(container, 'mesas');
              }
            }
          ]
        );
      } else {
        UIRenderer.setActiveUseCase('CU-02', 5); // Reviewing and ready to send
        UIRenderer.showModal(
          'Confirmar Envío del Pedido',
          `<p style="color:var(--text-secondary); margin-bottom:10px;">¿Estás seguro de enviar esta orden a la cocina?</p>
           <div style="background:var(--bg-primary); padding:10px; border-radius:6px; border:1px solid var(--border-color); font-size:0.85rem;">
             <strong>Total a Cocinar:</strong> ${totalItems} platos.<br>
             <strong>Monto Total:</strong> S/. ${totalAmount.toFixed(2)}
           </div>`,
          [
            {
              text: 'Cancelar',
              class: 'btn-secondary',
              onClick: (e, modalRef) => modalRef.closeModal()
            },
            {
              text: 'Sí, Enviar a Cocina',
              class: 'btn-primary',
              onClick: (e, modalRef) => {
                UIRenderer.setActiveUseCase('CU-02', 6);
                
                const newOrder = AppState.createOrder(tableId, this.currentOrderItems, waiterName);
                
                UIRenderer.logAction(`Pedido ${newOrder.id} enviado a Cocina para Mesa ${tableId}`, 'success');
                modalRef.closeModal();
                
                // Return to mesas grid
                this.render(container, 'mesas');
              }
            }
          ]
        );
      }
    });

    // Initial render call
    updateSummary();
  }

  // Adds a dish item to waitlist
  static addDishToOrder(dish) {
    const existing = this.currentOrderItems.find(item => item.id === dish.id);
    if (existing) {
      existing.quantity += 1;
    } else {
      this.currentOrderItems.push({
        id: dish.id,
        name: dish.name,
        price: dish.price,
        quantity: 1,
        note: ''
      });
    }
    UIRenderer.logAction(`Agregado: ${dish.name} (+1)`, 'info');
  }

  // Decreases item quantity with authorization interception if modifying active items
  static decreaseDishQty(dishId, container = null) {
    const existingIndex = this.currentOrderItems.findIndex(item => item.id === dishId);
    if (existingIndex === -1) return;

    const item = this.currentOrderItems[existingIndex];

    // Check if we are modifying an order that is already in preparation or served
    const order = this.isModifying ? AppState.getOrders().find(o => o.id === this.modifyingOrderId) : null;
    const isKitchenPreparing = order && order.status !== 'cocina_pendiente';

    // If it was in the original order, and we are trying to decrease below its original qty, and it's already cooking
    const originalItem = this.isModifying ? this.initialOrderItemsBackup.find(it => it.id === dishId) : null;
    const originalQty = originalItem ? originalItem.quantity : 0;

    if (isKitchenPreparing && originalQty > 0 && item.quantity <= originalQty) {
      // Check if already approved by admin
      if (!this.adminApprovedCancellations[dishId] || this.adminApprovedCancellations[dishId] < item.quantity) {
        // Intercept with admin authorization prompt (CU-08 Step 5)
        UIRenderer.setActiveUseCase('CU-08', 5);
        UIRenderer.logAction(`Clave de Administrador requerida para anular "${item.name}" (ya en preparación).`, 'warn');

        UIRenderer.showModal(
          '⚠️ Autorización de Administrador',
          `<p style="color:var(--text-secondary); margin-bottom:12px;">
            El plato <strong>${item.name}</strong> ya se encuentra en cocina (Estado: <em>${order.status}</em>).
          </p>
          <p style="color:var(--text-secondary); margin-bottom:15px;">
            Se requiere el código de supervisor para validar la anulación y registrar la merma en el sistema.
          </p>
          <div class="form-group">
            <label>Código de Administrador (Escriba: admin)</label>
            <input type="password" id="input-admin-auth-code" class="form-control" placeholder="••••">
          </div>`,
          [
            {
              text: 'Cancelar',
              class: 'btn-secondary',
              onClick: (e, modalRef) => {
                modalRef.closeModal();
                if (container) this.render(container, 'nuevo_pedido');
              }
            },
            {
              text: '🔑 Autorizar Anulación',
              class: 'btn-primary',
              onClick: (e, modalRef) => {
                const codeInput = document.getElementById('input-admin-auth-code');
                if (codeInput.value === 'admin') {
                  this.adminApprovedCancellations[dishId] = item.quantity;
                  UIRenderer.logAction(`Administrador autorizó anulación de "${item.name}" en Mesa ${this.activeTableId}`, 'success');
                  modalRef.closeModal();
                  
                  // Perform decrease
                  this.performDecrease(existingIndex);
                  
                  if (container) this.render(container, 'nuevo_pedido');
                } else {
                  alert('Código incorrecto. Ingrese "admin" para simular la aprobación.');
                }
              }
            }
          ]
        );
        return; // Halt decrease
      }
    }

    this.performDecrease(existingIndex);
  }

  static performDecrease(existingIndex) {
    const item = this.currentOrderItems[existingIndex];
    if (item.quantity > 1) {
      item.quantity -= 1;
      UIRenderer.logAction(`Quitado: ${item.name} (-1)`, 'info');
    } else {
      this.currentOrderItems.splice(existingIndex, 1);
      UIRenderer.logAction(`Eliminado del pedido: ${item.name}`, 'warn');
    }
  }

  // Sets customized text note on a dish
  static setDishNote(dishId, noteText) {
    const item = this.currentOrderItems.find(it => it.id === dishId);
    if (item) {
      item.note = noteText;
    }
  }

  // Opens details overlay of an active table
  static viewTableDetails(tableId) {
    const tables = AppState.getTables();
    const table = tables.find(t => t.id === tableId);
    const orders = AppState.getOrders();
    const order = orders.find(o => o.id === table.orderId);

    if (!order) return;

    let itemsHTML = '<div style="display:flex; flex-direction:column; gap:8px;">';
    order.items.forEach(it => {
      itemsHTML += `
        <div style="display:flex; justify-content:space-between; font-size:0.85rem; border-bottom:1px solid var(--border-color); padding-bottom:5px;">
          <div>
            <strong>${it.quantity}x</strong> ${it.name}
            ${it.note ? `<br><span style="color:#fbbf24; font-size:0.75rem; margin-left:10px;">📝 "${it.note}"</span>` : ''}
          </div>
          <span>S/. ${(it.price * it.quantity).toFixed(2)}</span>
        </div>
      `;
    });
    itemsHTML += '</div>';

    let orderStatusBadge = '';
    if (order.status === 'cocina_pendiente') orderStatusBadge = '<span class="badge badge-waiting">Pendiente en Cocina</span>';
    if (order.status === 'cocina_preparacion') orderStatusBadge = '<span class="badge badge-cooking">En Preparación</span>';
    if (order.status === 'cocina_listo') orderStatusBadge = '<span class="badge badge-ready">Listo para Servir</span>';
    if (order.status === 'entregado') orderStatusBadge = '<span class="badge badge-eating">Entregado al Cliente</span>';

    const modalBody = `
      <div style="display:flex; flex-direction:column; gap:15px; color:var(--text-secondary);">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span><strong>ID Pedido:</strong> ${order.id}</span>
          ${orderStatusBadge}
        </div>
        <div>
          <h4 style="color:white; margin-bottom:8px;">Consumo Detallado:</h4>
          ${itemsHTML}
        </div>
        <div style="display:flex; justify-content:space-between; border-top:1px dashed var(--border-color); padding-top:10px; color:white; font-size:1.1rem; font-weight:bold;">
          <span>Total de la Cuenta:</span>
          <span style="color:var(--accent);">S/. ${order.total.toFixed(2)}</span>
        </div>
      </div>
    `;

    const footerButtons = [
      {
        text: 'Cerrar',
        class: 'btn-secondary',
        onClick: (e, modalRef) => modalRef.closeModal()
      }
    ];

    // Permite al mesero modificar el pedido si no está cobrado
    if (order.status !== 'pagado') {
      footerButtons.push({
        text: '✏️ Modificar Pedido',
        class: 'btn-primary',
        onClick: (e, modalRef) => {
          modalRef.closeModal();
          const viewContainer = document.getElementById('main-viewport');
          this.startModifyOrder(tableId, order.id, viewContainer);
        }
      });
    }

    // Waiter can deliver the food if it's "Ready" (cocina_listo)
    if (order.status === 'cocina_listo') {
      footerButtons.push({
        text: '🍲 Entregar a Mesa',
        class: 'btn-success',
        onClick: (e, modalRef) => {
          AppState.updateOrderStatus(order.id, 'entregado');
          UIRenderer.logAction(`Mesero entregó pedido de la Mesa ${tableId} al cliente.`, 'success');
          modalRef.closeModal();
          
          // Re-render
          const viewContainer = document.getElementById('main-viewport');
          this.render(viewContainer, 'mesas');
        }
      });
    }

    UIRenderer.showModal(`Detalles: ${table.name}`, modalBody, footerButtons);
  }
}
