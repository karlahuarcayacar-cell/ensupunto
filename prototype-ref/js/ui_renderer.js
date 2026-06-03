// UI Renderer helper for "En su punto SAC"
// Handles the Use Case Panel, Logs, and global Modal overlay

class UIRenderer {
  static init() {
    const toggleBtn = document.getElementById('uc-panel-toggle');
    const panel = document.getElementById('use-case-panel');
    const mainContent = document.getElementById('main-viewport');
    const clearLogBtn = document.getElementById('clear-log-btn');
    
    // Toggle Use Case Panel
    toggleBtn.addEventListener('click', () => {
      panel.classList.toggle('open');
      mainContent.classList.toggle('panel-open');
    });

    // Sync main content padding-right on start if panel is open
    if (panel.classList.contains('open')) {
      mainContent.classList.add('panel-open');
    } else {
      mainContent.classList.remove('panel-open');
    }

    // Clear logs click handler
    clearLogBtn.addEventListener('click', () => {
      this.clearLogs();
    });

    // Setup modal close actions
    document.querySelectorAll('[data-close-modal]').forEach(el => {
      el.addEventListener('click', () => this.closeModal());
    });

    // Populate initial log
    this.logAction('Sistema inicializado. Base de datos cargada.', 'success');
  }

  // Set the current Use Case and highlight the current step
  static setActiveUseCase(cuId, stepNum) {
    const uc = USE_CASES[cuId];
    if (!uc) {
      console.warn(`Caso de uso no encontrado: ${cuId}`);
      return;
    }

    // Update Meta
    document.getElementById('uc-title').innerText = `${uc.id}: ${uc.title}`;
    document.getElementById('uc-actor').innerText = uc.actor;
    document.getElementById('uc-preconditions').innerText = uc.preconditions;
    document.getElementById('uc-postconditions').innerText = uc.postconditions;

    // Render steps list
    const stepsListContainer = document.getElementById('uc-steps-list');
    stepsListContainer.innerHTML = '';

    uc.steps.forEach(step => {
      const stepItem = document.createElement('div');
      stepItem.className = 'uc-step-item';
      
      // Determine if step is completed, active or pending
      if (step.num < stepNum) {
        stepItem.classList.add('completed');
      } else if (step.num === stepNum) {
        stepItem.classList.add('active');
      }

      stepItem.innerHTML = `
        <div class="uc-step-number">${step.num}</div>
        <div class="uc-step-desc">${step.desc}</div>
      `;
      stepsListContainer.appendChild(stepItem);
    });

    // Ensure the panel is open when tracking changes to make it noticeable to user
    const panel = document.getElementById('use-case-panel');
    if (!panel.classList.contains('open')) {
      panel.classList.add('open');
      document.getElementById('main-viewport').classList.add('panel-open');
    }
  }

  // Logs a simulation event
  static logAction(message, type = 'info') {
    const container = document.getElementById('uc-log-container');
    const time = new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    
    const entry = document.createElement('div');
    entry.className = `uc-log-entry ${type}`;
    entry.innerText = `[${time}] ${message}`;
    
    container.appendChild(entry);
    container.scrollTop = container.scrollHeight;
  }

  static clearLogs() {
    const container = document.getElementById('uc-log-container');
    container.innerHTML = '';
    this.logAction('Bitácora de simulación limpia.', 'info');
  }

  // Custom Modal helper
  static showModal(title, bodyHTML, footerButtons = []) {
    const modal = document.getElementById('general-modal');
    document.getElementById('modal-title').innerText = title;
    
    const bodyEl = document.getElementById('modal-body');
    bodyEl.innerHTML = bodyHTML;
    
    const footerEl = document.getElementById('modal-footer');
    footerEl.innerHTML = '';
    
    if (footerButtons.length === 0) {
      // Default Accept button
      const defBtn = document.createElement('button');
      defBtn.className = 'btn btn-primary';
      defBtn.innerText = 'Entendido';
      defBtn.addEventListener('click', () => this.closeModal());
      footerEl.appendChild(defBtn);
    } else {
      footerButtons.forEach(btnConfig => {
        const btn = document.createElement('button');
        btn.className = `btn ${btnConfig.class || 'btn-secondary'}`;
        btn.innerText = btnConfig.text;
        btn.addEventListener('click', (e) => btnConfig.onClick(e, this));
        footerEl.appendChild(btn);
      });
    }

    modal.classList.add('active');
  }

  static closeModal() {
    const modal = document.getElementById('general-modal');
    modal.classList.remove('active');
  }
}
