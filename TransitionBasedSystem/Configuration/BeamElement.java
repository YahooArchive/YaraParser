/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package TransitionBasedSystem.Configuration;

public class BeamElement implements  Comparable<BeamElement>{
    public float score;
    public int number;
    public int action;
    public String label;

    public BeamElement(float score, int number,int action,String label){
        this.score=score;
        this.number=number;
        this.action=action;
        this.label=label;
    }

    @Override
    public int compareTo(BeamElement beamElement) {
        float diff=score-beamElement.score;
        if(diff>0)
            return 1;
        if (diff<0)
            return -1;
        return 0;
    }

    @Override
    public boolean equals(Object o){
        return false;
    }
}
