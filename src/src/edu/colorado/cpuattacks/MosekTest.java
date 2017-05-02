package edu.colorado.cpuattacks;

import mosek.*; 

public class MosekTest 
{ 
	static final int numcon = 4; 
	static final int numvar = 10; 
	//static final int NUMANZ = 14; 
    
	/* 
	 * w(a) = 1; w(b) = 2; w(c) = 3
	 * 
	 * S ->  (aA)_1 | (bB)_1    // f1 | f2
	 * A -> (aCb)_2 | a         // f3 | f4 
	 * C ->  (aB)_2 | (A)_3 | c // f5 | f6 | f7
	 * B ->  (cA)_3 | (C)_3 | b // f8 | f9 | f10
	 * 
	 * 	max   	  w(a)*(f1+f3+f4+f5) // f1 + 2*f2 + 3*f3 + f4 + f5  
	 * 			+ w(b)*(f2+f3+f10)	 // + 3*f7 + 3*f8 + 2*f10
	 * 			+ w(c)*(f7+f8)       // [1.0,2.0,3.0,1.0,1.0,0.0,3.0,3.0,0.0,2.0]
	 * 									  f1, f2, f3, f4, f5, f6, f7, f8, f9,f10
	 * 								
	 * 								  // in(T) = out(T)
	 * 0:                f1 + f2  = 1 // in(S) = out(S)
	 * 1: f1 + f6 + f8 - f3 - f4  = 0 // in(A) = out(A)
	 * 2:-f3 - f9 + f5 + f6 + f7  = 0 // in(C) = out(C)
	 * 3:-f2 - f5 + f8 + f9 + f10 = 0 // in(B) = out(B)
	 * 
	 * 
	 * Now ask (unfinished):
	 * transition 1:
	 * c1,1 <= vA1
	 * c1,2 <= vB1
	 *    0 <= vC1
	 * c1,1 + c1,2 <= 1
	 * 0 <= c1,1; c1,2 <= 1
	 * 0 <= vA1 <= 1 
	 * 0 <= vB1 <= 1
	 * 0 <= vC1 <= 1
	 * vA1 + vB1 + vC1 <= 1
	 * transition 2:
	 * vA1 <= vC2 + va2
	 * vB1 <= vA2 + vC2 + vb2
	 * vC1 <= vB2 + vA2 + vc2
	 * c2,3 <= vC3
	 * c2,4 <= va2
	 * c2,8 <= vA2
	 * c2,9 <= vC3
	 * c2,10<= vb3
	 * 0 <= c2,3 + c2,4 + c2,8 + c2,9 + c2,10 <= 1
	 *  ...
	 *  for all k = 9 transitions 
	 *  
	 *  Then 
	 *  c1,1  + c2,1  + ... + ck,1  <= f1
	 *  c1,2  + c2,2  + ... + ck,2  <= f2
	 *  				...
	 *  c1,10 + c2,10 + ... + ck,10 <= f10  // k*10 vars, 10 constraints
	 *  c1,1 + c1,2 + ... + c1,10 = 1
	 *  c2,1 + c2,2 + ... + c2,10 = 1
	 *  			  ...
	 *  ck,1 + ck,2 + ... + ck,10 = 1   // 0 new vars, 10 new constraints
	 *  vS1 = 1							// 1 new constraint
	 *  vS1 + vS2 + vS3 + ... + vS10 <= in(S) = 1 
	 *  vA1 + vA2 + vA3 + ... + vA10 <= in(A)
	 *  vB1 + vB2 + vB3 + ... + vB10 <= in(B)
	 *  vC1 + vC2 + vC3 + ... + vC10 <= in(C)  // 4 * 10 new vars, 4 new cons
	 *  va1 + va2 + va3 + ... + va10 <= f4  (f_a = 1)
	 *  vb1 + vb2 + vb3 + ... + vb10 <= f7  (f_b = 0)
	 *  vc1 + vc2 + vc3 + ... + vc10 <= f10 (f_c = 0)
	 */

    /**
     * @param args
     */
    public static void main (String[] args) 
    { 
    // Since the value of infinity is ignored, we define it solely 
    // for symbolic purposes 
    	double infinity = 0; 
     
    	double c[]      = {1.0,2.0,3.0,1.0,1.0,0.0,3.0,3.0,0.0,2.0}; 
    	int    asub[][] = { {0,1},  // f1
    						{0,3},  // f2
    						{1,2},  // f3
    						{1},    // f4
    						{2,3},  // f5
    						{1,2},  // f6
    						{2},    // f7
    						{1,3},  // f8
    						{2,3},  // f9
    						{3}};   // f10 
    	double aval[][] = { { 1.0, 1.0}, // f1
    						{ 1.0,-1.0}, // f2
    						{-1.0,-1.0}, // f3
    						{-1.0},      // f4
    						{ 1.0,-1.0}, // f5
    						{ 1.0, 1.0}, // f6
    						{ 1.0},      // f7
    						{ 1.0, 1.0}, // f8
    						{-1.0, 1.0}, // f9
    						{ 1.0}};     // f10 
    	mosek.boundkey[] 
                 bkc    = {mosek.boundkey.fx,  // fx  = ; ra lb <= constr_0 <=ub, la
                           mosek.boundkey.fx, 
                           mosek.boundkey.fx,
                           mosek.boundkey.fx}; 
    	double  blc[]  = {1.0,  // constraint 0: LHS >= 1
    					  0.0,  // constraint 1: LHS >= 0
    					  0.0,  // constraint 2: LHS >= 0 
    					  0.0}; // constraint 3: LHS >= 0
    	double  buc[]  = {1.0,  // constraint 0: LHS <= 1
				  		  0.0,  // constraint 1: LHS <= 0
				  		  0.0,  // constraint 2: LHS <= 0 
				  		  0.0}; // constraint 3: LHS <= 0
    	mosek.boundkey 
            	bkx[]  = {mosek.boundkey.ra,  // 0 <= f1  <= 1  
            			  mosek.boundkey.ra,  // 0 <= f2  <= 1 
            			  mosek.boundkey.ra,  // 0 <= f3  <= 2
            			  mosek.boundkey.lo,  // 0 <= f4 
            			  mosek.boundkey.ra,  // 0 <= f5  <= 2 
            			  mosek.boundkey.ra,  // 0 <= f6  <= 3 
            			  mosek.boundkey.lo,  // 0 <= f7
            			  mosek.boundkey.ra,  // 0 <= f8  <= 3 
            			  mosek.boundkey.ra,  // 0 <= f9  <= 3 
            			  mosek.boundkey.lo}; // 0 <= f10
    	double  blx[]  = {0.0, 
    					  0.0,
    					  0.0,
    					  0.0,
    					  0.0,
    					  0.0,
    					  0.0,
    					  0.0,
    					  0.0,
    					  0.0}; 
    	double  bux[]  = {1.0,
    					  1.0,
    					  2.0,
                   	  	  +infinity, 
                   	  	  2.0,
                   	  	  3.0,
                   	  	  +infinity,
                   	  	  3.0,
                   	  	  3.0,
                   	  	  +infinity}; 
     
    double[] xx  = new double[numvar]; 
 
    try (Env  env  = new Env(); 
         Task task = new Task(env,0,0))
       {
         // Directs the log task stream to the user specified
         // method task_msg_obj.stream
         task.set_Stream(
           mosek.streamtype.log,
           new mosek.Stream() 
             { public void stream(String msg) { System.out.print(msg); }});
         
         /* FOR INTEGER PROGRAM */
         task.set_ItgSolutionCallback(
        	new mosek.ItgSolutionCallback()
	     	{
        		public void callback(double[] xx)
        		{
        		 System.out.print("New integer solution: ");
        		 for (double v : xx) System.out.print(""+v+" ");
        		 System.out.println("");
        		}
        	});
         /* END FOR INTEGER PROGRAM */
   
         /* Give MOSEK an estimate of the size of the input data. 
        This is done to increase the speed of inputting data. 
        However, it is optional. */
         /* Append 'numcon' empty constraints.
        The constraints will initially have no bounds. */
         task.appendcons(numcon);
         
         /* Append 'numvar' variables.
        The variables will initially be fixed at zero (x=0). */
         task.appendvars(numvar);
           
         for(int j=0; j<numvar; ++j)
         {
           /* Set the linear term c_j in the objective.*/  
           task.putcj(j,c[j]);
           /* Set the bounds on variable j.
              blx[j] <= x_j <= bux[j] */
           task.putvarbound(j,bkx[j],blx[j],bux[j]);
           
           // Input column j of A
           task.putacol(j, asub[j], aval[j]);
         }
         
         /* Set the bounds on constraints.
          for i=1, ...,numcon : blc[i] <= constraint i <= buc[i] */
         for(int i=0; i<numcon; ++i)  
           task.putconbound(i,bkc[i],blc[i],buc[i]);
         
         /* FOR INTEGER PROGRAM */
         /* Specify integer variables. */
         for(int j=0; j<numvar; ++j)
        	 task.putvartype(j,mosek.variabletype.type_int);
         /* END FOR INTEGER PROGRAM */

         
         /* A maximization problem */ 
         task.putobjsense(mosek.objsense.maximize);

         /* Solve the problem */
         try
         {
        	 task.optimize();
         }
         catch (mosek.Warning e)
         {
        	 System.out.println (" Mosek warning:");
        	 System.out.println (e.toString ());
         }
                   
         System.out.println("Done solving!!!");
         // Print a summary containing information
         //   about the solution for debugging purposes
         task.solutionsummary(mosek.streamtype.msg);     

         /* Get status information about the solution */ 
         mosek.solsta solsta[] = new mosek.solsta[1];
         task.getsolsta(mosek.soltype.itg,solsta); // get the integer solution type
         System.out.println("Done solving!!!");

         /* INTEGER PROGRAM HANDLER */
         task.getxx(mosek.soltype.itg, // Request the integer solution.     
                 xx);
         switch(solsta[0])
         {
         case integer_optimal:
         case near_integer_optimal:
           System.out.println("Optimal solution\n");
           double obj = 0.0;
           for(int j = 0; j < numvar; ++j){
             System.out.println ("f[" + j + "]:" + xx[j]);
             obj += c[j]*xx[j];
           };
           System.out.println("obj :"+obj);
           break;
         case prim_feas:
        	 System.out.println("Feasible solution\n");
        	 for(int j = 0; j < numvar; ++j)
        		 System.out.println ("x[" + j + "]:" + xx[j]);
        	 break;   
         case unknown:
        	 mosek.prosta prosta[] = new mosek.prosta[1];
        	 task.getprosta(mosek.soltype.itg,prosta);
        	 switch(prosta[0])
        	 {  
        	 case prim_infeas_or_unbounded:
        		 System.out.println("Problem status Infeasible or unbounded");
        		 break;
    		 case prim_infeas:
        		 System.out.println("Problem status Infeasible.");
        		 break;
    		 case unknown:
        		 System.out.println("Problem status unknown.");
        		 break;
    		 default:
        		 System.out.println("Other problem status.");
        		 break;
    		 }
    		 break;
    		 
         default:
           System.out.println("Other solution status");
           break;
         }
       }
       catch (mosek.Exception e)
       {
         System.out.println ("An error or warning was encountered");
         System.out.println (e.getMessage ());
         throw e;
       }
     }
   }