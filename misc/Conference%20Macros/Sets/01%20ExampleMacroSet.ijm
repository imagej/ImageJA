// This is a macro set, related macros that that can share 
// global variables and be assigned keyboard shortcuts.

  var n =10;  // global variable

  macro 'Set... [1]' {
  	n = getNumber("n:", n);
  }

  macro 'Increment [2]' {
      showStatus(++n);
  }

  macro 'Decrement [3]' {
      showStatus(--n);
  }

  macro 'Square [4]' {
      n=n*n; showStatus(n);
  }

  macro 'Sqrt [5]' {
      n=sqrt(n); showStatus(n);
  }

