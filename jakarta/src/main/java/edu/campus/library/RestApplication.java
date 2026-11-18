package edu.campus.library;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Turns on Jakarta REST. Every resource is served under /api.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
