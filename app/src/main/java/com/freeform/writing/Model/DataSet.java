package com.freeform.writing.Model;

public class DataSet {
    private String timeStamp;
    private double xAxis, yAxis, zAxis;

    public DataSet(String timeStamp, double xAxis, double yAxis, double zAxis) {
        this.timeStamp = timeStamp;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double getxAxis() {
        return xAxis;
    }

    public void setxAxis(double xAxis) {
        this.xAxis = xAxis;
    }

    public double getyAxis() {
        return yAxis;
    }

    public void setyAxis(double yAxis) {
        this.yAxis = yAxis;
    }

    public double getzAxis() {
        return zAxis;
    }

    public void setzAxis(double zAxis) {
        this.zAxis = zAxis;
    }
}
