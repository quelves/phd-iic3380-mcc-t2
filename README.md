# phd-iic3380-mcc-t2
Prueba de concepto para code offloading

## Configure x86 Server

### IP For

'''js
nc -kl 6000 0< backpipe | nc 127.0.0.1 5555 > backpipe &
'''


for vt-arm

adb shell
pm list packages | grep edu
pm uninstall -k t2.mcc.iic3380.puc.edu.vt_arm

exit

adb install -r vt-arm-apk

port 9000 upload files
port 3000 astral app
port 22 sh



./adb shell pm set-install-location 2
./adb shell pm get-install-location


8:31:46 PM tus.1    |  Received TUS request
8:31:46 PM tus.1    |  [TusServer] handle: HEAD /files/52383bb0e5cdedf87d92a650fa09fb00
8:31:47 PM tus.1    |  Received TUS request
8:31:47 PM tus.1    |  [TusServer] handle: PATCH /files/52383bb0e5cdedf87d92a650fa09fb00
8:31:47 PM tus.1    |  [FileStore] write: 0 bytes written to files/52383bb0e5cdedf87d92a650fa09fb00
8:31:47 PM tus.1    |  [FileStore] write: File is now 4526456 bytes

