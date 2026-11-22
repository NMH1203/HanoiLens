package vn.edu.usth.hanoilens.modules.identity;

import vn.edu.usth.hanoilens.shared.LocationPrecision;

/** Allowlisted profile fields; null means "leave unchanged". */
public record ProfileUpdate(String displayName, String locale, LocationPrecision defaultLocationPrecision) {
}
