// Cashier (Cajero) module logic for En su punto SAC

class RoleCajero {
  static selectedOrderId = null;
  static selectedPaymentMethod = null; // 'efectivo', 'tarjeta', 'yape'
  static isSplitting = false;
  static splitPartsCount = 2;
  static splitParts = [];
  static activeSplitPartIndex = null;
  static activeSplitPaymentMethod = null;

  static render(container) {
    container.innerHTML = '';
    this.renderCashierLayout(container);
  }

  static renderCashierLayout(container) {
    // Set UC Step 1
    UIRenderer.setActiveUseCase('CU-04', 1);

    const tables = AppState.getTables();
    const orders = AppState.getOrders();
    const activeOrders = AppState.getActiveOrders();

    // 1. Left Side: Active Tables Grid
    let tablesHTML = '<div class="tables-grid">';
    tables.forEach(table => {
      let isSelectable = table.status !== 'libre';
      let cardClass = isSelectable ? 'table-card' : 'table-card state-libre';
      let badgeClass = 'badge-free';
      let badgeLabel = 'Libre';
      let activeOrder = null;

      if (isSelectable) {
        activeOrder = orders.find(o => o.id === table.orderId);
        
        if (table.status === 'esperando_comida') {
          badgeClass = 'badge-waiting';
          badgeLabel = 'En Cola';
          cardClass += ' state-esperando_comida';
        } else if (table.status === 'cocina_preparacion') {
          badgeClass = 'badge-cooking';
          badgeLabel = 'Preparando';
          cardClass += ' state-cocina_preparacion';
        } else if (table.status === 'comiendo') {
          badgeClass = 'badge-eating';
          badgeLabel = 'Consumiendo';
          cardClass += ' state-comiendo';
        } else if (table.status === 'cuenta_pedida') {
          badgeClass = 'badge-ready';
          badgeLabel = 'Por Cobrar';
          cardClass += ' state-cuenta_pedida';
          // Add a subtle animation/glow for ready to check out
          cardClass += ' fade-in';
        }
      }

      const totalVal = activeOrder ? `S/. ${activeOrder.total.toFixed(2)}` : 'S/. 0.00';
      const isSelected = activeOrder && activeOrder.id === this.selectedOrderId;
      const selectedBorder = isSelected ? 'border-color: var(--accent); box-shadow: 0 0 15px rgba(255,102,0,0.15);' : '';

      tablesHTML += `
        <div class="glass-card ${cardClass}" style="${selectedBorder} cursor: ${isSelectable ? 'pointer' : 'not-allowed'};" data-cashier-table-id="${table.id}" data-selectable="${isSelectable}">
          <div class="table-header">
            <span class="table-number">${table.name}</span>
            <span class="badge ${badgeClass}">${badgeLabel}</span>
          </div>
          <div class="table-body-info">
            ${isSelectable && activeOrder ? `
              <div class="table-order-total" style="font-size: 1.25rem;">${totalVal}</div>
              <p style="font-size: 0.75rem; color: var(--text-muted); margin-top: 5px;">
                Cuentas por pagar.<br>
                Mesero: ${activeOrder.waiterName}
              </p>
            ` : `
              <p style="font-size: 0.75rem; color: var(--text-muted);">Sin cuenta activa.</p>
            `}
          </div>
        </div>
      `;
    });
    tablesHTML += '</div>';

    // 2. Right Side: Bill Details & Checkout Panel
    let billPanelHTML = '';
    const selectedOrder = activeOrders.find(o => o.id === this.selectedOrderId);

    if (!selectedOrder) {
      this.isSplitting = false;
      billPanelHTML = `
        <div class="glass-card cashier-bill-panel" style="justify-content: center; align-items: center; color: var(--text-muted); text-align: center; height: 100%;">
          <span style="font-size: 3.5rem; margin-bottom: 15px;">💰</span>
          <h3 style="color: white; margin-bottom: 5px;">Módulo de Caja</h3>
          <p style="font-size: 0.85rem;">Selecciona una mesa ocupada a la izquierda para procesar el cobro e imprimir el ticket.</p>
        </div>
      `;
    } else if (this.isSplitting) {
      const splitsConfigured = this.splitParts.length > 0;
      const paymentMethodSelected = this.activeSplitPaymentMethod !== null;
      UIRenderer.setActiveUseCase('CU-09', this.activeSplitPartIndex !== null ? (paymentMethodSelected ? 5 : 4) : (splitsConfigured ? 3 : 2));

      const totalVal = selectedOrder.total;

      if (this.splitParts.length === 0) {
        billPanelHTML = `
          <div class="glass-card cashier-bill-panel" style="height: 100%;">
            <div style="border-bottom: 1px solid var(--border-color); padding-bottom: 12px; margin-bottom: 15px;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <h3 style="font-size: 1.2rem; color: white;">Dividir Cuenta: ${selectedOrder.tableName}</h3>
                <button class="btn btn-secondary btn-sm" id="btn-cancel-split" style="padding: 2px 8px;">Volver</button>
              </div>
              <p style="font-size: 0.75rem; color: var(--text-muted);">Configure la cantidad de personas para la fracción.</p>
            </div>
            
            <div style="flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 20px;">
              <div style="text-align: center;">
                <div style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 5px;">Total Original de Cuenta</div>
                <div style="font-size: 1.8rem; font-weight: bold; color: var(--accent);">S/. ${totalVal.toFixed(2)}</div>
              </div>

              <div class="form-group" style="text-align: center;">
                <label>Número de Partes (Personas)</label>
                <div style="display: flex; justify-content: center; align-items: center; gap: 15px; margin-top: 10px;">
                  <button class="qty-btn" id="btn-split-dec" style="width: 36px; height: 36px; font-size: 1.2rem;">-</button>
                  <span style="font-size: 1.6rem; font-weight: 800; width: 40px; text-align: center;" id="lbl-split-count">${this.splitPartsCount}</span>
                  <button class="qty-btn" id="btn-split-inc" style="width: 36px; height: 36px; font-size: 1.2rem;">+</button>
                </div>
              </div>

              <div style="background: rgba(255,255,255,0.02); border: 1px dashed var(--border-color); border-radius: 8px; padding: 12px; text-align: center;">
                <span style="font-size: 0.8rem; color: var(--text-secondary);">Cada persona pagará:</span>
                <div style="font-size: 1.2rem; font-weight: 700; color: white;" id="lbl-split-fraction">S/. ${(totalVal / this.splitPartsCount).toFixed(2)}</div>
              </div>
            </div>

            <button class="btn btn-primary" id="btn-confirm-split" style="width: 100%; padding: 12px; margin-top: auto;">
              🥞 Confirmar División
            </button>
          </div>
        `;
      } else {
        let listHTML = '<div style="display: flex; flex-direction: column; gap: 12px; flex: 1; overflow-y: auto; margin-bottom: 15px; padding-right: 4px;">';
        
        this.splitParts.forEach((part, idx) => {
          const isSelected = this.activeSplitPartIndex === idx;
          const selectedGlow = isSelected ? 'border-color: var(--accent); background: rgba(255,102,0,0.05);' : '';
          
          let actionHTML = '';
          if (part.paid) {
            actionHTML = `<span class="badge badge-free" style="font-size:0.7rem; text-transform:none; padding:4px 8px;">Pagado (${part.method.toUpperCase()})</span>`;
          } else if (isSelected) {
            actionHTML = `<span style="font-size: 0.72rem; color: var(--accent); font-weight: bold;">Cobrando...</span>`;
          } else {
            actionHTML = `<button class="btn btn-primary btn-sm btn-pay-part" data-part-idx="${idx}" style="font-size: 0.72rem; padding: 4px 8px;">Cobrar</button>`;
          }

          listHTML += `
            <div class="summary-item" style="flex-direction: row; justify-content: space-between; align-items: center; padding: 12px; ${selectedGlow}">
              <div>
                <strong style="color: white; font-size: 0.9rem;">Cliente ${idx + 1}</strong>
                <p style="font-size: 0.75rem; color: var(--text-muted);">${part.billNum ? `Boleta: ${part.billNum}` : `Monto: S/. ${part.amount.toFixed(2)}`}</p>
              </div>
              <div>
                ${actionHTML}
              </div>
            </div>
          `;
        });
        listHTML += '</div>';

        const totalPaid = this.splitParts.filter(p => p.paid).reduce((sum, p) => sum + p.amount, 0);
        const remaining = totalVal - totalPaid;

        let splitPaymentHTML = '';
        if (this.activeSplitPartIndex !== null && !this.splitParts[this.activeSplitPartIndex].paid) {
          const partAmount = this.splitParts[this.activeSplitPartIndex].amount;
          const isEfectivo = this.activeSplitPaymentMethod === 'efectivo';
          const isTarjeta = this.activeSplitPaymentMethod === 'tarjeta';
          const isYape = this.activeSplitPaymentMethod === 'yape';

          splitPaymentHTML = `
            <div style="border-top: 1px solid var(--border-color); padding-top: 15px; margin-top: 10px;" class="fade-in">
              <h4 style="font-size: 0.85rem; color: white; margin-bottom: 8px;">Método de Pago para Cliente ${this.activeSplitPartIndex + 1}:</h4>
              
              <div class="payment-method-selector" style="margin-bottom: 10px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px;">
                <button class="payment-method-btn ${isEfectivo ? 'active' : ''}" id="btn-split-pm-efectivo" style="padding: 8px; font-size: 0.75rem;">
                  <span style="font-size:1rem;">💵</span>
                  <span>Efectivo</span>
                </button>
                <button class="payment-method-btn ${isTarjeta ? 'active' : ''}" id="btn-split-pm-tarjeta" style="padding: 8px; font-size: 0.75rem;">
                  <span style="font-size:1rem;">💳</span>
                  <span>Tarjeta</span>
                </button>
                <button class="payment-method-btn ${isYape ? 'active' : ''}" id="btn-split-pm-yape" style="padding: 8px; font-size: 0.75rem;">
                  <span style="font-size:1rem;">📱</span>
                  <span style="font-size:0.7rem;">Yape/Plin</span>
                </button>
              </div>

              ${isEfectivo ? `
                <div class="cash-calculator fade-in" style="margin-bottom: 10px; padding: 10px;">
                  <div class="form-group" style="margin-bottom: 5px;">
                    <label style="font-size: 0.75rem; margin-bottom: 4px;">Recibido S/.</label>
                    <input type="number" id="input-split-cash" class="form-control" style="padding: 8px 12px; font-size: 0.85rem;" placeholder="0.00" min="${partAmount}">
                  </div>
                  <div style="display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: bold;">
                    <span style="color: var(--text-secondary);">Vuelto:</span>
                    <span style="color: var(--status-free);" id="lbl-split-change">S/. 0.00</span>
                  </div>
                </div>
              ` : ''}

              <div style="display: flex; gap: 8px;">
                <button class="btn btn-secondary btn-sm" id="btn-split-pm-cancel" style="flex:1; padding: 8px;">Descartar</button>
                <button class="btn btn-primary btn-sm" id="btn-split-pm-confirm" style="flex:1.5; padding: 8px;" ${!this.activeSplitPaymentMethod ? 'disabled' : ''}>
                  💰 Cobrar Parte
                </button>
              </div>
            </div>
          `;
        }

        billPanelHTML = `
          <div class="glass-card cashier-bill-panel" style="height: 100%;">
            <div style="border-bottom: 1px solid var(--border-color); padding-bottom: 12px; margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
              <div>
                <h3 style="font-size: 1.2rem; color: white;">Cobro Fraccionado</h3>
                <p style="font-size: 0.75rem; color: var(--text-muted);">${selectedOrder.tableName} (Total: S/. ${totalVal.toFixed(2)})</p>
              </div>
              <button class="btn btn-danger btn-sm" id="btn-reset-split-list" style="padding: 2px 8px;">Restablecer</button>
            </div>

            <!-- Splits List -->
            ${listHTML}

            <!-- Total boxes -->
            <div class="summary-totals-box" style="margin-bottom: 10px; padding: 12px;">
              <div style="display: flex; justify-content: space-between; font-size: 0.78rem; color: var(--text-secondary);">
                <span>Cobrado hasta ahora</span>
                <span>S/. ${totalPaid.toFixed(2)}</span>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: bold; color: white; margin-top: 5px; padding-top: 5px; border-top: 1px dashed var(--border-color);">
                <span>Saldo Pendiente</span>
                <span style="color: ${remaining > 0 ? 'var(--status-waiting)' : 'var(--status-free)'};">S/. ${remaining.toFixed(2)}</span>
              </div>
            </div>

            <!-- Payment widget for active slice -->
            ${splitPaymentHTML}
          </div>
        `;
      }
    } else {
      let itemsListHTML = '';
      selectedOrder.items.forEach(it => {
        itemsListHTML += `
          <div style="display: flex; justify-content: space-between; font-size: 0.85rem; border-bottom: 1px solid var(--border-color); padding-bottom: 8px;">
            <div>
              <strong>${it.quantity}x</strong> ${it.name}
              ${it.note ? `<br><span style="color:#fbbf24; font-size:0.75rem;">📝 "${it.note}"</span>` : ''}
            </div>
            <span style="color: white;">S/. ${(it.price * it.quantity).toFixed(2)}</span>
          </div>
        `;
      });

      const totalVal = selectedOrder.total;
      const subtotalVal = totalVal / 1.18;
      const igvVal = totalVal - subtotalVal;

      const isEfectivo = this.selectedPaymentMethod === 'efectivo';
      const isTarjeta = this.selectedPaymentMethod === 'tarjeta';
      const isYape = this.selectedPaymentMethod === 'yape';

      billPanelHTML = `
        <div class="glass-card cashier-bill-panel" style="height: 100%;">
          <div style="border-bottom: 1px solid var(--border-color); padding-bottom: 12px; margin-bottom: 15px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <h3 style="font-size: 1.2rem; color: white;">Cuenta: ${selectedOrder.tableName}</h3>
              <button class="btn btn-secondary btn-sm" id="btn-deselect-table" style="padding: 2px 8px;">Deseleccionar</button>
            </div>
            <span style="font-size: 0.72rem; color: var(--text-muted); font-family: monospace;">Pedido: ${selectedOrder.id}</span>
          </div>

          <!-- Items Scroll List -->
          <div class="summary-items-list" style="margin: 0 0 15px 0;">
            ${itemsListHTML}
          </div>

          <!-- Summary Box -->
          <div class="summary-totals-box" style="margin-bottom: 15px;">
            <div class="summary-totals-row">
              <span>Subtotal afecto</span>
              <span>S/. ${subtotalVal.toFixed(2)}</span>
            </div>
            <div class="summary-totals-row">
              <span>I.G.V. (18%)</span>
              <span>S/. ${igvVal.toFixed(2)}</span>
            </div>
            <div class="summary-totals-row grand-total" style="font-size: 1.3rem;">
              <span>Total a Cobrar</span>
              <span style="color: var(--accent);">S/. ${totalVal.toFixed(2)}</span>
            </div>
          </div>

          <!-- Payment Methods Selector -->
          <div class="payment-method-selector">
            <button class="payment-method-btn ${isEfectivo ? 'active' : ''}" data-method="efectivo">
              <span class="method-icon">💵</span>
              <span>Efectivo</span>
            </button>
            <button class="payment-method-btn ${isTarjeta ? 'active' : ''}" data-method="tarjeta">
              <span class="method-icon">💳</span>
              <span>Tarjeta</span>
            </button>
            <button class="payment-method-btn ${isYape ? 'active' : ''}" data-method="yape">
              <span class="method-icon">📱</span>
              <span>Yape / Plin</span>
            </button>
          </div>

          <!-- Cash Received Calculator -->
          ${isEfectivo ? `
            <div class="cash-calculator fade-in">
              <div class="form-group" style="margin-bottom: 10px;">
                <label>Efectivo Recibido (S/.)</label>
                <input type="number" id="input-cash-received" class="form-control" placeholder="0.00" step="0.10" min="${totalVal}">
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; font-weight: bold; font-size: 0.95rem;">
                <span style="color: var(--text-secondary);">Vuelto a Entregar:</span>
                <span style="color: var(--status-free); font-size: 1.15rem;" id="lbl-change-due">S/. 0.00</span>
              </div>
            </div>
          ` : ''}

          <!-- Action Buttons -->
          <div style="display: flex; flex-direction: column; gap: 8px; margin-top: auto;">
            <button class="btn btn-primary" id="btn-complete-checkout" style="width: 100%; padding: 12px;" ${!this.selectedPaymentMethod ? 'disabled' : ''}>
              💰 Registrar Pago Completo
            </button>
            <button class="btn btn-secondary" id="btn-split-account-trigger" style="width: 100%; padding: 10px; border-style: dashed;">
              🥞 Dividir Cuenta (Pagos Parciales)
            </button>
          </div>
        </div>
      `;
    }

    container.innerHTML = `
      <div class="fade-in" style="height: 100%;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px;">
          <div>
            <h1 style="font-size: 1.8rem; color: white;">Módulo de Caja</h1>
            <p style="color: var(--text-secondary); font-size: 0.9rem;">Registra los pagos y libera las mesas atendidas.</p>
          </div>
        </div>

        <div class="cashier-layout">
          <div class="cashier-tables-view">
            ${tablesHTML}
          </div>
          <div id="checkout-panel-viewport">
            ${billPanelHTML}
          </div>
        </div>
      </div>
    `;

    // Hook events for Left Table click
    container.querySelectorAll('[data-cashier-table-id]').forEach(card => {
      card.addEventListener('click', () => {
        const isSelectable = card.getAttribute('data-selectable') === 'true';
        if (!isSelectable) return;
        
        const tableId = Number(card.getAttribute('data-cashier-table-id'));
        const table = tables.find(t => t.id === tableId);
        
        this.selectedOrderId = table.orderId;
        this.selectedPaymentMethod = null; // reset payment method on change
        this.isSplitting = false; // reset split state on table change
        this.splitParts = [];
        
        UIRenderer.logAction(`Cajero seleccionó ${table.name} para facturación`, 'action');
        
        // Re-render
        this.render(container);
      });
    });

    // Deselect table
    const deselectBtn = document.getElementById('btn-deselect-table');
    if (deselectBtn) {
      deselectBtn.addEventListener('click', () => {
        this.selectedOrderId = null;
        this.selectedPaymentMethod = null;
        this.isSplitting = false;
        this.splitParts = [];
        this.render(container);
      });
    }

    // Payment methods buttons
    container.querySelectorAll('.payment-method-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const method = btn.getAttribute('data-method');
        this.selectedPaymentMethod = method;
        
        UIRenderer.logAction(`Cajero seleccionó método de pago: ${method.toUpperCase()}`, 'info');
        
        // Re-render checkout panel parts
        this.render(container);
      });
    });

    // Cash received handler
    const cashInput = document.getElementById('input-cash-received');
    if (cashInput) {
      cashInput.focus();
      cashInput.addEventListener('input', () => {
        const received = parseFloat(cashInput.value) || 0;
        const total = selectedOrder.total;
        const changeDue = Math.max(0, received - total);
        document.getElementById('lbl-change-due').innerText = `S/. ${changeDue.toFixed(2)}`;
      });
    }

    // Confirm Payment
    const checkoutBtn = document.getElementById('btn-complete-checkout');
    if (checkoutBtn) {
      checkoutBtn.addEventListener('click', () => {
        const total = selectedOrder.total;
        let changeText = 'S/. 0.00';
        let cashReceived = total;

        if (this.selectedPaymentMethod === 'efectivo') {
          const rawReceived = parseFloat(cashInput.value);
          if (isNaN(rawReceived) || rawReceived < total) {
            alert('El monto recibido no es suficiente para cubrir la cuenta.');
            return;
          }
          cashReceived = rawReceived;
          changeText = `S/. ${(rawReceived - total).toFixed(2)}`;
        }

        // Trigger Receipt Modal (UML Step 6)
        this.showReceiptModal(selectedOrder, cashReceived, changeText, container);
      });
    }

    // --- SPLIT ACCOUNT BINDINGS (CU-09) ---
    const btnSplitAccountTrigger = document.getElementById('btn-split-account-trigger');
    if (btnSplitAccountTrigger) {
      btnSplitAccountTrigger.addEventListener('click', () => {
        this.isSplitting = true;
        this.splitParts = [];
        this.splitPartsCount = 2;
        UIRenderer.logAction(`Cajero inició división de cuenta para ${selectedOrder.tableName}`, 'action');
        this.render(container);
      });
    }

    const btnCancelSplit = document.getElementById('btn-cancel-split');
    if (btnCancelSplit) {
      btnCancelSplit.addEventListener('click', () => {
        this.isSplitting = false;
        this.splitParts = [];
        this.render(container);
      });
    }

    const btnSplitDec = document.getElementById('btn-split-dec');
    if (btnSplitDec) {
      btnSplitDec.addEventListener('click', () => {
        this.splitPartsCount = Math.max(2, this.splitPartsCount - 1);
        this.render(container);
      });
    }

    const btnSplitInc = document.getElementById('btn-split-inc');
    if (btnSplitInc) {
      btnSplitInc.addEventListener('click', () => {
        this.splitPartsCount = Math.min(8, this.splitPartsCount + 1);
        this.render(container);
      });
    }

    const btnConfirmSplit = document.getElementById('btn-confirm-split');
    if (btnConfirmSplit) {
      btnConfirmSplit.addEventListener('click', () => {
        const amt = selectedOrder.total / this.splitPartsCount;
        this.splitParts = Array.from({ length: this.splitPartsCount }, (_, i) => ({
          id: i,
          amount: amt,
          paid: false,
          method: null,
          billNum: null
        }));
        this.activeSplitPartIndex = null;
        this.activeSplitPaymentMethod = null;
        UIRenderer.logAction(`Cuenta dividida en ${this.splitPartsCount} partes de S/. ${amt.toFixed(2)} cada una.`, 'info');
        this.render(container);
      });
    }

    const btnResetSplitList = document.getElementById('btn-reset-split-list');
    if (btnResetSplitList) {
      btnResetSplitList.addEventListener('click', () => {
        this.splitParts = [];
        this.activeSplitPartIndex = null;
        this.activeSplitPaymentMethod = null;
        UIRenderer.logAction('Re-configurando división de cuenta.', 'warn');
        this.render(container);
      });
    }

    // Select part to pay
    container.querySelectorAll('.btn-pay-part').forEach(btn => {
      btn.addEventListener('click', () => {
        this.activeSplitPartIndex = Number(btn.getAttribute('data-part-idx'));
        this.activeSplitPaymentMethod = null;
        this.render(container);
      });
    });

    // Split Payment Method Selector
    const btnSplitPmEfectivo = document.getElementById('btn-split-pm-efectivo');
    const btnSplitPmTarjeta = document.getElementById('btn-split-pm-tarjeta');
    const btnSplitPmYape = document.getElementById('btn-split-pm-yape');

    if (btnSplitPmEfectivo) {
      btnSplitPmEfectivo.addEventListener('click', () => {
        this.activeSplitPaymentMethod = 'efectivo';
        this.render(container);
      });
    }
    if (btnSplitPmTarjeta) {
      btnSplitPmTarjeta.addEventListener('click', () => {
        this.activeSplitPaymentMethod = 'tarjeta';
        this.render(container);
      });
    }
    if (btnSplitPmYape) {
      btnSplitPmYape.addEventListener('click', () => {
        this.activeSplitPaymentMethod = 'yape';
        this.render(container);
      });
    }

    const btnSplitPmCancel = document.getElementById('btn-split-pm-cancel');
    if (btnSplitPmCancel) {
      btnSplitPmCancel.addEventListener('click', () => {
        this.activeSplitPartIndex = null;
        this.activeSplitPaymentMethod = null;
        this.render(container);
      });
    }

    const splitCashInput = document.getElementById('input-split-cash');
    if (splitCashInput) {
      splitCashInput.focus();
      splitCashInput.addEventListener('input', () => {
        const received = parseFloat(splitCashInput.value) || 0;
        const partAmt = this.splitParts[this.activeSplitPartIndex].amount;
        const change = Math.max(0, received - partAmt);
        document.getElementById('lbl-split-change').innerText = `S/. ${change.toFixed(2)}`;
      });
    }

    const btnSplitPmConfirm = document.getElementById('btn-split-pm-confirm');
    if (btnSplitPmConfirm) {
      btnSplitPmConfirm.addEventListener('click', () => {
        const part = this.splitParts[this.activeSplitPartIndex];
        let changeText = 'S/. 0.00';
        let cashReceived = part.amount;

        if (this.activeSplitPaymentMethod === 'efectivo') {
          const rawReceived = parseFloat(splitCashInput.value);
          if (isNaN(rawReceived) || rawReceived < part.amount) {
            alert('El monto recibido no cubre esta fracción.');
            return;
          }
          cashReceived = rawReceived;
          changeText = `S/. ${(rawReceived - part.amount).toFixed(2)}`;
        }

        // Show Fraccionated receipt
        this.showSplitReceiptModal(selectedOrder, this.activeSplitPartIndex, cashReceived, changeText, container);
      });
    }
  }

  // Opens simulated receipt window
  static showReceiptModal(order, cashReceived, changeText, mainContainer) {
    UIRenderer.setActiveUseCase('CU-04', 6);

    const now = new Date();
    const dateStr = now.toLocaleDateString('es-PE');
    const timeStr = now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    const subtotal = order.total / 1.18;
    const igv = order.total - subtotal;
    const boletaNum = `B001-${Math.floor(100000 + Math.random() * 900000)}`;

    let itemsLinesHTML = '';
    order.items.forEach(it => {
      const lineTotal = (it.price * it.quantity).toFixed(2);
      itemsLinesHTML += `
        <div class="receipt-line">
          <span>${it.quantity} x ${it.name.substring(0, 18)}</span>
          <span>S/. ${lineTotal}</span>
        </div>
      `;
    });

    const receiptBodyHTML = `
      <div class="receipt-wrapper fade-in">
        <div class="receipt-header">
          <div class="receipt-title">EN SU PUNTO SAC</div>
          <div style="font-size: 0.72rem; color: #4b5563; margin-top: 4px;">R.U.C. 20601234567</div>
          <div style="font-size: 0.65rem; color: #6b7280;">Av. La Marina 1500 - San Miguel</div>
        </div>
        
        <div class="receipt-body">
          <div class="receipt-line"><strong>BOLETA DE VENTA:</strong> <strong>${boletaNum}</strong></div>
          <div class="receipt-line">Fecha: ${dateStr}</div>
          <div class="receipt-line">Hora: ${timeStr}</div>
          <div class="receipt-line">Mesa: ${order.tableName}</div>
          <div class="receipt-line">Mesero: ${order.waiterName}</div>
          
          <div class="receipt-divider"></div>
          
          ${itemsLinesHTML}
          
          <div class="receipt-divider"></div>
          
          <div class="receipt-line">Subtotal (Op. Gravada): S/. ${subtotal.toFixed(2)}</div>
          <div class="receipt-line">I.G.V. (18.00%): S/. ${igv.toFixed(2)}</div>
          
          <div class="receipt-divider"></div>
          
          <div class="receipt-total-row">
            <span>TOTAL:</span>
            <span>S/. ${order.total.toFixed(2)}</span>
          </div>
          
          <div class="receipt-divider"></div>
          
          <div class="receipt-line">Pago: ${this.selectedPaymentMethod.toUpperCase()}</div>
          <div class="receipt-line">Recibido: S/. ${parseFloat(cashReceived).toFixed(2)}</div>
          <div class="receipt-line">Vuelto: ${changeText}</div>
        </div>
        
        <div class="receipt-footer">
          <div>¡Gracias por su preferencia!</div>
          <div style="font-size: 0.65rem; margin-top: 5px; color:#4b5563;">Representación impresa de boleta de venta mockup para análisis de Casos de Uso.</div>
        </div>
      </div>
    `;

    const footerButtons = [
      {
        text: '❌ Cerrar',
        class: 'btn-secondary',
        onClick: (e, modalRef) => modalRef.closeModal()
      },
      {
        text: '🖨️ Registrar Pago y Cerrar',
        class: 'btn-primary',
        onClick: (e, modalRef) => {
          // Process state payment
          AppState.payOrder(order.id, this.selectedPaymentMethod);
          
          UIRenderer.logAction(`Pago registrado para Mesa ${order.tableId} en ${this.selectedPaymentMethod.toUpperCase()}. Venta procesada.`, 'success');
          
          // Clear cashier selection
          this.selectedOrderId = null;
          this.selectedPaymentMethod = null;
          
          // Close modal
          modalRef.closeModal();

          // Re-render
          this.render(mainContainer);
        }
      }
    ];

    UIRenderer.showModal('Comprobante de Pago Emitido (Simulación)', receiptBodyHTML, footerButtons);
  }

  // Opens simulated receipt window for fraccionated payments
  static showSplitReceiptModal(order, partIndex, cashReceived, changeText, mainContainer) {
    const part = this.splitParts[partIndex];
    const now = new Date();
    const dateStr = now.toLocaleDateString('es-PE');
    const timeStr = now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    const subtotal = part.amount / 1.18;
    const igv = part.amount - subtotal;
    const boletaNum = `F001-${Math.floor(100000 + Math.random() * 900000)}`;

    const receiptBodyHTML = `
      <div class="receipt-wrapper fade-in">
        <div class="receipt-header">
          <div class="receipt-title">EN SU PUNTO SAC</div>
          <div style="font-size: 0.72rem; color: #4b5563; margin-top: 4px;">R.U.C. 20601234567</div>
          <div style="font-size: 0.65rem; color: #6b7280;">Av. La Marina 1500 - San Miguel</div>
        </div>
        
        <div class="receipt-body">
          <div class="receipt-line"><strong>PAGO PARCIAL (FACT. FRACCIONADA)</strong></div>
          <div class="receipt-line"><strong>BOLETA DE VENTA:</strong> <strong>${boletaNum}</strong></div>
          <div class="receipt-divider"></div>
          <div class="receipt-line">Fecha: ${dateStr}</div>
          <div class="receipt-line">Hora: ${timeStr}</div>
          <div class="receipt-line">Mesa: ${order.tableName}</div>
          <div class="receipt-line">Mesero: ${order.waiterName}</div>
          <div class="receipt-line"><strong>Pago: Cliente ${partIndex + 1} de ${this.splitParts.length}</strong></div>
          
          <div class="receipt-divider"></div>
          
          <div class="receipt-line">
            <span>1 x Pago Fraccionado Consumo</span>
            <span>S/. ${part.amount.toFixed(2)}</span>
          </div>
          
          <div class="receipt-divider"></div>
          
          <div class="receipt-line">Subtotal (Op. Gravada): S/. ${subtotal.toFixed(2)}</div>
          <div class="receipt-line">I.G.V. (18.00%): S/. ${igv.toFixed(2)}</div>
          
          <div class="receipt-divider"></div>
          
          <div class="receipt-total-row">
            <span>TOTAL FRACCIÓN:</span>
            <span>S/. ${part.amount.toFixed(2)}</span>
          </div>
          
          <div class="receipt-divider"></div>
          
          <div class="receipt-line">Pago: ${this.activeSplitPaymentMethod.toUpperCase()}</div>
          <div class="receipt-line">Recibido: S/. ${parseFloat(cashReceived).toFixed(2)}</div>
          <div class="receipt-line">Vuelto: ${changeText}</div>
        </div>
        
        <div class="receipt-footer">
          <div>¡Gracias por su preferencia!</div>
          <div style="font-size: 0.65rem; margin-top: 5px; color:#4b5563;">Representación de boleta de venta fraccionada mockup para análisis de Casos de Uso.</div>
        </div>
      </div>
    `;

    const footerButtons = [
      {
        text: '❌ Cerrar',
        class: 'btn-secondary',
        onClick: (e, modalRef) => modalRef.closeModal()
      },
      {
        text: '🖨️ Registrar Pago y Cerrar',
        class: 'btn-primary',
        onClick: (e, modalRef) => {
          // Process state payment locally for this slice
          this.splitParts[partIndex].paid = true;
          this.splitParts[partIndex].method = this.activeSplitPaymentMethod;
          this.splitParts[partIndex].billNum = boletaNum;
          
          UIRenderer.logAction(`Pago fraccionado (Boleta ${boletaNum}) registrado para Cliente ${partIndex + 1} en ${this.activeSplitPaymentMethod.toUpperCase()}.`, 'success');
          
          // Clear active slice selection
          this.activeSplitPartIndex = null;
          this.activeSplitPaymentMethod = null;
          
          // Close modal
          modalRef.closeModal();

          // Check if all parts have been paid
          const allPaid = this.splitParts.every(p => p.paid);
          if (allPaid) {
            // Process entire order as paid
            AppState.payOrder(order.id, 'dividido');
            UIRenderer.logAction(`Mesa ${order.tableId} saldada por completo. Pedido finalizado.`, 'success');
            
            // Clear cashier state
            this.selectedOrderId = null;
            this.isSplitting = false;
            this.splitParts = [];
          }

          // Re-render
          this.render(mainContainer);
        }
      }
    ];

    UIRenderer.showModal('Comprobante Fraccionado Emitido (Simulación)', receiptBodyHTML, footerButtons);
  }
}
