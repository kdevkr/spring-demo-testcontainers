logtime:{("T"sv string("d"$x;"t"$x))};

-1 logtime[.z.P]," [INFO] ","KDB+ Version: ",string .z.K;
-1 logtime[.z.P]," [INFO] ","KDB+ ProcessID: ",string .z.i;
-1 logtime[.z.P]," [INFO] ","KDB+ License: "," " sv .z.l;

.f.filesize:{("B";"KB";"MB";"GB";"TB")[i]{y,x}'.Q.f[2] each x%l i:(l:-1 1024,`long$1024 xexp 2 3 4) bin x}
.f.toEpoch:{{`long$x%1e6}x - `timestamp$1970.01.01}
.f.toUnixTimestamp:{floor((`long$x)-`long$"P"$"1970.01.01")%1e6}
.f.toTimestamp:{1970.01.01+0D00:00:00.001*x}

 -1"loaded q.q after bootstraping q.k.";
