
package com.me94me.resource_lifecycle.lifecycle;

import androidx.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MethodCallsLogger {
    private Map<String, Integer> mCalledMethods = new HashMap<>();

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean approveCall(String name, int type) {
        Integer nullableMask = mCalledMethods.get(name);
        int mask = nullableMask != null ? nullableMask : 0;
        boolean wasCalled = (mask & type) != 0;
        mCalledMethods.put(name, mask | type);
        return !wasCalled;
    }
}
