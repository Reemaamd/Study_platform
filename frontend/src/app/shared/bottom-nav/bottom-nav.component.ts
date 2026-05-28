import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-bottom-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './bottom-nav.component.html',
  styleUrls: ['./bottom-nav.component.css']
})
export class BottomNavComponent {

  navItems = [

    { label: 'Home',         icon: '⌂', route: '/' },

    { label: 'Dashboard',    icon: '▣', route: '/dashboard' },

    { label: 'Analytics',    icon: '↗', route: '/analytics' },

    { label: 'Groups',       icon: '◎', route: '/groups' },

    { label: 'Planning',     icon: '▦', route: '/planning' },

    { label: 'Notification', icon: '◉', route: '/notification' },

    { label: 'Settings',     icon: '⚙', route: '/settings' },

  ];

}