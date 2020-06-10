from android.os import Environment
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
def graph(x,y,z,length,name):
    d = str(Environment.getExternalStorageDirectory())
    n = str(name)
    x1=[]
    y1=[]
    z1=[]
    for i in range(length):
        x1.append(x[i])
        y1.append(y[i])
        z1.append(z[i])
    my_dpi= 96
    pixel= 256
    fig = plt.figure(figsize=(pixel/my_dpi, pixel/my_dpi), dpi=my_dpi)
    ax = fig.gca(projection='3d')
    ax.plot(z1, y1, x1, c='k') #your data list (z,y,x)
    ax.view_init(-90,140)
    ax.grid(False)
    ax.set_xticks([])
    ax.set_yticks([])
    ax.set_zticks([])
    plt.axis('off')
    plt.savefig(d + '/FreeForm-Writing/Graphs/' + n, transparent=True) #filename of the saved file



    #plt.plot(x1,y1)
    #plt.savefig(d + '/FreeForm-Writing/Graphs/' + n)
    plt.show()