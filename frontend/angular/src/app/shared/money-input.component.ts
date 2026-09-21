import { Component, EventEmitter, Input, Output } from '@angular/core';
import { maskBRLFromDigits, parseBRLInput } from '../core/format';

@Component({
  selector: 'fh-money-input',
  template: `
    <div class="money-wrap">
      <span class="prefix">R$</span>
      <input
        class="input"
        [class.error]="error"
        [value]="value"
        [placeholder]="placeholder"
        inputmode="decimal"
        autocomplete="off"
        (input)="onInput($event)"
      />
    </div>
  `,
})
export class MoneyInputComponent {
  @Input() value = '';
  @Input() error = false;
  @Input() placeholder = '0,00';
  @Output() valueChange = new EventEmitter<{ masked: string; numeric: number | null }>();

  onInput(event: Event) {
    const raw = (event.target as HTMLInputElement).value;
    const masked = maskBRLFromDigits(raw);
    (event.target as HTMLInputElement).value = masked;
    this.valueChange.emit({ masked, numeric: parseBRLInput(masked) });
  }
}
