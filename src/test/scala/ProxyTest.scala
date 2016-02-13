import akka.actor.{ActorRef, Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.github.scaladdi.{ActorDependency, FunctionDependency, FutureDependencies, FutureDependency}
import org.scalatest.WordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import model._

class ProxyTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with ImplicitSender {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(3 seconds)

  def findUser(name: String) = Future(User(name))

  def findShop(name: String) = Future(Shop(name))

  case object Calculate

  class PriceCalculator(products: Products) extends Actor {
    override def receive: Receive = {
      case Calculate =>
        sender ! Price(products.products.foldLeft(0)(_ + _.price))
    }
  }

  case class AskForProducts(basket: ImprovedBasket)

  def actor(prods: Products) = Props(new PriceCalculator(prods))

  "Proxied actor" should {

    val basketKeeper = new TestProbe(system)

    "be started and answer" in {
      val props = FutureDependencies().withFuture(findShop("Bakery"))
        .withVal(User("John"))
        .requires(FutureDependency((user: User, shop: Shop) => Future(Basket(user, shop))))
        .requires(FunctionDependency((basket: Basket) => ImprovedBasket(basket)))
        .requires(ActorDependency(basketKeeper.ref, (b: ImprovedBasket) => AskForProducts(b), classOf[Products])).props(actor _)

      val proxy = system.actorOf(props)

      checkBasketPrice(proxy)
    }

    def checkBasketPrice(proxy: ActorRef) = {
      basketKeeper.expectMsg(AskForProducts(ImprovedBasket(Basket(User("John"), Shop("Bakery")))))
      basketKeeper.reply(Products(List(Product(5), Product(10))))

      proxy ! Calculate
      expectMsg(Price(15))
    }
  }
}
