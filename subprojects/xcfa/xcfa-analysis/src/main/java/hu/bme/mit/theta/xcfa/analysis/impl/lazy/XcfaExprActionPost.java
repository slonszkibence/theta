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

package hu.bme.mit.theta.xcfa.analysis.impl.lazy;

import hu.bme.mit.theta.analysis.expr.BasicExprState;
import hu.bme.mit.theta.analysis.algorithm.lazy.expr.ExprActionPost;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaAction;

public class XcfaExprActionPost implements ExprActionPost<XcfaAction> {
    private final QuantifiedExprActionPost<XcfaAction> exprActionPost = new QuantifiedExprActionPost<>();
    private XcfaExprActionPost() {}

    public static XcfaExprActionPost create() {
        return new XcfaExprActionPost();
    }

    @Override
    public BasicExprState post(BasicExprState state, XcfaAction action) {
        return exprActionPost.post(state, action);
    }
}
