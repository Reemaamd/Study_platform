import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app';

export default () =>
  bootstrapApplication(AppComponent, appConfig);