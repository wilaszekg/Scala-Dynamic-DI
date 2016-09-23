import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.github.wilaszekg.scaladdi.Dependencies
import com.github.wilaszekg.scaladdi.akka.ActorDependency
import model._
import org.scalatest.{Matchers, WordSpecLike}
import shapeless.HNil
import shapeless.test.illTyped

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

class FutureDependenciesTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with ImplicitSender with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val timeoutDuration: FiniteDuration = 100 millis
  implicit val timeout = Timeout(timeoutDuration)

  "Future Dependencies" should {
    "call base future only once" in {
      val userFinder = system.actorOf(Props(new OneResponseUserFinder))
      val dependencies = Dependencies().withFuture(findShop("Bakery")).withVal("John")
        .requires(ActorDependency(userFinder, (name: String) => FindUser(name), classOf[User]))
        .requires(basketDependency)
        .requires(improvedBasketDependency).result

      val shop: Shop = Shop("Bakery")
      val user: User = User("John")
      val basket: Basket = Basket(user, shop)
      Await.result(dependencies, timeoutDuration) shouldBe (ImprovedBasket(basket) :: basket :: user :: "John" :: shop :: HNil)
    }

    "not compile" when {
      "future dependency duplicated" in {
        illTyped(
          """import com.github.wilaszekg.scaladdi.FunctionDependency

          Dependencies().withFuture(findShop("Bakery")).withVal("John")
            .requires(FunctionDependency((name: String) => User(name)))
            .requires(basketDependency)
            .requires(basketDependency)""",
          ".*Implicit not found.*Type model.Basket is forbidden to be present in HList.*")
      }

      "can't find requirement" in {
        illTyped(
          """import com.github.wilaszekg.scaladdi.FunctionDependency

          Dependencies().withFuture(findShop("Bakery")).withVal("John")
            .requires(basketDependency)""",
          ".*Implicit not found.*Not all types from FutReq can be found in.*")
      }
    }

  }

}
