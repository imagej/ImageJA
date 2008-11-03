// Use the macro language as a calculator

  print(2+2);
  2+2; //print() is assumed
  sqrt(2)  // 4 decimal places
  pow(0.11, 6)  // or scientific notation
  d2s(pow(0.11, 6), 9); // "Double To String"

  3000*2400*4/(1024*1024)
  d2s(3000*2400*4/(1024*1024),1)+'MB'

  exchangeRate = 1.18;
  "$"+d2s(100/exchangeRate, 2)

  print(0/0);  // NaN (Not-A-Number)
  print(1/0);  // Infinity
  print(-1/0);  // -Infinity

  print(isNaN(0/0)); // use isNaN() to detect NaNs
  print(true, false);  // true=1, false=0
