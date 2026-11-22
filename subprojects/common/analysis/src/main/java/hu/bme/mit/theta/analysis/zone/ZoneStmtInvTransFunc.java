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

package hu.bme.mit.theta.analysis.zone;

import hu.bme.mit.theta.analysis.InvTransFunc;
import hu.bme.mit.theta.analysis.expr.StmtAction;

import java.util.Collection;
import java.util.Collections;

public class ZoneStmtInvTransFunc implements InvTransFunc<ZoneState, StmtAction, ZonePrec> {

    private final static ZoneStmtInvTransFunc INSTANCE = new ZoneStmtInvTransFunc();

    private ZoneStmtInvTransFunc() {
    }

    public static ZoneStmtInvTransFunc getInstance() {
        return INSTANCE;
    }

    @Override
    public Collection<? extends ZoneState> getPreStates(ZoneState state, StmtAction action, ZonePrec prec) {
        return Collections.singleton(ZoneStmtApplier.pre(state, action, prec));
    }
}
