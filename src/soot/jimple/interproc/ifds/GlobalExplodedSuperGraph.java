package soot.jimple.interproc.ifds;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class GlobalExplodedSuperGraph <Method, Stmt, Fact, Value, I extends InterproceduralCFG<Stmt, Method>> {
    private Map<Key, ExplodedSuperGraph<Method, Stmt, Fact, Value>> methodToESG = new HashMap<>();
    private Map<Object, Integer> objectToInteger = new HashMap<>();
    private I icfg;
    private Logger logger = Logger.getGlobal();

    public GlobalExplodedSuperGraph(I icfg) {
        this.icfg = icfg;
    }

    public ExplodedSuperGraph<Method, Stmt, Fact, Value> getOrCreateESG(Method method) {
        logger.warning(method.toString());
        ExplodedSuperGraph<Method, Stmt, Fact, Value> ESG = methodToESG.get(new Key(method));
        if (ESG == null) {
            ESG = new ExplodedSuperGraph<>(method);
            logger.warning("Create ESG for method: " + method);
            methodToESG.put(new Key(method), ESG);
        }
        return ESG;
    }

    public void normalFlow(Stmt start, Fact startFact, Stmt target, Fact targetFact) {
        getOrCreateESG(icfg.getMethodOf(start)).normalFlow(start, startFact, target, targetFact);
    }

    private class Key {
        final Method m;

        private Key(Method m) {
            this.m = m;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m == null) ? 0 : m.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (m == null) {
                if (other.m != null)
                    return false;
            } else if (!m.equals(other.m))
                return false;
            return true;
        }
    }

    public Integer id(Object u) {
        if (objectToInteger.get(u) != null)
            return objectToInteger.get(u);
        int size = objectToInteger.size() + 1;
        objectToInteger.put(u, size);
        return size;
    }

    private JSONObject toJSONObject(ExplodedSuperGraph<Method, Stmt, Fact, Value> esg) {
        esg.linkSummaries(icfg);
        JSONObject o = new JSONObject();
        o.put("methodName", StringEscapeUtils.escapeHtml4(esg.method.toString()));
        o.put("methodId", id(esg.method));
        JSONArray data = new JSONArray();
        LinkedList<Stmt> stmtsList = new LinkedList<>();
        int offset = 0;
        int labelYOffset = 0;
        int charSize = 8;
        for (Fact g : esg.getFacts()) {
            labelYOffset = Math.max(labelYOffset, charSize * g.toString().length());
        }
        int index = 0;
        for (Stmt u : getListOfStmts(esg.method)) {
            JSONObject nodeObj = new JSONObject();
            JSONObject pos = new JSONObject();
            stmtsList.add(u);
            pos.put("x", 10);
            pos.put("y", stmtsList.size() * 30 + labelYOffset);
            nodeObj.put("position", pos);
            JSONObject label = new JSONObject();
            label.put("label", u.toString());
            label.put("shortLabel", getShortLabel(u));
            if (icfg.isCallStmt(u)) {
                label.put("callSite", icfg.isCallStmt(u));
                JSONArray callees = new JSONArray();
                for (Method callee : icfg.getCalleesOfCallAt(u)){
                    if(callee != null && callee.toString() != null && esg != null){
                        callees.add(new JSONMethod(callee));
                    }
                }
                label.put("callees", callees);
            }
            if (icfg.isExitStmt(u)) {
                label.put("returnSite", icfg.isExitStmt(u));
                JSONArray callees = new JSONArray();
                Set<Method> callers = new HashSet<>();
                for (Stmt callsite : icfg.getCallersOf(icfg.getMethodOf(u))) {
                    Method csMethod = icfg.getMethodOf(callsite);
                    if (csMethod == null) {
                        continue;
                    }
                    callers.add(csMethod);
                }

                for (Method caller : callers) {
                    callees.add(new JSONMethod(caller));
                }
                label.put("callers", callees);
            }
            label.put("stmtId", id(u));
            label.put("id", "stmt" + id(u));

            label.put("stmtIndex", index);
            index++;

            nodeObj.put("data", label);
            nodeObj.put("classes", "stmt label " + (icfg.isExitStmt(u) ? " returnSite " : " ")
                    + (icfg.isCallStmt(u) ? " callSite " : " ") + " method" + id(esg.method));
            data.add(nodeObj);
            offset = Math.max(offset, getShortLabel(u).toString().length());

            for (Stmt succ : icfg.getSuccsOf(u)) {
                JSONObject cfgEdgeObj = new JSONObject();
                JSONObject dataEntry = new JSONObject();
                dataEntry.put("source", "stmt" + id(u));
                dataEntry.put("target", "stmt" + id(succ));
                dataEntry.put("directed", "true");
                cfgEdgeObj.put("data", dataEntry);
                cfgEdgeObj.put("classes", "cfgEdge label method" + id(esg.method));
                data.add(cfgEdgeObj);
            }
        }

        LinkedList<Fact> factsList = new LinkedList<>();
        // System.out.println("Number of facts:\t" + esg.getFacts().size());
        for (Fact u : esg.getFacts()) {
            JSONObject nodeObj = new JSONObject();
            JSONObject pos = new JSONObject();
            factsList.add(u);
            pos.put("x", factsList.size() * 30 + offset * charSize);
            pos.put("y", labelYOffset);
            nodeObj.put("position", pos);
            JSONObject label = new JSONObject();
            label.put("label", u.toString());
            label.put("factId", id(u));
            nodeObj.put("classes", "fact label method" + id(esg.method));
            nodeObj.put("data", label);
            data.add(nodeObj);
        }

        // System.out.println("Number of nodes:\t" + esg.getNodes().size());
        for (ExplodedSuperGraph<Method, Stmt, Fact, Value>.ESGNode node : esg.getNodes()) {
            JSONObject nodeObj = new JSONObject();
            JSONObject pos = new JSONObject();
            if (node instanceof ExplodedSuperGraph.CalleeESGNode) {
                ExplodedSuperGraph.CalleeESGNode calleeESGNode = (ExplodedSuperGraph.CalleeESGNode) node;
                pos.put("x", (factsList.indexOf(calleeESGNode.linkedNode.a) + 1) * 30 + 10 + offset * charSize);
                pos.put("y",
                        (stmtsList.indexOf(calleeESGNode.linkedNode.u))
                                * 30 + labelYOffset);
            } else {
                assert stmtsList.indexOf(node.u) != -1;
                pos.put("x", (factsList.indexOf(node.a) + 1) * 30 + offset * charSize);
                pos.put("y",
                        (stmtsList.indexOf(node.u)) * 30 + labelYOffset);
            }

            nodeObj.put("position", pos);
            String classes = "esgNode method" + id(esg.method);

            JSONObject additionalData = new JSONObject();
            additionalData.put("id", "n" + id(node));
            additionalData.put("stmtId", id(node.u));
            additionalData.put("factId", id(node.a));
            if (esg.getIDEValue(node) != null)
                additionalData.put("ideValue", StringEscapeUtils.escapeHtml4(esg.getIDEValue(node).toString()));
            nodeObj.put("classes", classes);
            nodeObj.put("group", "nodes");
            nodeObj.put("data", additionalData);

            data.add(nodeObj);
        }
        Multimap<Stmt, Object> stmtToInfo = esg.getInformationPerStmt();
        for (Stmt stmt : stmtToInfo.keySet()) {
            int numberOfInfos = 1;
            for (Object info : stmtToInfo.get(stmt)) {
                JSONObject nodeObj = new JSONObject();
                JSONObject pos = new JSONObject();
                pos.put("x", -numberOfInfos * 30);
                pos.put("y", (stmtsList.indexOf(stmt) * 30 + labelYOffset));
                nodeObj.put("position", pos);
                String classes = "esgNode additional information method" + id(esg.method);

                JSONObject additionalData = new JSONObject();
                additionalData.put("stmtId", id(stmt));
                additionalData.put("ideValue", StringEscapeUtils.escapeHtml4(info.toString()));
                nodeObj.put("classes", classes);
                nodeObj.put("group", "nodes");
                nodeObj.put("data", additionalData);
                data.add(nodeObj);
            }
        }
        // System.out.println("Number of edges:\t" + esg.getEdges().size());
        for (ExplodedSuperGraph<Method, Stmt, Fact, Value>.ESGEdge edge : esg.getEdges()) {
            JSONObject nodeObj = new JSONObject();
            JSONObject dataEntry = new JSONObject();
            dataEntry.put("id", "e" + id(edge));
            dataEntry.put("source", "n" + id(edge.start));
            dataEntry.put("target", "n" + id(edge.target));
            dataEntry.put("directed", "true");
            nodeObj.put("data", dataEntry);
            nodeObj.put("classes", "esgEdge method" + id(esg.method) + " " + edge.labels);
            nodeObj.put("group", "edges");
            data.add(nodeObj);
        }
        o.put("data", data);
        return o;
    }

    public String getShortLabel(Stmt u) {
        return u.toString();
    }

    public List<Stmt> getListOfStmts(Method method) {
        LinkedList<Stmt> worklist = new LinkedList<>();
        worklist.addAll(icfg.getStartPointsOf(method));
        Set<Stmt> visited = new HashSet<>();
        LinkedList<Stmt> result = new LinkedList<>();

        while (!worklist.isEmpty()) {
            Stmt curr = worklist.pollFirst();
            if (visited.contains(curr))
                continue;
            visited.add(curr);
            result.add(curr);
            for (Stmt succ : icfg.getSuccsOf(curr)) {
                worklist.add(succ);
            }
        }
        return result;
    }

    public void writeToFile(File jsonFile) {
        try (FileWriter file = new FileWriter(jsonFile)) {
            JSONArray methods = new JSONArray();
            Set<Method> visitedMethods = new HashSet<>();
            for (ExplodedSuperGraph<Method, Stmt, Fact, Value> c : methodToESG.values()) {
                if(c.getEdges().isEmpty())
                    continue;
                if (visitedMethods.add(c.method)) {
                    JSONObject method = new JSONObject();
                    method.put("name", StringEscapeUtils.escapeHtml4(c.method.toString()));
                    method.put("id", id(c.method));
                    methods.add(method);
                }
            }
            for (Method m : visitedMethods) {
                getOrCreateESG(m);
            }
            JSONArray explodedSupergraphs = new JSONArray();
            for (ExplodedSuperGraph<Method, Stmt, Fact, Value> c : methodToESG.values()) {
                explodedSupergraphs.add(toJSONObject(c));
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("methodList", methods);
            jsonObject.put("explodedSupergraphs", explodedSupergraphs);

            file.write(jsonObject.toJSONString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class JSONMethod extends JSONObject {

        JSONMethod(Method m) {
            this.put("name", StringEscapeUtils.escapeHtml4(m.toString()));
            this.put("id", id(m));
        }
    }
}
