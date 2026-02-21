import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss']
})
export class LoginComponent {
    username = '';
    password = '';
    rememberMe = false;
    errorMessage = '';
    successMessage = '';
    loading = false;

    constructor(private authService: AuthService, private router: Router) {
        // Check URL params for messages
        const params = new URLSearchParams(window.location.search);
        if (params.get('registered') === 'true') {
            this.successMessage = 'Registration successful! Please sign in.';
        }
    }

    onSubmit() {
        this.errorMessage = '';
        this.loading = true;

        this.authService.login(this.username, this.password, this.rememberMe).subscribe({
            next: (response) => {
                // On success, Spring Security redirects. Fetch user and go to dashboard
                this.authService.fetchCurrentUser().subscribe({
                    next: () => this.router.navigate(['/dashboard']),
                    error: () => {
                        this.loading = false;
                        this.router.navigate(['/dashboard']);
                    }
                });
            },
            error: (err) => {
                this.loading = false;
                this.errorMessage = 'Invalid username or password. Please try again.';
            }
        });
    }
}
