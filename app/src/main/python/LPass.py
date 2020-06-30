from scipy import signal
import numpy as np
from numpy import loadtxt
from numpy import savetxt

def LPass(sTimes,eTimes,file,save):
    out = []
    filePath = str(file)
    saveDir = str(save)
    a = loadtxt(filePath,delimiter=",")
    times = np.array(a[:,0])
    xAxis = np.array(a[:,1])
    yAxis = np.array(a[:,2])
    zAxis = np.array(a[:,3])
    segLen = len(sTimes)
    sf=50
    cf=2
    nf=2*(cf/sf)
    n, d = signal.butter(2,nf,btype='low', analog=False)

    i = 0
    seg = 0
    x = []
    y = []
    z = []
    while i < len(xAxis):
        j = i
        while sTimes[seg] > times[j]:
            out.append([times[j] , xAxis[j] , yAxis[j] , zAxis[j]])
            j = j + 1
        sInd = j
        while eTimes[seg] >= times[j]:
            x.append(xAxis[j])
            y.append(yAxis[j])
            z.append(zAxis[j])
            j = j + 1
        length = len(x)
        ox = signal.lfilter(n,d,x)
        oy = signal.lfilter(n,d,y)
        oz = signal.lfilter(n,d,z)
        x.clear()
        y.clear()
        z.clear()
        for k in range(length):
            out.append([times[sInd] , ox[k] , oy[k] , oz[k]])
            sInd = sInd + 1
        seg = seg + 1
        i = j
        if seg == segLen:
            for k in range(i,len(xAxis)):
                out.append([times[k] , xAxis[k] , yAxis[k] , zAxis[k]])
            break
    savetxt(saveDir,out,delimiter=",")
    return