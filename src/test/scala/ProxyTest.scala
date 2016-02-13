import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.github.scaladdi._
import com.github.scaladdi.akka.{ActorDependency, ProxyProps}
import model._
import org.scalatest.{Matchers, WordSpecLike}
import shapeless.HNil

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}

class ProxyTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with ImplicitSender with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val timeoutDuration: FiniteDuration = 100 millis
  implicit val timeout = Timeout(timeoutDuration)

  def findUser(name: String) = Future(User(name))

  def findShop(name: String) = Future(Shop(name))

  case object Calculate

  class PriceCalculator(products: Products) extends Actor {
    override def receive: Receive = {
      case Calculate =>
        sender ! Price(products.products.foldLeft(0)(_ + _.price))
    }
  }

  class FailingUserFinder(var failures: Int) extends Actor {
    override def receive: Receive = {
      case FindUser(name) => failures -= 1
        if (failures <= 0) sender ! User(name)
    }
  }

  class OneResponseUserFinder extends Actor {
    override def receive: Receive = {
      case FindUser(name) => sender ! User(name)
        context become idle
    }

    private def idle: Receive = {
      case _ =>
    }
  }

  case class FindUser(name: String)

  def actor(prods: Products) = Props(new PriceCalculator(prods))

  val basketDependency = FutureDependency((user: User, shop: Shop) => Future(Basket(user, shop)))

  val improvedBasketDependency = FunctionDependency((basket: Basket) => ImprovedBasket(basket))

  "Proxied actor" should {

    val basketKeeper = new TestProbe(system)
    case class AskForProducts(basket: ImprovedBasket)

    "be started and answer" in {
      val proxyProps = new ProxyProps(actor _)
      val props = proxyProps from FutureDependencies().withFuture(findShop("Bakery"))
        .withVal(User("John"))
        .requires(basketDependency)
        .requires(improvedBasketDependency)
        .requires(ActorDependency(basketKeeper.ref, (b: ImprovedBasket) => AskForProducts(b), classOf[Products]))

      val proxy = system.actorOf(props)

      checkBasketPrice(proxy)
    }

    "retry executing dependencies and succeed" in {
      val proxyProps = new ProxyProps(actor _)
      val userFindFailures = 2
      val userFinder = system.actorOf(Props(new FailingUserFinder(userFindFailures)))
      val props = proxyProps from FutureDependencies().withFuture(findShop("Bakery")).withVal("John")
        .requires(ActorDependency(userFinder, (name: String) => FindUser(name), classOf[User]))
        .requires(basketDependency)
        .requires(improvedBasketDependency)
        .requires(ActorDependency(basketKeeper.ref, (b: ImprovedBasket) => AskForProducts(b), classOf[Products]))

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

  "Future Dependencies" should {
    "call base future only once" in {
      val userFinder = system.actorOf(Props(new OneResponseUserFinder))
      val dependencies = FutureDependencies().withFuture(findShop("Bakery")).withVal("John")
        .requires(ActorDependency(userFinder, (name: String) => FindUser(name), classOf[User]))
        .requires(basketDependency)
        .requires(improvedBasketDependency).result

      val shop: Shop = Shop("Bakery")
      val user: User = User("John")
      val basket: Basket = Basket(user, shop)
      Await.result(dependencies, timeoutDuration) shouldBe (ImprovedBasket(basket) :: basket :: user :: "John" :: shop :: HNil)
    }
  }
}
