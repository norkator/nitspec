import java.util.ArrayList;

public class Test1 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<Double> results = new ArrayList();
		results.add(getProjectileYCorrectionDegrees(	80.0, 		4.0, 	350.00, 	0.330, 	1013.25, 	24.0, 	4.76, 	4.76, 	0.015	));
		results.add(getProjectileYCorrectionDegrees(	120.0, 		4.0, 	350.00, 	0.330, 	1013.25, 	24.0, 	4.76, 	4.76, 	0.015	));
		results.add(getProjectileYCorrectionDegrees(	300.0, 		4.0, 	350.00, 	0.330, 	1013.25, 	24.0, 	4.76, 	4.76, 	0.015	));
		results.add(getProjectileYCorrectionDegrees(	800.0, 		4.0, 	350.00, 	0.330, 	1013.25, 	24.0, 	4.76, 	4.76, 	0.015	));
		results.add(getProjectileYCorrectionDegrees(	900.0, 		4.0, 	350.00, 	0.330, 	1013.25, 	24.0, 	4.76, 	4.76, 	0.015	));
		results.add(getProjectileYCorrectionDegrees(	1000.0, 	4.0, 	350.00, 	0.330, 	1013.25, 	24.0, 	4.76, 	4.76, 	0.015	));
		results.add(getProjectileYCorrectionDegrees(	1500.0, 	4.0, 	350.00, 	0.330, 	1013.25, 	24.0, 	4.76, 	4.76, 	0.015	));
		System.out.println("##########################################");
		for (int i = 0; i < results.size(); i++) {
			System.out.println(results.get(i) + "°");
		}
	}
	
	
	

	/**
     * Projectile calculations (development)
     */
	public static Double getProjectileYCorrectionDegrees(
				final Double targetRange, final Double initialAngle, final Double ammunitionSpeed, final Double ammunitionWeight, final Double airPressureHehtoPascals,
				final Double temperatureCelsius, final Double ammunitionSizeXMillis, final Double ammunitionSizeYMillis, final Double ammunitionDragCoefficient
			)  
	{
		System.out.println("-------------------------------------------");
		
		
		// Calculate height difference
		final Double hDifference = getHeightDifference(initialAngle, targetRange);
		System.out.print("Height difference: "); System.out.printf("%.10f", hDifference); System.out.println(" m");

		
		final Double horizontalDistance = getHorizontalDistance(initialAngle, targetRange);
		System.out.print("Horizontal distance: "); System.out.printf("%.10f", horizontalDistance); System.out.println(" m");
		// System.out.print("Sin: "); System.out.printf("%.10f", Math.sin(angle)); System.out.println("");
		
        
		final Double gravityAcceleration = getGravitationalAcceleration(); 	// m/s gravitational acceleration
        final Double ammunitionMass = gramsToKilograms(ammunitionWeight); // Divide by 1000.0 to get it into kilograms
        final Double Cv = 0.015; // Projectile drag coefficient. Dimensionless and cannot be determined algebraically. It can only be determined through trial and error, or a reference sheet.
        //final Double Ch = 0.015; // Projectile drag coefficient. Dimensionless and cannot be determined algebraically. It can only be determined through trial and error, or a reference sheet.
        
        //System.out.print("Mass: "); System.out.printf("%.6f", m); System.out.println(" kg");
        
        final Double p = getFluidDensity(airPressureHehtoPascals, temperatureCelsius);
        System.out.println("Fluid density: " + p + " kg/m^3");
        
        final Double Av = sizeSquareArea(ammunitionSizeXMillis, ammunitionSizeYMillis); // m²
        //final Double Ah = sizeCircleArea(ammunitionSizeYMillis/2); // m²        
        //System.out.print("Proj surface area: "); System.out.printf("%.9f", Av); System.out.println("");
        
        
        final Double ammunitionDrag = getAmmunitionDragForce(p, Av, Cv);
        System.out.print("Ammunition drag: "); System.out.printf("%.10f", ammunitionDrag); System.out.println("");
        
        
        // Calculate compensation 
        final double compensationAngle = getCompensationAngle(gravityAcceleration, horizontalDistance, ammunitionSpeed, hDifference, initialAngle, ammunitionDrag);

        System.out.println("-----");
        System.out.println("Compensation: " + compensationAngle + " degrees");
        return compensationAngle;
	}
	
	
	
    // ---------------------------------
    /* Math helper functions */
	
	
	/**
	 * Return height difference from shooter to target based on target select angle and it's range
	 * @param angle target select angle
	 * @param range range measured with laser range finder
	 * @return return height difference in meters
	 */
	private static Double getHeightDifference(final Double degreeAngle, final Double range) {
		double angle = (degreeAngle/180.0d)*Math.PI;
		//System.out.println(Math.sin(angle)); // Debug
		return range * Math.sin(angle);
    }
	
	
	/**
	 * 
	 * @param degreeAngle
	 * @param range
	 * @return
	 */
	private static Double getHorizontalDistance(final Double degreeAngle, final Double range) {
		double angle = (degreeAngle/180.0d)*Math.PI;
		return range * Math.cos(angle);
	}
	
	
	 /**
	  * Returns conpensation angle which is added to initial angle
	  * @param g Gravitation constant
	  * @param r Horizontal distance
	  * @param v Ammunition speed (velocity)
	  * @param h Height difference
	  * @param ia Initial angle
	  * @param ad Ammunition drag
	  * @return output angle
	  */
	private static Double getCompensationAngle(final Double g, final Double r, final Double v, final Double h, final Double ia, final Double ad) {
		final double a = g * r / Math.pow(v, 2);
		final double b = -(2 * r);
		final double c = a + 2*h;
		final double tanAngle = (-b - Math.sqrt((Math.pow(b, 2)-4*a*c)))/(2*a);
		
		final double angle = Math.atan(tanAngle);	
		//System.out.println(Math.toDegrees(angle));
		
		final double initialAngle = (ia / 180.0d) * Math.PI;
        return Math.toDegrees(angle-initialAngle);
    }
	
	
	/**
     * Returns fluid density kg/m^3 for given variables
     * @param airPressureHehtoPascals hehto pascals
     * @param temperatureCelsius celsius
     * @return value p
     */
	private static Double getFluidDensity(
            final Double airPressureHehtoPascals,
            final Double temperatureCelsius)
    {
        final Double p = airPressureHehtoPascals * 100; // To Pascal's
        final Double R = 287.05; // Specific gas constant for dry air (needs development)
        final Double T = temperatureCelsius + 273.15; // °C to kelvin's for this calculation

        return p / (R * T); // KG/m3 density of the fluid through which the projectile is launched
    }


    /**
     * Returns the inverse hyperbolic cosine
     * @param x Double input value x
     * @return arccosh
     */
	private static Double arccosh(final Double x) {
        return Math.log( x + Math.sqrt( Math.pow(x, 2) - 1 ) );
    }


    /**
     * return the front circle area of bullet
     * @param rMillimeters
     * @return
     */
	private static double sizeCircleArea(final Double rMillimeters) {
		double pi = Math.PI;
		return (0.5d * pi * (rMillimeters * rMillimeters) ) / 1000000;
	}
	
	
	/**
     * Convert x mm, y mm to mm² and to m²
     * @param xMillimeters x size millis
     * @param yMillimeters y size millis
     * @return m² (square meters, SI standard)
     */
	private static Double sizeSquareArea(final Double xMillimeters, final Double yMillimeters) {
        return (xMillimeters * yMillimeters) / 1000000;
    }


    /**
     * Calculate ammunition drag force on fluid using ammunition properties
     * @param p Density of the fluid through which the projectile is launched. SI unit for density is kg/m^3 (kg/cubic meter).
     * @param Av Projected surface area of the projectile in the y direction. SI is the square meter.
     * @param Cv drag coefficient of the projectile in the y direction. Dimensionless and cannot be determined algebraically. It can only be determined through trial and error, or a reference sheet.
     * @return kv
     */
	private static Double getAmmunitionDragForce(final Double p, final Double Av, final Double Cv) {
        return 1/2d * p * Av * Cv;
    }


    /**
     * Convert grams to kilograms
     * @param g grams
     * @return kilograms
     */
	private static Double gramsToKilograms(final Double g) {
        return g / 1000.0d;
    }


    /**
     * Return scientific gravitational accelerant constant value
     * @return m/s gravity constant
     */
	private static Double getGravitationalAcceleration() {
        return 9.81; // m/s gravitational acceleration
    }
	
	
	// ---------------------------------------------------------------------------------------------
	
	
	
	// OLD STUFF
    /*

	//final Double n = Math.PI / 180 * a;
    //System.out.print("n: "); System.out.printf("%.20f", n); System.out.println("");
    
    
   
    // final double r = targetRange; // Range in meters
          
    
    // final Double b1 =  Math.sqrt( m/(g*kv) ) * Math.atan( v * ( Math.sin(n)) * Math.sqrt( kv / (m*g) ) ) ; // Original
    
    
    
    Double degrees = Math.toDegrees( ( 1/2d * ( Math.asin( r * g / Math.pow(v, 2) ) ) ) );
    System.out.println("Dgrs, no drag: " + degrees);
    
    
    Double degrees2 = Math.toDegrees( ( 1/2d * ( Math.asin( r * g * b1 / Math.pow(v, 2) ) ) ) );
    System.out.println("Dgrs:          " + degrees2);
    */
	
	
	
	
} // End of class