import numpy as np
from numpy import loadtxt
from numpy import savetxt
from sklearn.cluster import KMeans
import pandas as pd
from sklearn.metrics import silhouette_score as score
from android.os import Environment

def Clustering(inputDate):
    dir = str(Environment.getExternalStorageDirectory())
    file = loadtxt(dir + "/FreeForm-Writing/." + inputDate + "/.Working/inputData.csv",delimiter=",")
    inData = file.reshape(-1,1)

    y = KMeans(n_clusters=2).fit(inData)
    scr = score(inData,y.labels_)

    cluster_map = pd.DataFrame()
    cluster_map['data_index'] = file
    cluster_map['cluster'] = y.labels_

    c0 = [item[0] for item in cluster_map[cluster_map.cluster == 0].values.tolist()]
    c1 = [item[0] for item in cluster_map[cluster_map.cluster == 1].values.tolist()]
    if len(c0) > len(c1):
        clust0 = c1
    else:
        clust0 = c0

    if(len(clust0) > 5):
        inData1 = np.array(clust0).reshape(-1,1)
        y1 = KMeans(n_clusters=2).fit(inData1)
        scr1 = score(inData1,y1.labels_)
        cluster_map1 = pd.DataFrame()
        cluster_map1['data_index'] = clust0
        cluster_map1['cluster'] = y1.labels_

        cl0 = [item[0] for item in cluster_map1[cluster_map1.cluster == 0].values.tolist()]
        cl1 = [item[0] for item in cluster_map1[cluster_map1.cluster == 1].values.tolist()]

        if scr > scr1:
            output = clust0
        else:
            if len(cl0) > len(cl1):
                output = cl1
            else:
                output = cl0
    else:
        output = clust0

    savetxt(dir + "/FreeForm-Writing/." + inputDate + "/.Working/outputData.csv",output,delimiter=",")
    return