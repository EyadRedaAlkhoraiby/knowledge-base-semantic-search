import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
    message: string;
    type: 'success' | 'error' | 'info';
    visible: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class ToastService {
    private toastSubject = new BehaviorSubject<Toast>({ message: '', type: 'success', visible: false });
    public toast$ = this.toastSubject.asObservable();

    private timeout: any;

    show(message: string, type: 'success' | 'error' | 'info' = 'success') {
        if (this.timeout) clearTimeout(this.timeout);
        this.toastSubject.next({ message, type, visible: true });
        this.timeout = setTimeout(() => {
            this.toastSubject.next({ ...this.toastSubject.value, visible: false });
        }, 3000);
    }
}
