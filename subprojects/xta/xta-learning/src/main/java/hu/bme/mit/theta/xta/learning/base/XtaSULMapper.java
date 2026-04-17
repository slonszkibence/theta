package hu.bme.mit.theta.xta.learning.base;

import de.learnlib.sul.SULMapper;

import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class XtaSULMapper<AO, CO> implements SULMapper<String, AO, XtaProcess.Edge, CO> {
    private final Map<String, XtaProcess.Edge> edgeDictionary;
    private final Function<CO, AO> outputFunction;

    private XtaSULMapper(XtaSystem xtaSystem, Function<CO, AO> outputFunction) {
        this.edgeDictionary = new HashMap<>();
        this.outputFunction = outputFunction;

        for (XtaProcess process : xtaSystem.getProcesses()) {
            for (var edge : process.getEdges()) {
                String edgeName = XtaTimingMapper.generateSymbolForEdge(edge);
                edgeDictionary.put(edgeName, edge);
            }
        }
    }

    public static <AO, CO> XtaSULMapper<AO, CO> create(XtaSystem xtaSystem, Function<CO, AO> outputFunction) {
        return new XtaSULMapper<>(xtaSystem, outputFunction);
    }

    @Override
    public XtaProcess.Edge mapInput(String abstractInput) {
        return edgeDictionary.get(abstractInput);
    }

    @Override
    public AO mapOutput(CO concreteOutput) {
        return outputFunction.apply(concreteOutput);
    }

    public Set<String> getAlphaBetSymbols() {
        return edgeDictionary.keySet();
    }
}
