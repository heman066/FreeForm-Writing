package com.freeform.writing.Model;

import java.util.List;

public class SegmentWithPenUpDown {
    private long startTime, endTime;
    private List<Segment> penUpDownSegment;

    public SegmentWithPenUpDown(long startTime, long endTime, List<Segment> penUpDownSegment) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.penUpDownSegment = penUpDownSegment;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<Segment> getPenUpDownSegment() {
        return penUpDownSegment;
    }

    public void setPenUpDownSegment(List<Segment> penUpDownSegment) {
        this.penUpDownSegment = penUpDownSegment;
    }
}
