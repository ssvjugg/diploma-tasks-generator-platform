export type UserProfile = {
  id: string;
  keycloakId: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
};
