# Guide de connexion Frontend - Backend Java

## 🌐 Configuration Backend

**URL de votre backend :** `http://localhost:8000`

**Endpoints disponibles :**
- Swagger UI: `http://localhost:8000/swagger-ui.html`
- H2 Console: `http://localhost:8000/h2-console`
- API Base: `http://localhost:8000/api`

## 🔑 Configuration pour votre Frontend

### **1. Variables d'environnement**

```bash
# Développement local
VITE_API_URL=http://localhost:8000/api
# ou pour React
REACT_APP_API_URL=http://localhost:8000/api
# ou pour Angular
API_URL=http://localhost:8000/api
```

### **2. Configuration CORS (Backend)**

Votre backend accepte déjà toutes les origines. Vérifiez dans `CorsConfig.java` :

```java
registry.addMapping("/api/**")
    .allowedOrigins("*")
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*");
```

## 📱 Exemples par Framework

### **React + JavaScript**

```javascript
// src/config/api.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8000/api';

export const api = {
  // Configuration de base
  getHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };
  },

  // Requête GET générique
  async get(endpoint) {
    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'GET',
        headers: this.getHeaders()
      });
      
      if (!response.ok) throw new Error('Erreur réseau');
      return response.json();
    } catch (error) {
      console.error('Erreur GET:', error);
      throw error;
    }
  },

  // Requête POST générique
  async post(endpoint, data) {
    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: this.getHeaders(),
        body: JSON.stringify(data)
      });
      
      if (!response.ok) throw new Error('Erreur réseau');
      return response.json();
    } catch (error) {
      console.error('Erreur POST:', error);
      throw error;
    }
  },

  // Requête PUT générique
  async put(endpoint, data) {
    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'PUT',
        headers: this.getHeaders(),
        body: JSON.stringify(data)
      });
      
      if (!response.ok) throw new Error('Erreur réseau');
      return response.json();
    } catch (error) {
      console.error('Erreur PUT:', error);
      throw error;
    }
  },

  // Requête DELETE générique
  async delete(endpoint) {
    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'DELETE',
        headers: this.getHeaders()
      });
      
      if (!response.ok) throw new Error('Erreur réseau');
      return response.json();
    } catch (error) {
      console.error('Erreur DELETE:', error);
      throw error;
    }
  }
};

// src/services/authService.js
export const authService = {
  async login(email, password) {
    const data = await api.post('/auth/login', { email, password });
    if (data.token) {
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
    }
    return data;
  },

  async register(userData) {
    return await api.post('/auth/register', userData);
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getToken() {
    return localStorage.getItem('token');
  },

  isAuthenticated() {
    return !!this.getToken();
  }
};

// src/components/Login.jsx
import React, { useState } from 'react';
import { authService } from '../services/authService';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await authService.login(email, password);
      // Rediriger vers le dashboard
      window.location.href = '/dashboard';
    } catch (error) {
      alert('Erreur de connexion');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Email"
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Mot de passe"
      />
      <button type="submit">Connexion</button>
    </form>
  );
};
```

### **Vue.js 3**

```javascript
// src/config/api.js
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000/api';

export const api = {
  getHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };
  },

  async get(endpoint) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'GET',
      headers: this.getHeaders()
    });
    return response.json();
  },

  async post(endpoint, data) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(data)
    });
    return response.json();
  },

  async put(endpoint, data) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(data)
    });
    return response.json();
  },

  async delete(endpoint) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'DELETE',
      headers: this.getHeaders()
    });
    return response.json();
  }
};

// src/services/authService.js
import { api } from '../config/api';

export const authService = {
  async login(email, password) {
    const data = await api.post('/auth/login', { email, password });
    if (data.token) {
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
    }
    return data;
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  isAuthenticated() {
    return !!localStorage.getItem('token');
  }
};

// src/components/Login.vue
<template>
  <form @submit.prevent="handleSubmit">
    <input v-model="email" type="email" placeholder="Email" />
    <input v-model="password" type="password" placeholder="Mot de passe" />
    <button type="submit">Connexion</button>
  </form>
</template>

<script setup>
import { ref } from 'vue';
import { authService } from '../services/authService';

const email = ref('');
const password = ref('');

const handleSubmit = async () => {
  try {
    await authService.login(email.value, password.value);
    window.location.href = '/dashboard';
  } catch (error) {
    alert('Erreur de connexion');
  }
};
</script>
```

### **Angular**

```typescript
// src/config/api.config.ts
export const API_CONFIG = {
  BASE_URL: 'http://localhost:8000/api'
};

// src/services/http.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_CONFIG } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class HttpService {
  constructor(private http: HttpClient) {}

  private getHeaders() {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    });
    return headers;
  }

  get<T>(endpoint: string) {
    return this.http.get<T>(`${API_CONFIG.BASE_URL}${endpoint}`, {
      headers: this.getHeaders()
    });
  }

  post<T>(endpoint: string, data: any) {
    return this.http.post<T>(`${API_CONFIG.BASE_URL}${endpoint}`, data, {
      headers: this.getHeaders()
    });
  }

  put<T>(endpoint: string, data: any) {
    return this.http.put<T>(`${API_CONFIG.BASE_URL}${endpoint}`, data, {
      headers: this.getHeaders()
    });
  }

  delete<T>(endpoint: string) {
    return this.http.delete<T>(`${API_CONFIG.BASE_URL}${endpoint}`, {
      headers: this.getHeaders()
    });
  }
}

// src/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpService } from './http.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private http: HttpService) {}

  login(email: string, password: string) {
    return this.http.post('/auth/login', { email, password }).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('user', JSON.stringify(response.user));
        }
      })
    );
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }
}

// src/app/components/login/login.component.ts
import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';

  constructor(private authService: AuthService) {}

  onSubmit() {
    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        window.location.href = '/dashboard';
      },
      error: () => {
        alert('Erreur de connexion');
      }
    });
  }
}
```

## 🔌 Exemples d'endpoints

### **Authentification**
```javascript
// Login
POST /api/auth/login
Body: { "email": "user@example.com", "password": "password" }

// Register
POST /api/auth/register
Body: { "email": "user@example.com", "password": "password", "username": "user" }

// Status
GET /api/auth/status
Headers: { "Authorization": "Bearer <token>" }
```

### **Utilisateurs**
```javascript
// Lister tous les utilisateurs
GET /api/utilisateurs
Headers: { "Authorization": "Bearer <token>" }

// Créer un utilisateur
POST /api/utilisateurs
Body: { "email": "user@example.com", "password": "password", "username": "user" }

// Mettre à jour un utilisateur
PUT /api/utilisateurs/{id}
Body: { "email": "new@example.com", "username": "newuser" }

// Supprimer un utilisateur
DELETE /api/utilisateurs/{id}
Headers: { "Authorization": "Bearer <token>" }
```

## 🧪 Test des endpoints

### **Avec curl**
```bash
# Test de connexion
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Test avec token
curl -X GET http://localhost:8000/api/utilisateurs \
  -H "Authorization: Bearer <votre_token>"
```

### **Avec Postman**
1. Importez la collection Swagger : `http://localhost:8000/swagger-ui.html`
2. Configurez l'URL de base : `http://localhost:8000/api`
3. Testez chaque endpoint

## ⚠️ Erreurs courantes

### **CORS**
Si vous avez des erreurs CORS :
```java
// Vérifiez CorsConfig.java
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:3000") // ou "*" pour tous
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(false);
```

### **401 Unauthorized**
Assurez-vous d'envoyer le token :
```javascript
headers: {
  'Authorization': `Bearer ${token}`
}
```

### **Network Error**
Vérifiez que le backend tourne sur le bon port :
```bash
# Vérifiez que le backend tourne sur le port 8000
curl http://localhost:8000
```

## 🎯 Checklist déploiement

- [ ] Backend démarré sur localhost:8000
- [ ] CORS configuré
- [ ] Variables d'environnement définies
- [ ] Token JWT stocké après login
- [ ] Headers Authorization configurés
- [ ] Test des endpoints avec Postman
- [ ] Frontend connecté avec succès

**Votre backend est prêt ! Commencez par tester les endpoints avec Swagger UI.** 🚀
