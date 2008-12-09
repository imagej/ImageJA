// This macro set demonstrates how a tool 
// can be configured by double clicking on it.

    var radius = 10;

    macro "Spot Measure Tool - C00cO11cc" {
        getCursorLoc(x, y, z, flags);
        makeOval(x-radius, y-radius, radius*2, radius*2);
        run("Measure");
    }

    macro "Spot Measure Tool Options" {
        radius = getNumber("Radius: ", radius);
    }
