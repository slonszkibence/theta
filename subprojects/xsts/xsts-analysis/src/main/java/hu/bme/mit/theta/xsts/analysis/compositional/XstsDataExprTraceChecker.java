/*
 *  Copyright 2026 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.xsts.analysis.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceStatus;
import hu.bme.mit.theta.analysis.expr.refinement.Refutation;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.ArrayList;
import java.util.List;

public final class XstsDataExprTraceChecker<R extends Refutation> implements ExprTraceChecker<R> {

    private final ExprTraceChecker<R> traceChecker;
    private final TimedXstsActionProjections projections;

    private XstsDataExprTraceChecker(
            final ExprTraceChecker<R> traceChecker, final TimedXstsActionProjections projections) {
        this.traceChecker = checkNotNull(traceChecker);
        this.projections = checkNotNull(projections);
    }

    public static <R extends Refutation> XstsDataExprTraceChecker<R> create(
            final ExprTraceChecker<R> traceChecker, final TimedXstsActionProjections projections) {
        return new XstsDataExprTraceChecker<>(traceChecker, projections);
    }

    @Override
    public ExprTraceStatus<R> check(final Trace<? extends ExprState, ? extends ExprAction> trace) {
        final List<XstsAction> dataActions = new ArrayList<>();
        for (final ExprAction action : trace.getActions()) {
            if (!(action instanceof XstsAction xstsAction)) {
                throw new IllegalArgumentException(String.format("Expected an XstsAction, got %s", action));
            }
            dataActions.add(projections.dataProjection(xstsAction));
        }
        return traceChecker.check(Trace.of(trace.getStates(), dataActions));
    }
}
