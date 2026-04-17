package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.util.automaton.builder.AutomatonBuilders;

import java.util.HashSet;
import java.util.Set;

public class XtaUntimedDfaBuilder {
    private XtaUntimedDfaBuilder() {}

    public static DFA<?, String> build(XtaProcess process, Alphabet<String> alphabet) {
        var builder = AutomatonBuilders.newDFA(alphabet)
                        .withInitial(process.getInitLoc());

        for (XtaProcess.Loc loc : process.getLocs()) {
            builder.withAccepting(loc);
        }

        Set<String> symbols = new HashSet<>();
        for (XtaProcess.Loc loc : process.getLocs()) {
            for (XtaProcess.Edge edge : loc.getOutEdges()) {
                String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
                symbols.add(symbol);

                builder.from(loc).on(symbol).to(edge.getTarget());
            }
        }

        for (XtaProcess.Loc loc : process.getLocs()) {
            for (String symbol : alphabet) {
                if (!symbols.contains(symbol)) {
                    builder.from(loc).on(symbol).loop();
                }
            }
        }

        return builder.create();
    }
}
