/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package YaraParser.TransitionBasedSystem.Configuration;

public class BeamElement implements Comparable<BeamElement> {
    public float score;
    public int number;
    public int action;
    public int label;

    public BeamElement(float score, int number, int action, int label) {
        this.score = score;
        this.number = number;
        this.action = action;
        this.label = label;
    }

    @Override
    public int compareTo(BeamElement beamElement) {
        float diff = score - beamElement.score;
        if (diff > 0)
            return 2;
        if (diff < 0)
            return -2;
        if (number != beamElement.number)
            return beamElement.number - number;
        return beamElement.action - action;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }
}
