package com.freeform.writing.Model;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;

public class GraphView {
    private LineData lineData;

    public LineData getLineData() {
        return lineData;
    }

    public void setLineData(LineData lineData) {
        this.lineData = lineData;
    }

    public GraphView(LineData lineData) {
        this.lineData = lineData;
    }
}
