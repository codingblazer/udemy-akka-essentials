package part1recap

import scala.concurrent.Future

object AdvancedRecap extends App {

  // partial functions
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val pf = (x: Int) => x match {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val function: (Int => Int) = partialFunction

  val modifiedList = List(1,2,3).map {
    case 1 => 42
    case _ => 0
  }

  // lifting
  val lifted = partialFunction.lift // total function Int => Option[Int]
  lifted(2) // Some(65)
  lifted(5000) // None

  // orElse
  val pfChain = partialFunction.orElse[Int, Int] {
    case 60 => 9000
  }

  pfChain(5) // 999 per partialFunction
  pfChain(60) // 9000
  pfChain(457) // throw a MatchError

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  def receive: ReceiveFunction = {
    case 1 => println("hello")
    case _ => println("confused....")
  }

  // implicits

  implicit val timeout = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()

  setTimeout(() => println("timeout"))// extra parameter list omitted by compiler

  // implicit conversions
  // 1) implicit defs
  case class Person(name: String) {
    def greet = s"Hi, my name is $name"
  }

  implicit def fromStringToPerson(string: String): Person = Person(string)
  "Peter".greet
  // fromStringToPerson("Peter").greet - automatically by the compiler

  // 2) implicit classes
  implicit class Dog(name: String) {
    def bark = println("bark!")
  }
  "Lassie".bark
  // new Dog("Lassie").bark - automatically done by the compiler

  // organize properly => because otherwise other can confuse where it is happening

  //priority of scopes => local > imported > companion objects
  // local scope => picks the first nearest scope of implicit if implcits at multiple levels
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted // List(3,2,1) => sorted function is using the implicit here

  // imported scope example => global implicit coming from imported class
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    println("hello, future")
  }

  // companion objects of the types included in the call
  //i.e. only person type is present in list sorted call => companion object of Person is present below => all implicits
  //inside this are taken into account => since sortint on person, this was implicit required when it searched here and
  //found it
  object Person {
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }

  List(Person("Bob"), Person("Alice")).sorted
  // List(Person(Alice), Person(Bob))
}
