// Generate a sine/cosine table and displays 
// it in the "Results" window.

    run("Clear Results");
    row = 0;
    for (n=0; n<=2*PI; n += 0.1) {
        setResult("n", row, n);
        setResult("Sine(n)", row, sin(n));
        setResult("Cos(n)", row, cos(n));
        row++;
    }
    updateResults();
