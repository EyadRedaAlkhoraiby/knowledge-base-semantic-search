import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, of } from 'rxjs';
import { User } from '../models/document.model';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private readonly apiBase = '/api/auth';
    private currentUserSubject = new BehaviorSubject<User | null>(null);
    public currentUser$ = this.currentUserSubject.asObservable();

    constructor(private http: HttpClient) { }

    get currentUser(): User | null {
        return this.currentUserSubject.value;
    }

    get isAdmin(): boolean {
        return this.currentUser?.isAdmin || this.currentUser?.role === 'ADMIN' || false;
    }

    get isAuthenticated(): boolean {
        return this.currentUser !== null;
    }

    fetchCurrentUser(): Observable<User | null> {
        return this.http.get<User>(`${this.apiBase}/me`).pipe(
            tap(user => this.currentUserSubject.next(user)),
            catchError(() => {
                this.currentUserSubject.next(null);
                return of(null);
            })
        );
    }

    login(username: string, password: string, rememberMe: boolean = false): Observable<any> {
        // Spring Security expects form-encoded data
        const body = new URLSearchParams();
        body.set('username', username);
        body.set('password', password);
        if (rememberMe) {
            body.set('remember-me', 'on');
        }

        const headers = new HttpHeaders({
            'Content-Type': 'application/x-www-form-urlencoded'
        });

        return this.http.post(`${this.apiBase}/login`, body.toString(), {
            headers,
            responseType: 'text' as 'json',
            observe: 'response'
        });
    }

    register(username: string, email: string, password: string): Observable<any> {
        return this.http.post(`${this.apiBase}/register`, { username, email, password });
    }

    logout(): Observable<any> {
        return this.http.post(`${this.apiBase}/logout`, null, {
            responseType: 'text' as 'json'
        }).pipe(
            tap(() => this.currentUserSubject.next(null))
        );
    }

    checkAuth(): Observable<any> {
        return this.http.get(`${this.apiBase}/check`);
    }
}
