import { Routes } from '@angular/router';
import { ChatComponent } from './pages/chat.component/chat.component';

export const routes: Routes = [
    {
        path: '',
        component: ChatComponent
    },
    {
        path: '**',
        redirectTo: ''
    }
];
