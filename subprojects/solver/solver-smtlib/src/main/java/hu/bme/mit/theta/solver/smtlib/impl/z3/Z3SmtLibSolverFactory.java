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

package hu.bme.mit.theta.solver.smtlib.impl.z3;

import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverBinary;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverFactory;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTransformationManager;

import java.nio.file.Path;

public class Z3SmtLibSolverFactory extends GenericSmtLibSolverFactory {
    private final boolean itpSupported;

    private Z3SmtLibSolverFactory(Path solverPath, String[] args, boolean itpSupported) {
        super(solverPath, args);
        this.itpSupported = itpSupported;
    }

    public static Z3SmtLibSolverFactory create(Path solverPath, String[] args, boolean itpSupported) {
        return new Z3SmtLibSolverFactory(solverPath, args, itpSupported);
    }

    @Override
    public ItpSolver createItpSolver() {
        if(itpSupported) {
            final var symbolTable = new GenericSmtLibSymbolTable();
            final var transformationManager = new GenericSmtLibTransformationManager(symbolTable);
            final var termTransformer = new GenericSmtLibTermTransformer(symbolTable);
            final var solverBinary = new GenericSmtLibSolverBinary(solverPath, args);

            return new Z3SmtLibItpSolver(symbolTable, transformationManager, termTransformer, solverBinary);
        }
        else {
            throw new UnsupportedOperationException("Z3 interpolation supported below 4.5.0");
        }
    }
}
