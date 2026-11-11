/**
 * Visits (journeys): lifecycle, landmark check-ins, stamps and rewards.
 * Owns tables: journeys, journey_landmark_visits, stamps, user_stamps, rewards, user_rewards.
 * Publishes {@link vn.edu.usth.hanoilens.modules.visit.VisitEnded}.
 */
@ApplicationModule(displayName = "Visit")
package vn.edu.usth.hanoilens.modules.visit;

import org.springframework.modulith.ApplicationModule;
