// Chef (Cocina) module logic for En su punto SAC

class RoleChef {
  static timerInterval = null;

  static render(container) {
    container.innerHTML = '';
    this.renderKitchenDashboard(container);
  }

  static renderKitchenDashboard(container) {
    // Set UC Track
    UIRenderer.setActiveUseCase('CU-03', 1);

    // Fetch orders that are pending or in preparation in kitchen
    const activeOrders = AppState.getOrders().filter(o => 
      o.status === 'cocina_pendiente' || o.status === 'cocina_preparacion'
    );

    let ticketsHTML = '';
    if (activeOrders.length === 0) {
      ticketsHTML = `
        <div style="grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 300px; color: var(--text-muted); text-align: center;" class="fade-in">
          <span style="font-size: 3.5rem; margin-bottom: 15px;">👨‍🍳</span>
          <h3 style="color: white; margin-bottom: 5px;">Cocina al Día</h3>
          <p style="font-size: 0.9rem;">No hay pedidos pendientes en la cola. ¡Buen trabajo!</p>
        </div>
      `;
    } else {
      ticketsHTML = '<div class="kitchen-grid">';
      activeOrders.forEach(order => {
        const isPreparing = order.status === 'cocina_preparacion';
        const ticketClass = isPreparing ? 'ticket-preparacion' : '';
        const badgeHTML = isPreparing 
          ? '<span class="badge badge-cooking">Preparando</span>' 
          : '<span class="badge badge-waiting">En Cola</span>';
        
        const actionBtnHTML = isPreparing 
          ? `<button class="btn btn-success btn-complete-ticket" data-order-id="${order.id}" style="width: 100%;">✅ Terminado (Listo)</button>`
          : `<button class="btn btn-primary btn-start-ticket" data-order-id="${order.id}" style="width: 100%;">👨‍🍳 Preparar</button>`;

        let itemsHTML = '';
        order.items.forEach(it => {
          itemsHTML += `
            <div class="ticket-item">
              <div class="ticket-item-row">
                <span class="ticket-item-qty">${it.quantity}</span>
                <span class="ticket-item-name">${it.name}</span>
              </div>
              ${it.note ? `<span class="ticket-item-note">⚠️ Note: "${it.note}"</span>` : ''}
            </div>
          `;
        });

        // Calculate time elapsed
        const elapsedSecs = Math.floor((Date.now() - new Date(order.timestamp).getTime()) / 1000);
        const formatTime = (totalSecs) => {
          const mins = Math.floor(totalSecs / 60);
          const secs = totalSecs % 60;
          return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        };

        ticketsHTML += `
          <div class="kitchen-ticket ${ticketClass} fade-in" id="ticket-${order.id}">
            <div class="kitchen-ticket-header">
              <div class="ticket-meta">
                <span class="ticket-table-name">${order.tableName}</span>
                <span class="ticket-id">Ped: ${order.id.split('-')[1] || order.id}</span>
              </div>
              <div style="display:flex; flex-direction:column; align-items:flex-end; gap:5px;">
                ${badgeHTML}
                <div class="ticket-timer" data-time-stamp="${order.timestamp}">${formatTime(elapsedSecs)}</div>
              </div>
            </div>
            <div class="kitchen-ticket-body">
              ${itemsHTML}
            </div>
            <div class="kitchen-ticket-footer">
              ${actionBtnHTML}
            </div>
          </div>
        `;
      });
      ticketsHTML += '</div>';
    }

    container.innerHTML = `
      <div class="fade-in">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px;">
          <div>
            <h1 style="font-size: 1.8rem; color: white;">Monitor de Cocina</h1>
            <p style="color: var(--text-secondary); font-size: 0.9rem;">Visualiza y gestiona la cola de preparación del restaurante.</p>
          </div>
          <button class="btn btn-secondary" id="btn-refresh-kitchen" style="padding: 8px 14px;">🔄 Refrescar</button>
        </div>
        ${ticketsHTML}
      </div>
    `;

    // Hook events
    container.querySelectorAll('.btn-start-ticket').forEach(btn => {
      btn.addEventListener('click', () => {
        const orderId = btn.getAttribute('data-order-id');
        const order = activeOrders.find(o => o.id === orderId);
        
        UIRenderer.setActiveUseCase('CU-03', 2);
        
        // Update to preparation
        AppState.updateOrderStatus(orderId, 'cocina_preparacion');
        
        UIRenderer.setActiveUseCase('CU-03', 3);
        UIRenderer.logAction(`Chef inició preparación de Mesa ${order.tableId}`, 'action');
        
        // Re-render
        this.render(container);
      });
    });

    container.querySelectorAll('.btn-complete-ticket').forEach(btn => {
      btn.addEventListener('click', () => {
        const orderId = btn.getAttribute('data-order-id');
        const order = activeOrders.find(o => o.id === orderId);

        UIRenderer.setActiveUseCase('CU-03', 4);
        
        // Update to list/ready
        AppState.updateOrderStatus(orderId, 'cocina_listo');
        
        UIRenderer.logAction(`Chef marcó Mesa ${order.tableId} como LISTO PARA SERVIR`, 'success');
        
        // Re-render
        this.render(container);
      });
    });

    document.getElementById('btn-refresh-kitchen').addEventListener('click', () => {
      this.render(container);
    });

    // Start ticks
    this.startTimers();
  }

  static startTimers() {
    this.stopTimers();
    
    this.timerInterval = setInterval(() => {
      document.querySelectorAll('.ticket-timer').forEach(timerEl => {
        const timestamp = timerEl.getAttribute('data-time-stamp');
        if (timestamp) {
          const elapsedSecs = Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000);
          const mins = Math.floor(elapsedSecs / 60);
          const secs = elapsedSecs % 60;
          timerEl.innerText = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
          
          // Visual indicator if order takes > 10 minutes (amber) or > 15 minutes (red)
          if (elapsedSecs > 900) {
            timerEl.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
            timerEl.style.color = '#ef4444';
            timerEl.style.borderColor = '#ef4444';
          } else if (elapsedSecs > 600) {
            timerEl.style.backgroundColor = 'rgba(245, 158, 11, 0.15)';
            timerEl.style.color = '#f59e0b';
            timerEl.style.borderColor = '#f59e0b';
          }
        }
      });
    }, 1000);
  }

  static stopTimers() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }
}
