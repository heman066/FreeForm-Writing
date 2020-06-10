import math as mt

def calibration(xAxisA,yAxisA,zAxisA,xAxisG,yAxisG,zAxisG,l,check):
	xAxis = []
	yAxis = []
	zAxis = []
	for i in range(0,l):
		x = mt.atan2(yAxisA[i],xAxisA[i])
		p = (yAxisA[i]*yAxisA[i]) + (zAxisA[i]*zAxisA[i])
		y = mt.atan2((-xAxisA[i]),mt.sqrt(p))
		x1 = xAxisG[i] + (yAxisG[i]*(mt.sin(x)) + zAxisG[i]*(mt.cos(x)))*(mt.tan(y))
		y1 = yAxisG[i]*(mt.cos(x)) - zAxisG[i]*(mt.sin(x))
		z1 = (yAxisG[i]*(mt.sin(x)) + zAxisG[i]*(mt.cos(x)))/mt.cos(y)
		xAxis.append(x1)
		yAxis.append(y1)
		zAxis.append(z1)
	if check == 1:
		return xAxis
	elif check == 2:
		return yAxis
	else:
		return zAxis
