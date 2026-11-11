/**
 * Recaps and albums. Owns tables: recaps, recap_photos.
 * Reacts to {@link vn.edu.usth.hanoilens.modules.visit.VisitEnded} by creating a PENDING recap.
 */
@ApplicationModule(displayName = "Recap")
package vn.edu.usth.hanoilens.modules.recap;

import org.springframework.modulith.ApplicationModule;
