import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent {
  sessions = [
    { time: '08:00 - 09:30', title: 'Mathématiques', sub: 'Algèbre linéaire', color: 'blue' },
    { time: '10:00 - 11:30', title: 'Physique', sub: 'Mécanique quantique', color: 'purple' },
    { time: '14:00 - 15:30', title: 'Chimie', sub: 'Réactions organiques', color: 'green' },
    { time: '16:00 - 17:30', title: 'Biologie', sub: 'Génétique moléculaire', color: 'orange' }
  ];

  features = [
    {
      num: 1,
      tag: 'Planifier',
      icon: '📅',
      title: 'Votre semaine en un coup dœil',
      desc: 'Visualisez toutes vos sessions sur un planning élégant.',
      color: 'blue'
    },
    {
      num: 2,
      tag: 'Étudier',
      icon: '📚',
      title: 'Bloquez le temps, pas la créativité',
      desc: 'Des sessions structurées sans prescription rigide.',
      color: 'green'
    },
    {
      num: 3,
      tag: 'Progresser',
      icon: '📊',
      title: 'Voyez vos progrès s’accumuler',
      desc: 'Chaque session crée un effet de boule de neige.',
      color: 'purple'
    },
    {
      num: 4,
      tag: 'Cercles',
      icon: '👥',
      title: 'Étudier n’est jamais seul',
      desc: 'Rejoignez des cercles d’étude curatés.',
      color: 'orange',
      large: true
    },
    {
      num: 5,
      tag: 'Bonus',
      icon: '⚡',
      title: 'L’IA qui orchestre votre semaine',
      desc: 'Des suggestions douces, jamais des ordres.',
      color: 'red'
    }
  ];
}