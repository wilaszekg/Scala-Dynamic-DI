import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe, TestKitBase}
import akka.util.Timeout
import com.github.scaladdi.{ActorDep, DynConfig}
import org.scalatest.{WordSpecLike, WordSpec}
import shapeless.{Nat, HNil, ::}
import com.github.scaladdi.FutureDependencies._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.concurrent.duration._

class ProxyTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with ImplicitSender {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(3 seconds)

  case class User(name: String)

  case class Shop(name: String)

  case class Basket(user: User, shop: Shop, products: Int)

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

  object BasketDep extends DynConfig[Basket, User :: Shop :: HNil]

  implicit def basket(us: User :: Shop :: HNil): Basket =
    Basket(us.head, us(Nat._1), 3)

  case class AskForProducts(basket: Basket)

  def products(basket: Basket :: HNil) = AskForProducts(basket.head)

  //Products((1 to basket.head.products).map(Product).toList)

  def actor(prods: Products :: Basket :: HNil) = Props(new PriceCalculator(prods.head))

  "ooo" should {
    "aaa" in {
      val basketKeeper = new TestProbe(system)
      val props = deps isGiven
        findShop("Bakery") isGiven
        findUser("Greg") requires
        BasketDep requires
        ActorDep[Basket :: HNil, Products](basketKeeper.ref, products) props actor

      val proxy = system.actorOf(props)

      basketKeeper.expectMsg(AskForProducts(Basket(User("Greg"), Shop("Bakery"), 3)))
      basketKeeper.reply(Products(List(Product(5), Product(10))))

      proxy ! Calculate
      expectMsg(Price(15))
    }
  }
}
