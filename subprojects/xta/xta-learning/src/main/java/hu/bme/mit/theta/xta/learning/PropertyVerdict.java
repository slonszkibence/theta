package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.xta.XtaSystem;

public final class PropertyVerdict {
    private PropertyVerdict() {}

    public static boolean holds(final XtaSystem system, final boolean errorLocationUnreachable) {
        return switch (system.getPropertyKind()) {
            case AG, NONE -> errorLocationUnreachable;
            case EF -> !errorLocationUnreachable;
            default -> throw new UnsupportedOperationException(
                    "Unsupported property kind: " + system.getPropertyKind());
        };
    }

    public static String describe(final XtaSystem system,
                                  final boolean errorLocationUnreachable) {
        return holds(system, errorLocationUnreachable) ? "SAFE" : "UNSAFE";
    }
}
