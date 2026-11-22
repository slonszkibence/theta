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
package hu.bme.mit.theta.analysis.dfa;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;

public abstract class DfaState<S> implements ExprState {

    private DfaState() {}

    public static <S> DfaState<S> of(final S state) {
        return new NonBottom<>(state);
    }

    public static <S> DfaState<S> top() {
        return new Top<>();
    }

    public static <S> DfaState<S> bottom() {
        return new Bottom<>();
    }

    public abstract boolean isTop();

    public abstract S getState();

    public abstract boolean isLeq(DfaState<S> that);

    private static final class NonBottom<S> extends DfaState<S> {
        private final S state;

        private NonBottom(final S state) {
            this.state = checkNotNull(state, "Automaton state must not be null");
        }

        @Override
        public boolean isTop() {
            return false;
        }

        @Override
        public S getState() {
            return state;
        }

        @Override
        public boolean isLeq(final DfaState<S> that) {
            return this.equals(that);
        }

        @Override
        public boolean isBottom() {
            return false;
        }

        @Override
        public Expr<BoolType> toExpr() {
            return BoolExprs.True();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof NonBottom<?> that && this.state.equals(that.state);
        }

        @Override
        public int hashCode() {
            return state.hashCode();
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(DfaState.class.getSimpleName()).add(state).toString();
        }
    }

    private static final class Top<S> extends DfaState<S> {
        private static final int HASH_SEED = 5381;

        @Override
        public boolean isTop() {
            return true;
        }

        @Override
        public S getState() {
            throw new UnsupportedOperationException("Top has no automaton state");
        }

        @Override
        public boolean isLeq(final DfaState<S> that) {
            return that.isTop();
        }

        @Override
        public boolean isBottom() {
            return false;
        }

        @Override
        public Expr<BoolType> toExpr() {
            return BoolExprs.True();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Top<?>;
        }

        @Override
        public int hashCode() {
            return HASH_SEED;
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(DfaState.class.getSimpleName()).add("Top").toString();
        }
    }

    private static final class Bottom<S> extends DfaState<S> {
        private static final int HASH_SEED = 7919;

        @Override
        public boolean isTop() {
            return false;
        }

        @Override
        public S getState() {
            throw new UnsupportedOperationException("Bottom has no automaton state");
        }

        @Override
        public boolean isLeq(final DfaState<S> that) {
            return true;
        }

        @Override
        public boolean isBottom() {
            return true;
        }

        @Override
        public Expr<BoolType> toExpr() {
            return BoolExprs.False();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Bottom<?>;
        }

        @Override
        public int hashCode() {
            return HASH_SEED;
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(DfaState.class.getSimpleName()).add("Bottom").toString();
        }
    }
}
