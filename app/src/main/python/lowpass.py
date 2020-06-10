from scipy import signal
import numpy as np

def lowpass(x,length):
    xA = []
    for i in range(length):
        xA.append(x[i])
    sf=50
    cf=2
    nf=2*(cf/sf)
    n, d = signal.butter(2,nf,btype='low', analog=False)
    ox = signal.lfilter(n,d,xA)
    return ox

