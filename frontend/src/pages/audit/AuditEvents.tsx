// Auditor · Events — the read-only mirror of the admin events export.
import { EventsView } from '../admin/Events';

export default function AuditEvents() {
  return <EventsView base="audit" />;
}
