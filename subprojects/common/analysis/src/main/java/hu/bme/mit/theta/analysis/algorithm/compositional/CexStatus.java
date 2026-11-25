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
package hu.bme.mit.theta.analysis.algorithm.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.common.Utils;

import net.automatalib.word.Word;

public abstract class CexStatus<A, I> {

    private CexStatus() {}

    public static <A, I> Realizable<A, I> realizable() {
        return new Realizable<>();
    }

    public static <A, I> Spurious<A, I> spurious(final Word<I> word) {
        return new Spurious<>(word);
    }

    public static <A, I> Imprecise<A, I> imprecise(final LearnedComponent<A, I> refinedComponent) {
        return new Imprecise<>(refinedComponent);
    }

    public boolean isRealizable() {
        return false;
    }

    public boolean isSpurious() {
        return false;
    }

    public boolean isImprecise() {
        return false;
    }

    public Spurious<A, I> asSpurious() {
        throw new ClassCastException(String.format("Cannot cast %s to Spurious", getClass().getSimpleName()));
    }

    public Imprecise<A, I> asImprecise() {
        throw new ClassCastException(String.format("Cannot cast %s to Imprecise", getClass().getSimpleName()));
    }

    public static final class Realizable<A, I> extends CexStatus<A, I> {
        private Realizable() {}

        @Override
        public boolean isRealizable() {
            return true;
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(CexStatus.class.getSimpleName()).add(getClass().getSimpleName()).toString();
        }
    }

    public static final class Spurious<A, I> extends CexStatus<A, I> {
        private final Word<I> word;

        private Spurious(final Word<I> word) {
            this.word = checkNotNull(word);
        }

        public Word<I> getWord() {
            return word;
        }

        @Override
        public boolean isSpurious() {
            return true;
        }

        @Override
        public Spurious<A, I> asSpurious() {
            return this;
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(CexStatus.class.getSimpleName()).add(getClass().getSimpleName()).add(word).toString();
        }
    }

    public static final class Imprecise<A, I> extends CexStatus<A, I> {
        private final LearnedComponent<A, I> refinedComponent;

        private Imprecise(final LearnedComponent<A, I> refinedComponent) {
            this.refinedComponent = checkNotNull(refinedComponent);
        }

        public LearnedComponent<A, I> getRefinedComponent() {
            return refinedComponent;
        }

        @Override
        public boolean isImprecise() {
            return true;
        }

        @Override
        public Imprecise<A, I> asImprecise() {
            return this;
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(CexStatus.class.getSimpleName()).add(getClass().getSimpleName()).toString();
        }
    }
}
