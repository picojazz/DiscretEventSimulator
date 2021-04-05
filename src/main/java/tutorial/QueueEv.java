package tutorial;
import umontreal.ssj.simevents.*;
import umontreal.ssj.rng.*;
import umontreal.ssj.randvar.*;
import umontreal.ssj.stat.*;
import java.util.LinkedList;

public class QueueEv {

   RandomVariateGen genArr;
   RandomVariateGen genServ;
   LinkedList<Customer> waitList = new LinkedList<Customer> ();
   LinkedList<Customer> waitList2 = new LinkedList<Customer> ();
   LinkedList<Customer> servList = new LinkedList<Customer> ();
   LinkedList<Customer> servList2 = new LinkedList<Customer> ();
   Tally custWaits     = new Tally ("Waiting times");
   Accumulate totWait  = new Accumulate ("Size of queue");
   
   class Customer {
      double arrivTime, servTime;
      int type;
      public Customer(){
         type = (int)(1 + Math.random() * ( 3 - 1 ));
      }
   }

   public QueueEv (double lambda, double mu) {
      genArr = new ExponentialGen (new MRG32k3a(), lambda);
      genServ = new ExponentialGen (new MRG32k3a(), mu);
      //s =s;  add int s in constructor
   }

   public void simulate (double timeHorizon) {
      Sim.init();
      new EndOfSim().schedule (timeHorizon);
      new Arrival().schedule (genArr.nextDouble());
      Sim.start();
   }

   class Arrival extends Event {

      public void addCustToServ(LinkedList<Customer> serv,Customer cust){
            custWaits.add (0.0);
            serv.addLast (cust);
      }
      public void addCustToWait(LinkedList<Customer> wait,Customer cust){
         wait.addLast (cust);
         totWait.update (wait.size());
      }


      public void actions() {
         new Arrival().schedule (genArr.nextDouble()); // Next arrival.
         Customer cust = new Customer();  // Cust just arrived.
         cust.arrivTime = Sim.time();
         cust.servTime = genServ.nextDouble();
         //routing for a customer type 1
         if(cust.type == 1){
            if(servList.size() == 0){
               addCustToServ(servList,cust);
               new Departure(cust,1).schedule (cust.servTime);
            }else if(servList2.size() == 0){
               addCustToServ(servList2,cust);
               new Departure(cust,2).schedule (cust.servTime);
            }else {
               addCustToWait(waitList,cust);

            }
         }
         if(cust.type == 2){
            if (servList2.size() > 0) {       // Must join the queue.
               addCustToWait(waitList2,cust);
            } else {                         // Starts service.
               addCustToServ(servList2,cust);
               new Departure(cust,2).schedule (cust.servTime);
            }
         }

      }
   }

   class Departure extends Event {
      int type,serv;

      public Departure(Customer cust, int serv ){
         type = cust.type;
         this.serv = serv;
      }

      public void addCustFromWaitToServ1(LinkedList<Customer> serv,LinkedList<Customer> wait){
         Customer cust = wait.removeFirst();
         totWait.update(wait.size());
         custWaits.add(Sim.time() - cust.arrivTime);
         serv.addLast(cust);
         new Departure(cust, 1).schedule(cust.servTime);
      }
      public void addCustFromWaitToServ2(LinkedList<Customer> serv,LinkedList<Customer> wait){
         Customer cust = wait.removeFirst();
         totWait.update(wait.size());
         custWaits.add(Sim.time() - cust.arrivTime);
         serv.addLast(cust);
         new Departure(cust, 2).schedule(cust.servTime);
      }

      public void actions() {
         if (type == 1) {
            if (serv == 1) {
               servList.removeFirst();
               if (waitList.size() > 0) {
                  // Starts service for next one in queue.
                  addCustFromWaitToServ1(servList,waitList);

               }
            } else {
               servList2.removeFirst();
               if (waitList.size() > 0) {
                  // Starts service for next one in queue.
                  addCustFromWaitToServ2(servList2,waitList);
               } else if (waitList2.size() > 0) {
                  addCustFromWaitToServ2(servList2,waitList2);
               }
            }

         } else {
            servList2.removeFirst();
            if (waitList2.size() > 0) {
               // Starts service for next one in queue.
               addCustFromWaitToServ2(servList2,waitList2);
            }


         }
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
      QueueEv queue = new QueueEv (lambda, mu);
      queue.simulate (10000.0);
      System.out.println (queue.custWaits.report());
      System.out.println (queue.totWait.report());
   /*      double Wq=(lambda)/(mu*(mu-lambda));
      System.out.println ("W="+Wq);
      double Lq=(lambda*lambda)/(mu*(mu-lambda));
      System.out.println ("Lq="+Lq);*/
   }
}
