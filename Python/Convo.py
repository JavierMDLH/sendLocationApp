from re import I
import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import numbers 


def triangle_wave (x, c, hc): #Una onda triangular con amplitud hc, ancho c y pendiente hc / 2c
     if x>=c/2:
          r = 0.0
     elif x<=-c/2:
          r = 0.0
     elif x > -c/2 and x<0:
          r=2*x/c*hc+hc
     else:
          r=-2*x/c*hc+hc
     return r

x=np.linspace(-10,10,1000)
y=np.array([triangle_wave(t,2,1.0) for t in x])
y1=np.array([triangle_wave((t-1)/4,2,1.0) for t in x])
plt.ylim(0,1)
plt.subplot(1,2,1)
plt.plot(x,y)
#ejemplo
#ejemplo2
plt.title('Señal triangular original')
plt.subplot(1,9,2)
plt.plot(x,y1)
plt.title('Señal triangular ')
plt.show()