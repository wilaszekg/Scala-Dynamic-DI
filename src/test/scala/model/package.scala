import akka.actor.Actor
import com.github.wilaszekg.scaladdi.{FunctionDependency, FutureDependency}

import scala.concurrent.Future

package object model {

  def findUser(name: String) = Future.successful(User(name))

  def findShop(name: String) = Future.successful(Shop(name))

  case class FindUser(name: String)

  val basketDependency: FutureDependency[(User, Shop) => Future[Basket]] = FutureDependency((user: User, shop: Shop) => Future.successful(Basket(user, shop)))

  val improvedBasketDependency = FunctionDependency((basket: Basket) => ImprovedBasket(basket))

  case object Calculate

  case object KillCalculator

  class PriceCalculator(products: Products) extends Actor {
    override def receive: Receive = {
      case Calculate =>
        sender ! Price(products.products.foldLeft(0)(_ + _.price))
      case KillCalculator => throw new IllegalStateException()
    }
  }

  class FailingUserFinder(var failures: Int) extends Actor {
    override def receive: Receive = {
      case FindUser(name) => failures -= 1
        if (failures < 0) sender ! User(name)
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

}
