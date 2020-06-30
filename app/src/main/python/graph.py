from android.os import Environment
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
from numpy import loadtxt
from numpy import savetxt
def graph(x1,y1,z1,name,direc,totalSeg,inputDate):
    d = str(Environment.getExternalStorageDirectory())
    n = str(name)
    dir = str(direc)

    my_dpi= 96
    pixel= 256
    fig = plt.figure(figsize=(pixel/my_dpi, pixel/my_dpi), dpi=my_dpi)
    ax = fig.gca(projection='3d')
    ax.plot(z1, y1, x1, c='k' , linewidth = 3) #your data list (z,y,x)

    if totalSeg != 1:
        x = []
        y = []
        z = []
        for i in range(1 , totalSeg):
            data = loadtxt(d + "/FreeForm-Writing/." + inputDate + "/Seg/" + str(i) + "Seg.csv",ndmin=2,delimiter=",")
            #savetxt(d + "/FreeForm-Writing/." + inputDate + "/.Working/" + str(i) + "Seg.csv",data,delimiter=",")
            x.extend(data[:,0].tolist())
            y.extend(data[:,1].tolist())
            z.extend(data[:,2].tolist())
        ax.plot(z , y , x , c='r' , linewidth = 3) #penupDown segment

    ax.view_init(-90,140)
    ax.grid(False)
    ax.set_xticks([])
    ax.set_yticks([])
    ax.set_zticks([])
    plt.axis('off')
    plt.savefig( dir + '/' + n, transparent=True) #filename of the saved file
    plt.show()