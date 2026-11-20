import { Routes } from '@angular/router';
import { ChatComponent } from './pages/chat.component/chat.component';
import { LoginComponent } from './pages/login.component/login.component';
import { authGuard, guestGaurd } from './core/auth-guard';
import { DashboardComponent } from './pages/dashboard.component/dashboard.component';

export const routes: Routes = [
    {
        path: 'login',
        component: LoginComponent,
        canActivate: [guestGaurd]
    },
    {
        path: 'dashboard',
        component: DashboardComponent,
        canActivate: [authGuard]
    },
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
    },
    {
        path: '**',
        redirectTo: 'dashboard'
    }
];
