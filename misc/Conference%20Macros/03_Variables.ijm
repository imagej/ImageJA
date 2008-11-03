// The ImageJ macro language is "typeless". Variables 
// do not need to be declared and do not have explicit 
// data types. They are automatically initialized when used 
// in an assignment statement. A variable can contain a 
// number, a string or an array. In fact, the same variable 
// can be any of these at different times.

n = 123;

s = "10";

s2 = "cat";

print(n);  // numbers automatically converted to strings [IJ.log(""+n)]

print("n/s: ", n/s); // strings automatically converted to numbers

print("n/s2: ", n/s2); // NaN ("cat" can't be converted to a number)

//print("s/n: ", s/n); // can't always convert strings to numbers

print("log(s):", log(s)); // 2.3026

print("s+n: ", s+n);  // 10123

print("n+s: ", n+s); // 12310 (??)

// use parseInt() and parseFloat() to explicity
// convert numbers to strings

print("n+parseInt(s): ", n+parseInt(s)); // 133
print(parseInt("12.34"));  // 12
print(parseFloat("12.34")); // 12.34
print(parseFloat("ImageJ")); // NaN
