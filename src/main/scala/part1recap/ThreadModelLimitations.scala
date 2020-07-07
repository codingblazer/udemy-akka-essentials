package part1recap

import scala.concurrent.Future

object ThreadModelLimitations extends App {

  /*
  Daniel's rants
   */

  /**
    * DR #1: OOP encapsulation is only valid in the SINGLE THREADED MODEL.
    */
  class BankAccount(private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int) = this.synchronized {
      this.amount -= money
    }

    def deposit(money: Int) = this.synchronized {
      this.amount += money
    }

    def getAmount = this.synchronized {amount }
  }

  val account = new BankAccount(2000)
  for(_ <- 1 to 1000) {
    new Thread(() => account.withdraw(1)).start()
  }

  for(_ <- 1 to 1000) {
    new Thread(() => account.deposit(1)).start()
  }
  println(account.getAmount)

  // OOP encapsulation is broken in a multithreaded env => if you remove the synchronized block above =>
  //T1 deposit => read value 2000 => 2001 => not updated yet
  //T2 withdraw => read value 2000 => 1999 => updated
  //T1 => updated now => 2001 even though value should have been 2000

  // synchronization! Locks to the rescue => note that if we make all synchronized and not getAmount, since getAmount
  //is not used anywhere, it should work but you will still get 1999
  //this is because the print statement also has main thread which is getting executing before other threads have completed
  //so it accessed this.amount value while other threads were still using it because getAmount function print was using
  //wasnt synchronized and it never asked for lock on it
  //also you could have used thread.sleep before print so all threads are done with



  // Still there are issues: deadlocks, livelocks, blocking data structures

  /**
    * DR #2: delegating something to a thread is a PAIN.
    */

  // you have a running thread and you want to pass a runnable to that thread.

  var task: Runnable = null

  val runningThread: Thread = new Thread(() => {
    while (true) {
      while (task == null) {
        runningThread.synchronized {
          println("[background] waiting for a task...")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("[background] I have a task!")
        task.run()
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable) = {
    if (task == null) task = r

    runningThread.synchronized {
      runningThread.notify()
    }
  }

  runningThread.start()
  Thread.sleep(1000)
  delegateToBackgroundThread(() => println(42))
  Thread.sleep(1000)
  delegateToBackgroundThread(() => println("this should run in the background"))
//Solution => Need a data structure which can receive message, identify sender etc and handle things independently
//
//  /**
//    * DR #3: tracing and dealing with errors in a multithreaded env is a PITN.
//    */
//  // 1M numbers in between 10 threads
  import scala.concurrent.ExecutionContext.Implicits.global

  val futures = (0 to 9)
    .map(i => 100000 * i until 100000 * (i + 1)) // 0 - 99999, 100000 - 199999, 200000 - 299999 etc
    .map(range => Future {
      if (range.contains(546735)) throw new RuntimeException("invalid number")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)(_ + _) // Future with the sum of all the numbers
  sumFuture.onComplete(println)
  //debugging that of different ranges, where it occured is difficult => because you will just see the exception
  // and other extra information and also every run is different so debugging becomes more difficult
}
