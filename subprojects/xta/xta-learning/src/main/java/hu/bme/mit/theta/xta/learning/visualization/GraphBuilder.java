package hu.bme.mit.theta.xta.learning.visualization;

import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Update;
import hu.bme.mit.theta.xta.XtaProcess;
import net.automatalib.automaton.fsa.DFA;

import java.util.HashMap;
import java.util.Map;

public class GraphBuilder {
    public static String generateXtaProcessDot(XtaProcess process) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ").append(process.getName()).append(" {\n");
        sb.append("\tlabel=\"\";\n");
        sb.append("\tsubgraph cluster_proc_0 {\n");
        sb.append("\t\tcolor=\"#000000\";\n");
        sb.append("\t\tstyle=solid;\n");
        sb.append("\t\tlabel=\"").append(process.getName()).append("\\n\";\n");

        Map<XtaProcess.Loc, String> locToId = new HashMap<>();
        int idCounter = 1;


        for (XtaProcess.Loc loc : process.getLocs()) {
            String locId = "loc_" + idCounter++;
            locToId.put(loc, locId);


            String cleanLocName = cleanSide(loc.getName());


            StringBuilder labelBuilder = new StringBuilder(cleanLocName + "\\n");
            for (Guard invar : loc.getInvars()) {
                String gStr = invar.toString().replace("\"", "\\\"");

                if (gStr.startsWith("(") && gStr.endsWith(")")) {
                    gStr = gStr.substring(1, gStr.length() - 1);
                }
                labelBuilder.append(gStr).append("\\n");
            }

            sb.append("\t\t").append(locId).append(" [label=\"").append(labelBuilder.toString())
                    .append("\",style=\"solid,filled\",fillcolor=\"#FFFFFF\",color=\"#000000\",shape=ellipse];\n");
        }


        sb.append("\t\tphantom_init_proc_0 [label=\"\\n\",style=\"solid,filled\",fillcolor=\"#FFFFFF\",color=\"#FFFFFF\",shape=ellipse];\n");
        sb.append("\t}\n");


        for (XtaProcess.Loc loc : process.getLocs()) {
            for (XtaProcess.Edge edge : loc.getOutEdges()) {
                StringBuilder edgeLabel = new StringBuilder();


                if (edge.getSync().isPresent()) {
                    edgeLabel.append("(").append(edge.getSync().get().toString()).append(")\\n");
                }


                for (Guard guard : edge.getGuards()) {
                    String gStr = guard.toString().replace("\"", "\\\"");
                    if (gStr.startsWith("(") && gStr.endsWith(")")) {
                        gStr = gStr.substring(1, gStr.length() - 1);
                    }
                    edgeLabel.append("[").append(gStr).append("]\\n");
                }


                for (Update update : edge.getUpdates()) {
                    String uStr = update.toString();


                    if (uStr.contains("__")) continue;


                    if (uStr.matches("\\(reset\\s+(\\w+)\\s+(\\d+)\\)")) {
                        uStr = uStr.replaceAll("\\(reset\\s+(\\w+)\\s+(\\d+)\\)", "assign $1 = $2");
                    }

                    else if (uStr.matches("\\(assign\\s+(\\w+)\\s+([^)]+)\\)")) {
                        uStr = uStr.replaceAll("\\(assign\\s+(\\w+)\\s+([^)]+)\\)", "assign $1 = $2");
                    } else {

                        if (uStr.startsWith("(") && uStr.endsWith(")")) {
                            uStr = uStr.substring(1, uStr.length() - 1);
                        }
                    }
                    edgeLabel.append(uStr).append("\\n");
                }

                sb.append("\t").append(locToId.get(loc)).append(" -> ").append(locToId.get(edge.getTarget()))
                        .append(" [label=\"").append(edgeLabel.toString()).append("\",color=\"#000000\",style=solid];\n");
            }
        }


        sb.append("\tphantom_init_proc_0 -> ").append(locToId.get(process.getInitLoc()))
                .append(" [label=\"\\n\",color=\"#000000\",style=solid];\n");
        sb.append("}\n");

        return sb.toString();
    }


    public static <S> String generateLearnedDfaDot(DFA<S, String> dfa, Iterable<String> alphabet) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph LearnedHypothesis {\n");
        sb.append("\tlabel=\"\";\n");
        sb.append("\tsubgraph cluster_dfa {\n");
        sb.append("\t\tcolor=\"#000000\";\n");
        sb.append("\t\tstyle=solid;\n");
        sb.append("\t\tlabel=\"Learned Timing DFA\\n\";\n");

        Map<S, String> stateToId = new HashMap<>();
        int idCounter = 0;


        for (S state : dfa.getStates()) {
            String stateId = "s" + idCounter++;
            stateToId.put(state, stateId);

            String shape = dfa.isAccepting(state) ? "doublecircle" : "ellipse";
            sb.append("\t\t").append(stateId).append(" [label=\"").append(stateId)
                    .append("\\n\",style=\"solid,filled\",fillcolor=\"#FFFFFF\",color=\"#000000\",shape=").append(shape).append("];\n");
        }

        sb.append("\t\tphantom_init_dfa [label=\"\\n\",style=\"solid,filled\",fillcolor=\"#FFFFFF\",color=\"#FFFFFF\",shape=none,width=0,height=0];\n");
        sb.append("\t}\n");


        for (S state : dfa.getStates()) {
            for (String symbol : alphabet) {
                S target = dfa.getTransition(state, symbol);
                if (target != null) {

                    String shortSymbol = shortenSymbolName(symbol);
                    sb.append("\t").append(stateToId.get(state)).append(" -> ").append(stateToId.get(target))
                            .append(" [label=\"").append(shortSymbol).append("\\n\",color=\"#000000\",style=solid];\n");
                }
            }
        }


        sb.append("\tphantom_init_dfa -> ").append(stateToId.get(dfa.getInitialState()))
                .append(" [label=\"\\n\",color=\"#000000\",style=solid];\n");
        sb.append("}\n");

        return sb.toString();
    }


    private static String shortenSymbolName(String symbol) {
        if (!symbol.contains("->")) {
            return symbol;
        }
        String[] parts = symbol.split("->");
        if (parts.length == 2) {
            return cleanSide(parts[0].trim()) + " -> " + cleanSide(parts[1].trim());
        }
        return symbol;
    }


    private static String cleanSide(String side) {
        String[] tokens = side.split("_");

        if (tokens.length <= 1) return side;

        int startIndex = 0;


        if (tokens.length >= 3 && tokens[0].equals(tokens[1])) {
            startIndex = 2;
        }

        else if (tokens.length >= 2) {
            startIndex = 1;
        }


        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < tokens.length; i++) {
            sb.append(tokens[i]);
            if (i < tokens.length - 1) sb.append("_");
        }
        return sb.toString();
    }
}