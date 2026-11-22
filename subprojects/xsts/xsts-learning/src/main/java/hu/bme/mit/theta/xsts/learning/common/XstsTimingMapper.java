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

package hu.bme.mit.theta.xsts.learning.common;

import de.learnlib.sul.SULMapper;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsTimingMapper<AI, AO, CO> implements SULMapper<AI, AO, XstsAction, CO> {
    private final Function<CO, AO> outputFunction;

    private XstsTimingMapper(Function<CO, AO> outputFunction) {
        this.outputFunction = checkNotNull(outputFunction);
    }

    public static <AO, CO> XstsTimingMapper<XstsAction, AO, CO> identity(
            final Function<CO, AO> outputFunction) {
        return new XstsTimingMapper<>(outputFunction);
    }

    @Override
    public XstsAction mapInput(AI input) {
        return (XstsAction) input;
    }

    @Override
    public AO mapOutput(CO concreteOutput) {
        return outputFunction.apply(concreteOutput);
    }
}
