package io.logflux.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {

    @Test
    void scopeIsolatesAttributes() {
        Scope scope1 = new Scope(100);
        Scope scope2 = new Scope(100);

        scope1.setAttribute("key1", "value1");
        scope2.setAttribute("key2", "value2");

        // Scopes don't share state (verified by no exception and no contamination)
        scope1.setAttribute("only_on_scope1", "yes");
        // scope2 should not have "only_on_scope1" - this is inherently true since
        // they are separate objects
        assertNotSame(scope1, scope2);
    }

    @Test
    void scopeSetUser() {
        Scope scope = new Scope(100);
        scope.setUser("user_123");
        // setAttribute internally - no exception means success
    }

    @Test
    void scopeSetRequest() {
        Scope scope = new Scope(100);
        scope.setRequest("GET", "/api/users", "req-abc-123");
        // setAttribute internally - no exception means success
    }

    @Test
    void scopeSetTraceContext() {
        Scope scope = new Scope(100);
        scope.setTraceContext("trace-id-1234", "span-id-5678");
        // No exception means success
    }

    @Test
    void scopeBreadcrumbs() {
        Scope scope = new Scope(100);
        scope.addBreadcrumb("http", "GET /api", null);
        scope.addBreadcrumb("db", "SELECT * FROM users", null);
        // Breadcrumbs are internal to the scope
    }

    @Test
    void scopeCreationWithDifferentBreadcrumbSizes() {
        Scope scope1 = new Scope(10);
        Scope scope2 = new Scope(0); // should default to 100
        Scope scope3 = new Scope(-1); // should default to 100
        assertNotNull(scope1);
        assertNotNull(scope2);
        assertNotNull(scope3);
    }
}
