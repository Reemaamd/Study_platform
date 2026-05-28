// Correspond exactement aux DTOs Spring Boot

export interface DashboardStatsDTO {
  totalObjectives: number;
  achievedObjectives: number;
  totalSessions: number;
  completedSessions: number;
  totalHours: number;
  completionRate: number;  // % sessions complétées / total
}

export interface StudyTimeStatsDTO {
  plannedHours: number;
  completedHours: number;
  remainingHours: number;
}

export interface SessionProgressDTO {
  plannedSessions: number;
  doneSessions: number;
  completionRate: number;
}

export interface WeeklyProductivityDTO {

  week: string;

  hoursStudied: number;

  sessionsCompleted: number;

  achievedObjectives: number;
}

export interface SubjectStatsDTO {
  subjectName: string;
  totalHours: number;
  progressPercentage: number;
}

export interface DailyStudyHoursDTO {
  day: string;   // "MONDAY" | "TUESDAY" | ...
  hours: number;
}

export interface ObjectiveCompletionDTO {
  totalObjectives: number;
  achievedObjectives: number;
  completionRate: number;
}

export interface CurrentWeekStatsDTO {
  studyHours: number;
  completedSessions: number;
  achievedObjectives: number;
}
