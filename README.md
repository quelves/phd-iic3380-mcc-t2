# phd-iic3380-mcc-t2
Prueba de concepto para code offloading

## Configure x86 Server

### IP For

'''js
nc -kl 6000 0< backpipe | nc 127.0.0.1 5555 > backpipe &
'''
