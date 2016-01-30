import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.github.scaladdi.FutureDependencies._
import com.github.scaladdi.{ActorDep, FunctionDependency, FutureDependency}
import org.scalatest.WordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.implicitConversions

class ProxyTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with ImplicitSender {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(3 seconds)

  case class User(name: String)

  case class Shop(name: String)

  case class Basket(user: User, shop: Shop)

  case class ImprovedBasket(basket: Basket)

  case class Product(price: Int)

  case class Products(products: List[Product])

  def findUser(name: String) = Future(User(name))

  def findShop(name: String) = Future(Shop(name))

  case object Calculate

  case class Price(value: Int)

  class PriceCalculator(products: Products) extends Actor {
    override def receive: Receive = {
      case Calculate =>
        sender ! Price(products.products.foldLeft(0)(_ + _.price))
    }
  }

  val improveDep: FunctionDependency[ImprovedBasket, Tuple1[Basket]] = FunctionDependency((basket: Basket) => ImprovedBasket(basket))

  case class AskForProducts(basket: ImprovedBasket)

  def actor(prods: Products) = Props(new PriceCalculator(prods))

  "Proxied actor" should {

    "be started and answer" in {
      val basketKeeper = new TestProbe(system)
      val props = deps.withFuture(findShop("Bakery"))
        .withVal(User("John"))
        .requires(FutureDependency((user: User, shop: Shop) => Future(Basket(user, shop))))
        .requires(improveDep)
        .requires(ActorDep(basketKeeper.ref, (b: ImprovedBasket) => AskForProducts(b), classOf[Products])).props(actor _)

      val proxy = system.actorOf(props)

      basketKeeper.expectMsg(AskForProducts(ImprovedBasket(Basket(User("John"), Shop("Bakery")))))
      basketKeeper.reply(Products(List(Product(5), Product(10))))

      proxy ! Calculate
      expectMsg(Price(15))
    }
  }
}
