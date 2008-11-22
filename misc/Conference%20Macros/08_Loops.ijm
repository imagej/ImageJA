// The macro language has three looping statements:
//     for - runs a block of code a specified number of times
//     while - repeatedly runs a block of code while a condition is true
//     do...while - runs a block of code once then repeats while a condition is true

// Each of these loops prints 0 to 40, incrementing by 10

   print("for");
   for (i=0; i<5; i++) {
      j = 10*i;
      print(j);
   }

   print("\nwhile");
   i = 0;
   while (i<50) {
      print(i);
      i = i+10;
  }

   print("\ndo...while");
   i = 0;
   do {
      print(i);
      i += 10;
   } while (i<50);
