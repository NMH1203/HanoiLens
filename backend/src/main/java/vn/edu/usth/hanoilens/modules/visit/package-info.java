/**
 * Visits: lifecycle, landmark check-ins and stamps.
 * Owns tables: visits, visit_landmarks, stamps, user_stamps.
 * Publishes {@link vn.edu.usth.hanoilens.modules.visit.VisitEnded}.
 */
@ApplicationModule(displayName = "Visit")
package vn.edu.usth.hanoilens.modules.visit;

import org.springframework.modulith.ApplicationModule;
