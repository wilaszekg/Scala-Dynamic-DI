import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.github.scaladdi.FutureDependencies._
import com.github.scaladdi.{ActorDep, DynConfig}
import org.scalatest.WordSpecLike
import shapeless.{::, HNil}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.implicitConversions

class ProxyTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with ImplicitSender {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(3 seconds)

  case class User(name: String)

  case class Shop(name: String)

  case class Basket(user: User, shop: Shop)

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

  case class AskForProducts(basket: Basket)

  def products(basket: Basket): Any = AskForProducts(basket)

  def actor(prods: Products, b: Basket) = Props(new PriceCalculator(prods))

  "Proxied actor" should {

    implicit def basket(u: User, s: Shop): Basket = Basket(u, s)

    "be started and answer" in {
      val basketKeeper = new TestProbe(system)
      val props = deps isGiven
        findShop("Bakery") isGiven
        findUser("Greg") requires
        BasketDep requires
        ActorDep(basketKeeper.ref, products _, classOf[Products]) props actor _

      val proxy = system.actorOf(props)

      basketKeeper.expectMsg(AskForProducts(Basket(User("Greg"), Shop("Bakery"))))
      basketKeeper.reply(Products(List(Product(5), Product(10))))

      proxy ! Calculate
      expectMsg(Price(15))
    }
  }
}
