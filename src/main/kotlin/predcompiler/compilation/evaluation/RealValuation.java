package predcompiler.compilation.evaluation;

import java.util.*;

import rabinizer.bdd.BDDForVariables;
import rabinizer.formulas.*;
import net.sf.javabdd.*;

/**
 * An adaptation of the Valuation class from the Rabinizer3 library to enable
 * representations of real-valued atomic predicates (i.e. supporting quantitative
 * semantics instead of purely qualitative ones).
 */
public class RealValuation extends HashMap<Integer, Float> {

    /**
	 * Automated serial version ID
	 */
	private static final long serialVersionUID = 2889213641057348217L;

    public RealValuation() {
        super();
    }
    
    public RealValuation(int n) {
        super();
        for (int i = 0; i < n; i++) {
            this.put(i, 0f);
        }
    }

    public RealValuation(float[] values) {
        super();
        for (int i = 0; i < values.length; i++) {
            this.put(i, values[i]);
        }
    }

    public RealValuation(List<Float> values) {
        super();
        for (int i = 0; i < values.size(); i++) {
            this.put(i, values.get(i));
        }
    }
    
    public RealValuation set(int var, float value) {
        this.put(var, value);
        return this;
    }

    private String strValuation = null;

    public String toString() {
        if (strValuation == null) {
            strValuation = "{";
            boolean first = true;
            for (Map.Entry<Integer, Float> e : this.entrySet()) {
                if (first) {
                    String v = BDDForVariables.bijectionIdAtom.atom(e.getKey().intValue());
                    if (v == null) {
                        strValuation = strValuation + "v" + e.getKey().intValue() + "[" + e.getValue().floatValue() + "]";
                    } else {
                        strValuation = strValuation + v + "[" + e.getValue().floatValue() + "]";
                    }
                    first = false;
                } else {
                    String v = BDDForVariables.bijectionIdAtom.atom(e.getKey().intValue());
                    if (v == null) {
                        strValuation = strValuation + (", v" + e.getKey().intValue() + "[" + e.getValue().floatValue() + "]");
                    } else {
                        strValuation = strValuation + (", " + v + "[" + e.getValue().floatValue() + "]");;
                    }

                }
            }
            strValuation = strValuation + "}";
        }
        return strValuation;
    }

    public Formula toFormula() {
        Formula result = null;
        for (Map.Entry<Integer, Float> e : this.entrySet()) {
            Literal l = new Literal(BDDForVariables.bijectionIdAtom.atom(e.getKey()), e.getKey(), e.getValue() <= 0f);
            if (result == null) {
                result = l;
            } else {
                result = new Conjunction(result, l);
            }
        }
        return result;
    }

    public BDD toFormulaBDD() {
        BDD result = BDDForVariables.getTrueBDD();  // BDD for True
        for (Map.Entry<Integer, Float> e : this.entrySet()) {
            Literal l = new Literal(BDDForVariables.bijectionIdAtom.atom(e.getKey()), e.getKey(), e.getValue() <= 0f);
            result = result.and(l.bdd());
        }
        return result;
    }

    public BDD toValuationBDD() {
        BDD result = BDDForVariables.getTrueBDD();  // BDD for True
        for (Integer i : this.keySet()) {
            if (this.get(i) > 0f) {
                result = result.and(BDDForVariables.variableToBDD(i));
            } else {
                result = result.and(BDDForVariables.variableToBDD(i).not());
            }
        }
        return result;
    }

}
