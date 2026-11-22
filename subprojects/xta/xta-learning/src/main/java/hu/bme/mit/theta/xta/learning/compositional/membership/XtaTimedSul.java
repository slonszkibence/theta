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

package hu.bme.mit.theta.xta.learning.compositional.membership;

import de.learnlib.sul.SUL;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import static com.google.common.base.Preconditions.checkNotNull;

public final class XtaTimedSul implements SUL<String, Boolean> {
    private final TimedStepper stepper;
    private TimedStepper.Config config;
    private boolean dead;

    private XtaTimedSul(final TimedStepper stepper) {
        this.stepper = checkNotNull(stepper);
    }

    public static XtaTimedSul create(final TimedStepper stepper) {
        return new XtaTimedSul(stepper);
    }

    @Override
    public void pre() {
        config = stepper.initial();
        dead = config.zone().isBottom();
    }

    @Override
    public void post() {
        config = null;
        dead = false;
    }

    @Override
    public Boolean step(final String symbol) {
        if (dead) {
            return false;
        }
        final TimedStepper.Config next = stepper.step(config, symbol);
        if (next == null) {
            dead = true;
            return false;
        }
        config = next;
        return true;
    }
}
