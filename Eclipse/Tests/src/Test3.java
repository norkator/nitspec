import java.util.ArrayList;

public class Test3 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		getProjectileYCorrectionDegrees();
	}
	
	
	

	/**
     * Projectile calculations (development)
     * Based on: https://www.desmos.com/calculator/on4xzwtdwz
     * https://hardairmagazine.com/ballistic-coefficients/
     */
	public static void getProjectileYCorrectionDegrees()  
	{
		
		// Variables
		final Double r 	= 500.0; // Target range meters
		final Double v  = 350.00; // Initial velocity
		final Double m  = 0.330; // Mass
		final Double g  = getGravitationalAcceleration(); // Gravitational acceleration
		final Double p  = getFluidDensity(1013.25, 24.0); // Density of the fluid through which the projectile is launched
		final Double Av = sizeSquareArea(4.76, 4.76); // Projected surface area of the projectile in the y direction (squere meter)
		final Double Ah = sizeSquareArea(4.76, 4.76); // Projected surface area of the projectile in the x direction (square meter)
		final Double Cv = 0.015; // Drag coefficient of the projectile in the y direction
		final Double Ch = 0.015; // Drag coefficient of the projectile in the x direction
		
		// Initialization
		Double a  = 0.001; // Initial angle
		Double rg; // Temp range for iteration
		rg = iterateAngle(a, v, m, g, p, Av, Ah, Cv, Ch);	
		Double degrees = (r / rg) / 1000;
		System.out.print("Angle adj: "); System.out.printf("%.5f", degrees); System.out.println("°");
		
		
		/*
		// Loop
		while (rg < r) {
			rg = iterateAngle(a, v, m, g, p, Av, Ah, Cv, Ch);
			a = a + 0.0001;
			
			System.out.println( (r / rg) );
			
			if (a >= 90.0) {
				System.out.println("Range impossible");
				break;
			}
		}
			
		// Result
		System.out.print("Angle adjustment: "); System.out.printf("%.5f", a); System.out.println("°");
		*/
	} // End of getProjectileYCorrectionDegrees()
	
	
	
	
	/**
	 * Iterate maximum meters for given angle
     * @param a angle
     * @param v velocity
     * @param m mass
     * @param g gravity constant
     * @param p fluid density calculation
     * @param Av projectile surface area y
     * @param Ah projectile surface area x
     * @param Cv drag coefficient y
     * @param Ch drag coefficient x
	 * @return return max distance meters
	 */
	private static Double iterateAngle(final Double a, final Double v, final Double m, final Double g, final Double p, final Double Av, final Double Ah, final Double Cv, final Double Ch) {
		// Calculate n
		final Double n = Math.PI / 180 * a;
		//printResult("n", n);
		
		
		// Calculate kv
		final Double kv = 1/2d * p * Av * Cv;
		//printResult("kv", kv);
		
		
		// Calculate b2
		final Double b2 = 
				Math.sqrt(m / (g*kv)) *  (arccosh( Math.sqrt( ((kv * Math.pow(v, 2)) / (m * g)) * Math.pow(Math.sin(n), 2) + 1 ) )   + Math.atan(  v * (Math.sin(n)) * Math.sqrt( kv / (m * g )  ) ));
		//printResult("b2", b2);
		
		
		// Calculate kh
		final Double kh = 1/2d * p * Ah * Ch;
		//printResult("kh", kh);
		
		// Calculate range meters when bullet hits ground
		final Double rg = ( m / kh ) * Math.log( 	( (kh * v * Math.cos(n)) / m ) * b2 + 1 	);
		//printResult("", rg);
		
		return rg; // Return this iteration
	}
	
	
	

    // ---------------------------------
    /* Math helper functions */
	
	
	
	/**
	 * Return height difference from shooter to target based on target select angle and it's range
	 * @param degreeAngle target select angle
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
	/* Common helpers */
	
	
	/**
	 * Helper to print big double numbers in Eclipse
	 * @param text title
	 * @param number long number
	 */
	private static void printResult(final String text, final Double number) {
		System.out.print(text + ": "); System.out.printf("%.12f", number); System.out.println("");
	}
	
	
	// ---------------------------------------------------------------------------------------------
	
	
} // End of class