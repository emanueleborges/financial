import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'fh-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class AppComponent {}
