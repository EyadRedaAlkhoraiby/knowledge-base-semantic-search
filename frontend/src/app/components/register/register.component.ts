import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './register.component.html',
    styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
    username = '';
    email = '';
    password = '';
    confirmPassword = '';
    errorMessage = '';
    loading = false;

    constructor(private authService: AuthService, private router: Router) { }

    onSubmit() {
        this.errorMessage = '';

        if (this.password !== this.confirmPassword) {
            this.errorMessage = 'Passwords do not match';
            return;
        }

        this.loading = true;

        this.authService.register(this.username, this.email, this.password).subscribe({
            next: () => {
                this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
            },
            error: (err) => {
                this.loading = false;
                this.errorMessage = err.error?.error || 'Registration failed. Please try again.';
            }
        });
    }
}
