package tutorial;
import umontreal.ssj.simevents.*;
import umontreal.ssj.rng.*;
import umontreal.ssj.randvar.*;
import umontreal.ssj.stat.*;
import java.util.LinkedList;

public class QueueEv {

   RandomVariateGen genArr;
   RandomVariateGen genServ;
   RandomVariateGen genAbandon;
   LinkedList<Customer> waitList = new LinkedList<Customer> ();
   LinkedList<Customer> servList = new LinkedList<Customer> ();
   Tally custWaits     = new Tally ("Waiting times");
   Accumulate totWait  = new Accumulate ("Size of queue");
   static int nbAbandon;
   class Customer { double arrivTime, servTime, abandonTime; } //add abandon time

   public QueueEv (double lambda, double mu , double s) {
      genArr = new ExponentialGen (new MRG32k3a(), lambda);
      genServ = new ExponentialGen (new MRG32k3a(), mu);
      genAbandon = new ExponentialGen (new MRG32k3a(), s);

      //s =s;  add int s in constructor
   }

   public void simulate (double timeHorizon) {
      Sim.init();
      new EndOfSim().schedule (timeHorizon);
      new Arrival().schedule (genArr.nextDouble());
      Sim.start();
   }

   class Arrival extends Event {
      public void actions() {
         new Arrival().schedule (genArr.nextDouble()); // Next arrival.
         Customer cust = new Customer();  // Cust just arrived.
         cust.arrivTime = Sim.time();
         cust.servTime = genServ.nextDouble();
         cust.abandonTime = genAbandon.nextDouble();
         //schelude customer's abandon

         if (servList.size() > 0) {       // Must join the queue.
            waitList.addLast (cust);
            totWait.update (waitList.size());
            new Abandon(cust).schedule(cust.abandonTime);
         } else {                         // Starts service.
            custWaits.add (0.0);
            servList.addLast (cust);

            new Departure().schedule (cust.servTime);
         }
      }
   }

   class Departure extends Event {
      public void actions() {
         servList.removeFirst();
         if (waitList.size() > 0) {
            // Starts service for next one in queue.
            Customer cust = waitList.removeFirst();
            new Abandon(cust).cancel();
            totWait.update (waitList.size());
            custWaits.add (Sim.time() - cust.arrivTime);
            servList.addLast (cust);
            new Departure().schedule (cust.servTime);
         }
      }
   }

   class Abandon extends Event{
      Customer cust = new Customer();

      public Abandon(Customer customer){
         cust = customer;
      }

      @Override
      public void actions() {
         waitList.remove(cust);
         totWait.update (waitList.size());
         nbAbandon++;
      }
   }

   class EndOfSim extends Event {
      public void actions() {
         Sim.stop();
      }
   }

   public static void main (String[] args) {
	
	  double mu=2.0;
	  double lambda= 1.0;
	  double s = 2.5;
	  //int nbAbandon =0;
      QueueEv queue = new QueueEv (lambda, mu,s);
      queue.simulate (10000.0);
      System.out.println (queue.custWaits.report());
      System.out.println (queue.totWait.report());
      System.out.println ("Nombre d'abandons ==== "+queue.nbAbandon);
   /*      double Wq=(lambda)/(mu*(mu-lambda));
      System.out.println ("W="+Wq);
      double Lq=(lambda*lambda)/(mu*(mu-lambda));
      System.out.println ("Lq="+Lq);*/
   }
}
