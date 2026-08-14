import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ApiClientError } from '../core/models';
import { fieldLabel } from '../core/format';

@Component({
  selector: 'fh-form-error',
  template: `
    @if (error) {
      <div class="alert" role="alert">
        @if (isFieldError()) {
          <p style="margin: 0 0 0.5rem; font-weight: 600;">{{ message() }}</p>
          <ul style="margin: 0; padding-left: 1.25rem;">
            @for (item of fieldEntries(); track item[0]) {
              <li><strong>{{ fieldLabel(item[0]) }}</strong>: {{ item[1] }}</li>
            }
          </ul>
        } @else {
          {{ message() }}
        }
      </div>
    }
  `,
})
export class FormErrorComponent {
  @Input() error: unknown;
  @Output() dismiss = new EventEmitter<void>();

  fieldLabel = fieldLabel;

  isFieldError() {
    return this.error instanceof ApiClientError && Object.keys(this.error.fields).length > 0;
  }

  fieldEntries() {
    return this.error instanceof ApiClientError ? Object.entries(this.error.fields) : [];
  }

  message() {
    if (this.error instanceof ApiClientError) {
      return this.isFieldError() ? this.error.message : `${this.error.code}: ${this.error.message}`;
    }
    if (this.error instanceof Error) return this.error.message;
    return 'Erro inesperado';
  }
}
