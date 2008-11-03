// The ImageJ macro language has more than 200 built in functions.
// The run(command, options) function allows you to run any ImageJ
// menu command, over 1100 on this system.

// In addition, you can define your own functions.
// Here are a couple of examples.

  
  print("sum="+sum(2,2));

  for (i=0; i<15; i++) {
      print(leftPad(pow(2, i), 5));
  }

  // Returns the sum of two numbers
  function sum(a1, a2) {
      return a1+a1;
  }

  // Converts 'n' to a string, left padding with zeros
  // so the length of the string is 'width'
  function leftPad(n, width) {
      s =""+n;
      while (lengthOf(s)<width)
          s = "0"+s;
      return s;
  }

