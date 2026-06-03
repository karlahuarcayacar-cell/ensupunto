// Main Application Orchestrator for En su punto SAC

class Application {
  static mainViewport = null;
  static currentRole = null;

  static start() {
    // 1. Initialize DB State
    AppState.init();

    // 2. Initialize UI Engine
    UIRenderer.init();

    // 3. Cache DOM Elements
    this.mainViewport = document.getElementById('main-viewport');
    
    // Bind Login & Session Action Listeners
    this.bindLoginActions();
    this.bindGlobalNavigation();
    
    // Check if session is already active
    const activeSession = AppState.getCurrentSession();
    if (activeSession) {
      this.handleSuccessfulLogin(activeSession);
    } else {
      this.routeToLogin();
    }
  }

  // ----------------------------------------------------
  // ROUTING & SESSION FLOW
  // ----------------------------------------------------
  static routeToLogin() {
    RoleChef.stopTimers(); // stop kitchen ticking if active
    document.getElementById('login-screen').style.display = 'flex';
    document.getElementById('app-screen').style.display = 'none';
    this.currentRole = null;
    
    // Set UC Track to CU-01 Step 1
    UIRenderer.setActiveUseCase('CU-01', 1);
  }

  static handleSuccessfulLogin(session) {
    document.getElementById('login-screen').style.display = 'none';
    document.getElementById('app-screen').style.display = 'block';

    // Set NavBar profile labels
    document.getElementById('session-name').innerText = session.name;
    document.getElementById('session-role').innerText = session.role;

    // Set Active Role highlight in NavBar
    this.updateNavbarRoleSwitcher(session.role);

    UIRenderer.logAction(`Sesión iniciada por ${session.name} (${session.role.toUpperCase()})`, 'success');
    
    // Route to Role Workspace
    this.routeToRole(session.role);
  }

  static routeToRole(role) {
    this.currentRole = role;

    // Clean kitchen timers if we are leaving Chef role
    if (role !== 'chef') {
      RoleChef.stopTimers();
    }

    // Render corresponding role interface
    if (role === 'mesero') {
      RoleMesero.render(this.mainViewport, 'mesas');
    } else if (role === 'chef') {
      RoleChef.render(this.mainViewport);
    } else if (role === 'cajero') {
      RoleCajero.render(this.mainViewport);
    } else if (role === 'admin') {
      RoleAdmin.render(this.mainViewport, 'reportes');
    }

    // Dynamic Use Case synchronization
    this.syncUseCaseTrack();
  }

  // Synchronizes the Use Case Analyzer panel according to role/view
  static syncUseCaseTrack() {
    let ucDetails = null;

    if (this.currentRole === 'mesero') {
      const view = RoleMesero.activeView;
      const count = RoleMesero.currentOrderItems.length;
      ucDetails = getContextUseCase('mesero', view, { 
        selectedDishCount: count,
        isModifying: RoleMesero.isModifying,
        adminApprovalRequested: Object.keys(RoleMesero.adminApprovedCancellations).length > 0
      });
    } else if (this.currentRole === 'chef') {
      ucDetails = getContextUseCase('chef', null, { preparingOrderId: null });
    } else if (this.currentRole === 'cajero') {
      const selectedId = RoleCajero.selectedOrderId;
      const paymentSelected = !!RoleCajero.selectedPaymentMethod;
      const isSplitting = RoleCajero.isSplitting;
      const splitPanelOpen = RoleCajero.splitParts.length === 0;
      const splitsConfigured = RoleCajero.splitParts.length > 0;
      const paymentMethodSelected = RoleCajero.activeSplitPaymentMethod !== null;
      // If we are currently showing a printed fractioned boleta, we simulate step 5 (paid fraction)
      const splitInvoiceIndex = (RoleCajero.activeSplitPartIndex !== null) ? RoleCajero.activeSplitPartIndex : undefined;

      ucDetails = getContextUseCase('cajero', 'caja_main', { 
        selectedOrderId: selectedId, 
        paymentSelected,
        isSplitting,
        splitPanelOpen,
        splitsConfigured,
        paymentMethodSelected,
        splitInvoiceIndex
      });
    } else if (this.currentRole === 'admin') {
      const tab = RoleAdmin.activeTab;
      ucDetails = getContextUseCase('admin', tab, null);
    }

    if (ucDetails) {
      UIRenderer.setActiveUseCase(ucDetails.cu, ucDetails.step);
    }
  }

  // ----------------------------------------------------
  // DOM BINDINGS & EVENT LISTENERS
  // ----------------------------------------------------
  static bindLoginActions() {
    const loginForm = document.getElementById('login-form');
    
    // Normal input submission
    loginForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const usernameInput = document.getElementById('login-username');
      const passwordInput = document.getElementById('login-password');
      
      UIRenderer.setActiveUseCase('CU-01', 3); // Validating step
      
      const session = AppState.login(usernameInput.value, passwordInput.value);
      
      if (session) {
        UIRenderer.setActiveUseCase('CU-01', 4); // Redirecting step
        setTimeout(() => {
          this.handleSuccessfulLogin(session);
          usernameInput.value = '';
        }, 300);
      } else {
        alert('Credenciales incorrectas. (Intente usar el acceso rápido o contraseñas por defecto).');
        UIRenderer.setActiveUseCase('CU-01', 2); // Go back to credentials entry
        UIRenderer.logAction('Intento fallido de inicio de sesión.', 'warn');
      }
    });

    // Quick Simulation Access Buttons
    document.querySelectorAll('.quick-role-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const username = btn.getAttribute('data-username');
        
        UIRenderer.setActiveUseCase('CU-01', 2); // Simulating clicking
        UIRenderer.logAction(`Acceso rápido clickeado para el rol: ${username.toUpperCase()}`, 'info');

        UIRenderer.setActiveUseCase('CU-01', 3);
        const session = AppState.login(username, '123'); // Standard default pwd is 123
        
        if (session) {
          UIRenderer.setActiveUseCase('CU-01', 4);
          setTimeout(() => {
            this.handleSuccessfulLogin(session);
          }, 200);
        }
      });
    });
  }

  static bindGlobalNavigation() {
    // Logout Click Handler
    document.getElementById('logout-btn').addEventListener('click', () => {
      UIRenderer.logAction('Sesión cerrada por el usuario.', 'warn');
      AppState.logout();
      this.routeToLogin();
    });

    // Role Quick Switcher Click Handlers (Great simulator aid!)
    document.querySelectorAll('.role-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const targetRole = btn.getAttribute('data-role');
        if (this.currentRole === targetRole) return;

        // Simulate login as a default user for that target role
        const users = AppState.getUsers();
        const user = users.find(u => u.role === targetRole);
        
        if (user) {
          // Switch session mock
          const session = {
            userId: user.id,
            name: user.name,
            role: user.role,
            username: user.username
          };
          AppState.setStorageItem('current_session', session);
          
          // Render View
          this.handleSuccessfulLogin(session);
          UIRenderer.logAction(`Cambio rápido de simulación a rol: ${targetRole.toUpperCase()}`, 'info');
        }
      });
    });

    // Observe global document clicks to dynamically sync steps tracker 
    // since user changes fields, updates amounts, or modifies modals.
    document.addEventListener('click', () => {
      if (this.currentRole) {
        this.syncUseCaseTrack();
      }
    });
  }

  static updateNavbarRoleSwitcher(activeRole) {
    document.querySelectorAll('.role-btn').forEach(btn => {
      if (btn.getAttribute('data-role') === activeRole) {
        btn.classList.add('active');
      } else {
        btn.classList.remove('active');
      }
    });
  }
}

// Start the Application when DOM is ready
window.addEventListener('DOMContentLoaded', () => {
  Application.start();
});
