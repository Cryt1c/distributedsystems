Reflect about your solution!


Summary:


I am really content with my solution apart from one small thing:
If you send a calculation to a server right after you closed the node for this calculation, there
won't be an error message returned altough it should. I couldn't figure out why this is the case.
The thread just stops ant the new Socket method in the sendCalc() method in the CloudWorker. 
Apart from this everything is working fine. 
I also didn't get why i should use the ThreadLocal. I have implemented it, but i didn't figured out
why i should. It was working fine before. I hope i will know it until the interview. haha
I could have programed the whole thing more object oriented and therefore extendable i guess. 
I hope that won't be to big of a problem in the next assignment. 
Hopefully i didn't skip anything on the assigment. :(
I really enjoyed programming this thing. So have fun with it. :D

We used the decorator-pattern for the tcp channels, as suggested in the assessment. In Client.java you 
can turn off the Base64 Encoding by setting the variable useBase64 to false.