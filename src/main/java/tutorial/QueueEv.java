package tutorial;
import umontreal.ssj.simevents.*;
import umontreal.ssj.rng.*;
import umontreal.ssj.randvar.*;
import umontreal.ssj.stat.*;
import java.util.LinkedList;

public class QueueEv {

   RandomVariateGen genArr,genArr2;
   RandomVariateGen genServ,genServ2;
   LinkedList<Customer> waitList = new LinkedList<Customer> ();
   LinkedList<Customer> waitList2 = new LinkedList<Customer> ();
   LinkedList<Customer> servList = new LinkedList<Customer> ();
   LinkedList<Customer> servList2 = new LinkedList<Customer> ();
   Tally custWaits     = new Tally ("Waiting times");
   Accumulate totWait  = new Accumulate ("Size of queue");
   Tally custWaits2     = new Tally ("Waiting times 2");
   Accumulate totWait2  = new Accumulate ("Size of queue 2");
   int grp1,grp2;
   
   class Customer {
      double arrivTime, servTime;
      int type;
      public Customer(int type){
         this.type = type;

      }
   }

   public QueueEv (double lambda, double mu,double lambda2, double mu2,int grp1,int grp2) {
      genArr = new ExponentialGen (new MRG32k3a(), lambda);
      genServ = new ExponentialGen (new MRG32k3a(), mu);
      genArr2 = new ExponentialGen (new MRG32k3a(), lambda2);
      genServ2 = new ExponentialGen (new MRG32k3a(), mu2);
      this.grp1 = grp1;
      this.grp2 = grp2;
   }

   public void simulate (double timeHorizon) {
      Sim.init();
      new EndOfSim().schedule (timeHorizon);
      new Arrival(1).schedule (genArr.nextDouble());
      new Arrival(2).schedule (genArr2.nextDouble());
      Sim.start();
   }

   class Arrival extends Event {

      Customer cust ;
      int type;
      public Arrival(int type){
         cust = new Customer(type);
      }

      public void addCustToServ(LinkedList<Customer> serv,Customer cust,int typeWait){
         if(typeWait == 1) {
            custWaits.add(0.0);
         }else{
            custWaits2.add(0.0);
         }
            serv.addLast (cust);
      }
      public void addCustToWait(LinkedList<Customer> wait,Customer cust,int typeWait){
         wait.addLast (cust);
         if(typeWait ==1) {
            totWait.update(wait.size());
         }else{
            totWait2.update(wait.size());
         }
      }


      public void actions() {



         cust.arrivTime = Sim.time();
         if(cust.type == 1) {
            cust.servTime = genServ.nextDouble();
         }else{
            cust.servTime = genServ2.nextDouble();
         }
         //routing for a customer type 1
         if(cust.type == 1){
            new Arrival(1).schedule (genArr.nextDouble());
            if(servList.size() < grp1){
               addCustToServ(servList,cust,1);
               new Departure(cust,1).schedule (cust.servTime);
            }else if(servList2.size() < grp2){
               addCustToServ(servList2,cust,1);
               new Departure(cust,2).schedule (cust.servTime);
            }else {
               addCustToWait(waitList,cust,1);

            }
         }
         if(cust.type == 2){
            new Arrival(2).schedule (genArr2.nextDouble());

            if(servList2.size() < grp2){                         // Starts service.
               addCustToServ(servList2,cust,2);
               new Departure(cust,2).schedule (cust.servTime);
            }else{
                  addCustToWait(waitList2,cust,2);
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
      public void addCustFromWaitToServ2(LinkedList<Customer> serv,LinkedList<Customer> wait,int typeWait){
         Customer cust = wait.removeFirst();
         if(typeWait == 1) {
            totWait.update(wait.size());
            custWaits.add(Sim.time() - cust.arrivTime);
         }else{
            totWait2.update(wait.size());
            custWaits2.add(Sim.time() - cust.arrivTime);
         }
         serv.addLast(cust);
         new Departure(cust, 2).schedule(cust.servTime);
      }
      public void addCustomerToServ2(){
         servList2.removeFirst();
         if (waitList.size() > 0 && waitList2.size() == 0){
            addCustFromWaitToServ2(servList2,waitList,1);
         }
         if (waitList2.size() > 0 && waitList.size() == 0){
            addCustFromWaitToServ2(servList2,waitList2,2);
         }
         if (waitList.size() > 0 && waitList2.size() > 0){
            if(waitList.getFirst().arrivTime > waitList2.getFirst().arrivTime){
               addCustFromWaitToServ2(servList2,waitList,1);
            }
            addCustFromWaitToServ2(servList2,waitList2,2);
         }
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
               addCustomerToServ2();

            }

         } else {
            addCustomerToServ2();

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
	  double lambda= 2.1;
      double mu2=2.2;
      double lambda2= 3.0;
      int grp1 = 3;
      int grp2 = 5;
      QueueEv queue = new QueueEv (lambda, mu,lambda2,mu2,grp1,grp2);
      queue.simulate (10000.0);
      System.out.println (queue.custWaits.report());
      System.out.println (queue.custWaits2.report());
      System.out.println("----------------------------------------------------------------------------------------------------");
      System.out.println (queue.totWait.report());
      System.out.println (queue.totWait2.report());
   /*      double Wq=(lambda)/(mu*(mu-lambda));
      System.out.println ("W="+Wq);
      double Lq=(lambda*lambda)/(mu*(mu-lambda));
      System.out.println ("Lq="+Lq);*/
   }
}
