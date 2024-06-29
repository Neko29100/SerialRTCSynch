# SerialRTCSynch
A way to synchronize any RTC to the exact time without having to deal with the annoying 10s delay that the usual synch scripts causes, and removes the need for a NTP connection to grab the actual time. Just synchronize and you're good to go!

Will add sample code for the receiving end, but basically: Wait for serial to receive string of data (Serial.available > 0) and convert that string into an array of ints, and then synchronize your RTC with those values. String format is the following : yy, M, d, H, m, s Note that you will need to add 2000 years to the year values.